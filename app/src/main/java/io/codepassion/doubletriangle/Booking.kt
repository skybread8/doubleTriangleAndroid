package io.codepassion.doubletriangle

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import io.codepassion.doubletriangle.feature.workout.RemoteTrainingImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Android counterpart of iOS's GymHubView and NewBookingView.  Reservations
 * remain server-authoritative: slots are only guides until the POST succeeds. */
internal data class BookingGym(val id: String, val name: String, val address: String?, val zone: ZoneId, val avatarUrl: String?)
internal data class BookingRoom(val id: String, val name: String, val avatarUrl: String?, val capacity: Int, val interval: Int, val minDuration: Int, val maxDuration: Int, val windowDays: Int, val cancellationCutoff: Int) {
    val durations: List<Int> get() = (interval..maxDuration step interval).filter { it >= minDuration }
}
internal data class BookingSlot(val startsAt: OffsetDateTime, val endsAt: OffsetDateTime, val availablePlaces: Int, val isBookable: Boolean)
internal data class BookingStart(val startsAt: OffsetDateTime, val endsAt: OffsetDateTime, val availablePlaces: Int)
internal data class BookingReservation(val id: String, val status: String, val startsAt: OffsetDateTime, val endsAt: OffsetDateTime, val cancellableUntil: OffsetDateTime?, val roomName: String, val roomAvatar: String?, val gymId: String, val gymName: String, val gymAvatar: String?) {
    val isCancelled get() = status.equals("cancelled", true)
    val isCancellable get() = !isCancelled && cancellableUntil?.isAfter(OffsetDateTime.now()) == true
}

private class BookingApi(private val context: Context) {
    private val token get() = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("token", null)
        ?: throw IllegalStateException("Inicia sesión para gestionar reservas.")

    suspend fun gyms(): List<BookingGym> = getArray("booking/gyms").map { json ->
        BookingGym(json.getString("id"), json.getString("name"), json.optString("address").ifBlank { null }, ZoneId.of(json.optString("timezone", ZoneId.systemDefault().id)), json.optString("avatar_url").ifBlank { null })
    }
    suspend fun rooms(gymId: String): List<BookingRoom> = getArray("booking/gyms/$gymId/rooms").map(::room)
    suspend fun availability(roomId: String, date: String): List<BookingSlot> {
        val root = getObject("booking/rooms/$roomId/availability?date=$date").getJSONObject("data")
        return root.getJSONArray("slots").objects().map { slot -> BookingSlot(OffsetDateTime.parse(slot.getString("starts_at")), OffsetDateTime.parse(slot.getString("ends_at")), slot.getInt("available_places"), slot.getBoolean("is_bookable")) }
    }
    suspend fun reservations(): List<BookingReservation> = getArray("booking/reservations").map(::reservation)
    /** Joins the authenticated account to every gym in the code's team.
     * This is deliberately separate from booking: a gym member can enter the
     * app with no cached gyms and must still be able to establish membership. */
    suspend fun joinGym(code: String) {
        post("gym-memberships/join", JSONObject().put("code", code.trim().uppercase()))
    }
    suspend fun reserve(roomId: String, start: BookingStart, idempotencyKey: UUID): BookingReservation = post("booking/rooms/$roomId/reservations", JSONObject().put("starts_at", start.startsAt).put("ends_at", start.endsAt), idempotencyKey).getJSONObject("data").let(::reservation)
    suspend fun cancel(id: String): BookingReservation = post("booking/reservations/$id/cancel", null).getJSONObject("data").let(::reservation)

    private fun room(json: JSONObject) = BookingRoom(json.getString("id"), json.getString("name"), json.optString("avatar_url").ifBlank { null }, json.getInt("participant_capacity"), json.getInt("slot_interval_minutes"), json.getInt("min_duration_minutes"), json.getInt("max_duration_minutes"), json.getInt("booking_window_days"), json.getInt("cancellation_cutoff_minutes"))
    private fun reservation(json: JSONObject): BookingReservation {
        val room = json.getJSONObject("room"); val gym = json.getJSONObject("gym")
        return BookingReservation(json.getString("id"), json.getString("status"), OffsetDateTime.parse(json.getString("starts_at")), OffsetDateTime.parse(json.getString("ends_at")), json.optString("cancellable_until").takeIf { it.isNotBlank() }?.let(OffsetDateTime::parse), room.getString("name"), room.optString("avatar_url").ifBlank { null }, gym.getString("id"), gym.getString("name"), gym.optString("avatar_url").ifBlank { null })
    }
    private suspend fun getArray(path: String): List<JSONObject> = getObject(path).getJSONArray("data").objects()
    private suspend fun getObject(path: String): JSONObject = withContext(Dispatchers.IO) { connection(path, "GET").useResponse() }
    private suspend fun post(path: String, body: JSONObject?, key: UUID? = null): JSONObject = withContext(Dispatchers.IO) {
        connection(path, "POST", key).also { connection ->
            if (body != null) OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
        }.useResponse()
    }
    private fun connection(path: String, method: String, key: UUID? = null) = (URL(WildforceApiEnvironment.apiUrl(path)).openConnection() as HttpURLConnection).apply {
        requestMethod = method; connectTimeout = 15_000; readTimeout = 20_000
        setRequestProperty("Accept", "application/json"); setRequestProperty("Authorization", "Bearer $token")
        if (method == "POST") { doOutput = true; setRequestProperty("Content-Type", "application/json") }
        key?.let { setRequestProperty("Idempotency-Key", it.toString()) }
    }
    private fun HttpURLConnection.useResponse(): JSONObject {
        val body = BufferedReader((if (responseCode in 200..299) inputStream else errorStream).reader()).use { it.readText() }
        disconnect()
        if (responseCode !in 200..299) throw IllegalStateException(runCatching { JSONObject(body).optString("message") }.getOrDefault("").ifBlank { "No se pudo completar la reserva." })
        return JSONObject(body)
    }
}
private fun JSONArray.objects() = List(length()) { getJSONObject(it) }
internal fun contiguousStarts(slots: List<BookingSlot>, duration: Int): List<BookingStart> = slots.indices.mapNotNull { index ->
    val start = slots[index].startsAt; val end = start.plusMinutes(duration.toLong()); var cursor = start; var places = Int.MAX_VALUE
    for (slot in slots.drop(index)) { if (!cursor.isBefore(end)) break; if (slot.startsAt != cursor || !slot.isBookable) return@mapNotNull null; places = minOf(places, slot.availablePlaces); cursor = slot.endsAt }
    BookingStart(start, end, places).takeIf { cursor == end }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable internal fun BookingScreen(contentPadding: PaddingValues) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val api = remember { BookingApi(context.applicationContext) }
    var gyms by remember { mutableStateOf<List<BookingGym>>(emptyList()) }; var selectedGymId by remember { mutableStateOf<String?>(null) }
    var reservations by remember { mutableStateOf<List<BookingReservation>>(emptyList()) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf<String?>(null) }
    var bookingGym by remember { mutableStateOf<BookingGym?>(null) }; var cancellation by remember { mutableStateOf<BookingReservation?>(null) }
    var joiningGym by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun reload() { loading = true; error = null; runCatching { api.gyms() to api.reservations() }.onSuccess { (nextGyms, nextReservations) -> gyms = nextGyms; selectedGymId = selectedGymId?.takeIf { id -> nextGyms.any { it.id == id } } ?: nextGyms.firstOrNull()?.id; reservations = nextReservations }.onFailure { error = it.message ?: "No se pudieron cargar las reservas." }; loading = false }
    LaunchedEffect(Unit) { reload() }
    bookingGym?.let { gym -> NewBookingScreen(gym, api, contentPadding, onBack = { bookingGym = null }, onBooked = { bookingGym = null; scope.launch { reload() } }); return }
    val selectedGym = gyms.firstOrNull { it.id == selectedGymId }
    Column(Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("GIMNASIO", fontSize = 28.sp, fontWeight = FontWeight.Black, color = WildforceThemeTokens.textPrimary)
        if (gyms.size > 1) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(gyms) { gym -> FilterChip(selected = gym.id == selectedGymId, onClick = { selectedGymId = gym.id }, content = { Text(gym.name) }) } }
        when {
            loading -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> BookingEmpty("No se pudieron cargar las reservas", error!!, onRetry = { scope.launch { reload() } })
            selectedGym == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BookingEmpty("No tienes gimnasios disponibles", "Introduce el código que te ha dado tu gimnasio para acceder a sus salas.")
                OutlinedButton(onClick = { joiningGym = true }) { Text("TENGO UN CÓDIGO DE GIMNASIO") }
            }
            else -> {
                GymHeading(selectedGym)
                Text("PRÓXIMAS RESERVAS", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                val visible = reservations.filter { it.gymId == selectedGym.id }
                if (visible.none { !it.isCancelled }) BookingEmpty("No hay reservas próximas", "Reserva una sala para entrenar en tu gimnasio.")
                visible.filter { !it.isCancelled }.forEach { reservation -> ReservationCard(reservation, onCancel = { cancellation = reservation }) }
                Button(onClick = { bookingGym = selectedGym }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.background)) { Text("NUEVA RESERVA", fontWeight = FontWeight.Bold) }
                visible.filter { it.isCancelled }.takeIf { it.isNotEmpty() }?.let { cancelled -> Text("CANCELADAS", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary); cancelled.forEach { ReservationCard(it, {}) } }
            }
        }
    }
    if (joiningGym) JoinGymDialog(
        onDismiss = { joiningGym = false },
        onJoin = { code ->
            api.joinGym(code)
            joiningGym = false
            reload()
        },
    )
    cancellation?.let { reservation -> AlertDialog(onDismissRequest = { cancellation = null }, title = { Text("¿Cancelar esta reserva?") }, text = { Text("${reservation.roomName} · ${formatDateTime(reservation.startsAt)}") }, confirmButton = { TextButton(onClick = { cancellation = null; scope.launch { runCatching { api.cancel(reservation.id) }.onFailure { error = it.message }; reload() } }) { Text("CANCELAR") } }, dismissButton = { TextButton(onClick = { cancellation = null }) { Text("MANTENER") } }) }
}

/** Android counterpart of iOS's JoinGymView for an authenticated user. */
@Composable private fun JoinGymDialog(onDismiss: () -> Unit, onJoin: suspend (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("ÚNETE A TU GIMNASIO") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Introduce el código que te ha dado tu gimnasio.", color = WildforceThemeTokens.textSecondary)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().filter(Char::isLetterOrDigit).take(16); localError = null },
                    label = { Text("Código de gimnasio") },
                    singleLine = true,
                    enabled = !submitting,
                )
                localError?.let { Text(it, color = Color(0xFFE26D5A), fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = code.isNotBlank() && !submitting,
                onClick = {
                    submitting = true
                    scope.launch {
                        runCatching { onJoin(code) }.onFailure { localError = it.message ?: "No se pudo unir al gimnasio." }
                        submitting = false
                    }
                },
            ) { if (submitting) CircularProgressIndicator(Modifier.size(18.dp)) else Text("UNIRSE") }
        },
        dismissButton = { TextButton(enabled = !submitting, onClick = onDismiss) { Text("CANCELAR") } },
    )
}

@Composable private fun GymHeading(gym: BookingGym) = Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { RemoteTrainingImage(gym.avatarUrl, gym.name, Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))); Column { Text(gym.name, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); gym.address?.let { Text(it, color = WildforceThemeTokens.textSecondary) } } }
@Composable private fun BookingEmpty(title: String, detail: String, onRetry: (() -> Unit)? = null) = Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.EventAvailable, null, Modifier.size(42.dp), WildforceThemeTokens.textSecondary); Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center); Text(detail, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center); onRetry?.let { TextButton(onClick = it) { Text("REINTENTAR") } } }
@Composable private fun ReservationCard(reservation: BookingReservation, onCancel: () -> Unit) = Card(modifier = Modifier.fillMaxWidth(), backgroundColor = WildforceThemeTokens.backgroundSecondary, shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { RemoteTrainingImage(reservation.roomAvatar ?: reservation.gymAvatar, reservation.roomName, Modifier.size(50.dp).clip(RoundedCornerShape(12.dp))); Column(Modifier.weight(1f)) { Text(reservation.roomName, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); Text("${formatDateTime(reservation.startsAt)} · ${formatTime(reservation.startsAt)}–${formatTime(reservation.endsAt)}", fontSize = 13.sp, color = WildforceThemeTokens.textSecondary); if (reservation.isCancelled) Text("CANCELADA", color = Color(0xFFE26D5A), fontSize = 12.sp) }; if (reservation.isCancellable) IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, "Cancelar reserva", tint = Color(0xFFE26D5A)) } } }

@OptIn(ExperimentalMaterialApi::class)
@Composable private fun NewBookingScreen(gym: BookingGym, api: BookingApi, contentPadding: PaddingValues, onBack: () -> Unit, onBooked: () -> Unit) {
    var rooms by remember { mutableStateOf<List<BookingRoom>>(emptyList()) }; var selectedRoom by remember { mutableStateOf<BookingRoom?>(null) }; var selectedDate by remember { mutableStateOf(ZonedDateTime.now(gym.zone).toLocalDate()) }; var duration by remember { mutableIntStateOf(0) }; var slots by remember { mutableStateOf<List<BookingSlot>>(emptyList()) }; var selected by remember { mutableStateOf<BookingStart?>(null) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf<String?>(null) }; var confirm by remember { mutableStateOf(false) }; var reserving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(gym.id) { runCatching { api.rooms(gym.id) }.onSuccess { rooms = it; selectedRoom = it.firstOrNull(); duration = selectedRoom?.durations?.firstOrNull() ?: 0 }.onFailure { error = it.message }; loading = false }
    LaunchedEffect(selectedRoom?.id, selectedDate) { selectedRoom?.let { room -> runCatching { api.availability(room.id, selectedDate.toString()) }.onSuccess { slots = it; selected = null }.onFailure { error = it.message } } }
    val starts = remember(slots, duration) { contiguousStarts(slots, duration) }
    Column(Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Filled.Close, "Volver") }; Column { Text("NUEVA RESERVA", fontSize = 22.sp, fontWeight = FontWeight.Black, color = WildforceThemeTokens.textPrimary); Text(gym.name, color = WildforceThemeTokens.textSecondary) } }
        if (loading) Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } else if (rooms.isEmpty()) BookingEmpty("No hay salas disponibles", "Este gimnasio no tiene salas abiertas para reservar.") else {
            BookingSection("DÍA") { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items((0..selectedRoom!!.windowDays).map { selectedDate.plusDays(it.toLong()) }) { day -> FilterChip(selected = day == selectedDate, onClick = { selectedDate = day }, content = { Text(day.format(DateTimeFormatter.ofPattern("EEE d"))) }) } } }
            BookingSection("SALA") { rooms.forEach { room -> RoomChoice(room, room == selectedRoom, onClick = { selectedRoom = room; duration = room.durations.first(); selected = null }) } }
            if (selectedRoom!!.durations.size > 1) BookingSection("DURACIÓN") { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(selectedRoom!!.durations) { minutes -> FilterChip(selected = minutes == duration, onClick = { duration = minutes; selected = null }, content = { Text("$minutes min") }) } } }
            BookingSection("HORAS DISPONIBLES") { if (starts.isEmpty()) BookingEmpty("No hay horas disponibles", "Prueba otro día o una duración más corta.") else FlowTimes(starts, selected, { selected = it }) }
            selected?.let { start -> Button(onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.background)) { Text("CONTINUAR", fontWeight = FontWeight.Bold) } }
        }
        error?.let { Text(it, color = Color(0xFFE26D5A)) }
    }
    if (confirm) AlertDialog(onDismissRequest = { if (!reserving) confirm = false }, title = { Text("Confirmar reserva") }, text = { val start = selected!!; Text("${selectedRoom!!.name}\n${formatDateTime(start.startsAt)} · ${formatTime(start.startsAt)}–${formatTime(start.endsAt)}\n\nPuedes cancelar gratis hasta ${selectedRoom!!.cancellationCutoff} min antes.") }, confirmButton = { TextButton(enabled = !reserving, onClick = { reserving = true; val start = selected!!; scope.launch { runCatching { api.reserve(selectedRoom!!.id, start, UUID.randomUUID()) }.onSuccess { confirm = false; onBooked() }.onFailure { error = it.message ?: "No se pudo confirmar la reserva."; reserving = false } } }) { if (reserving) CircularProgressIndicator(Modifier.size(18.dp)) else Text("CONFIRMAR") } }, dismissButton = { TextButton(enabled = !reserving, onClick = { confirm = false }) { Text("CANCELAR") } })
}
@Composable private fun BookingSection(title: String, content: @Composable () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); content() }
@Composable private fun RoomChoice(room: BookingRoom, selected: Boolean, onClick: () -> Unit) = Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), backgroundColor = if (selected) WildforceThemeTokens.textPrimary.copy(alpha = .10f) else WildforceThemeTokens.backgroundSecondary, shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { RemoteTrainingImage(room.avatarUrl, room.name, Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))); Column { Text(room.name, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); Text("${room.capacity} plazas · ${room.minDuration}–${room.maxDuration} min", color = WildforceThemeTokens.textSecondary, fontSize = 13.sp) } } }
@OptIn(ExperimentalMaterialApi::class)
@Composable private fun FlowTimes(starts: List<BookingStart>, selected: BookingStart?, onSelected: (BookingStart) -> Unit) = androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { starts.forEach { start -> FilterChip(selected = selected?.startsAt == start.startsAt, onClick = { onSelected(start) }, content = { Text("${formatTime(start.startsAt)} · ${start.availablePlaces}") }) } }
private fun formatDateTime(time: OffsetDateTime) = time.format(DateTimeFormatter.ofPattern("EEE d MMM"))
private fun formatTime(time: OffsetDateTime) = time.format(DateTimeFormatter.ofPattern("HH:mm"))
