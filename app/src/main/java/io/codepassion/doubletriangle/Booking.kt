package io.codepassion.doubletriangle

import android.content.Context
import android.icu.text.DateTimePatternGenerator
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens
import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import io.codepassion.doubletriangle.feature.onboarding.GymJoinCodeInfo
import io.codepassion.doubletriangle.feature.onboarding.parseGymJoinCode
import io.codepassion.doubletriangle.feature.workout.RemoteTrainingImage
import io.codepassion.wildforce.android.R
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
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/** Android counterpart of iOS's GymHubView and NewBookingView.  Reservations
 * remain server-authoritative: slots are only guides until the POST succeeds. */
internal data class BookingGym(val id: String, val name: String, val address: String?, val teamName: String, val zone: ZoneId, val avatarUrl: String?)
internal data class BookingRoom(val id: String, val name: String, val avatarUrl: String?, val capacity: Int, val interval: Int, val minDuration: Int, val maxDuration: Int, val windowDays: Int, val cancellationCutoff: Int) {
    val durations: List<Int> get() = if (interval > 0) (interval..maxDuration step interval).filter { it >= minDuration } else emptyList()
}
internal data class BookingSlot(val startsAt: OffsetDateTime, val endsAt: OffsetDateTime, val availablePlaces: Int, val isBookable: Boolean)
internal data class BookingStart(val startsAt: OffsetDateTime, val endsAt: OffsetDateTime, val availablePlaces: Int)
internal data class BookingReservation(val id: String, val status: String, val startsAt: OffsetDateTime, val endsAt: OffsetDateTime, val cancellableUntil: OffsetDateTime?, val roomName: String, val roomAvatar: String?, val gymId: String, val gymName: String, val gymAvatar: String?, val gymZone: ZoneId) {
    val isCancelled get() = status.equals("cancelled", true)
    val isCancellable get() = !isCancelled && cancellableUntil?.isAfter(OffsetDateTime.now()) == true
}

internal class BookingApi(private val context: Context) {
    private val token get() = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("token", null)
        ?: throw IllegalStateException("Inicia sesión para gestionar reservas.")

    suspend fun gyms(): List<BookingGym> = getArray("booking/gyms").map { json ->
        BookingGym(json.getString("id"), json.getString("name"), json.optionalBookingString("address"), json.optJSONObject("team")?.optionalBookingString("name").orEmpty(), bookingZone(json.optionalBookingString("timezone")), json.optionalBookingString("avatar_url"))
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

    /** Public counterpart of iOS's pre-registration join-code check. */
    suspend fun checkJoinCode(code: String): GymJoinCodeInfo = withContext(Dispatchers.IO) {
        val normalized = code.trim().uppercase()
        val root = connection("gym-memberships/codes/$normalized", "GET", authenticated = false).useResponse()
        val data = root.getJSONObject("data")
        GymJoinCodeInfo(normalized, data.getJSONObject("team").getString("name"))
    }
    suspend fun reserve(roomId: String, start: BookingStart, idempotencyKey: UUID): BookingReservation = post("booking/rooms/$roomId/reservations", JSONObject().put("starts_at", start.startsAt).put("ends_at", start.endsAt), idempotencyKey).getJSONObject("data").let(::reservation)
    suspend fun cancel(id: String): BookingReservation = post("booking/reservations/$id/cancel", null).getJSONObject("data").let(::reservation)

    private fun room(json: JSONObject) = BookingRoom(json.getString("id"), json.getString("name"), json.optionalBookingString("avatar_url"), json.getInt("participant_capacity"), json.getInt("slot_interval_minutes"), json.getInt("min_duration_minutes"), json.getInt("max_duration_minutes"), json.getInt("booking_window_days"), json.getInt("cancellation_cutoff_minutes"))
    private fun reservation(json: JSONObject): BookingReservation {
        val room = json.getJSONObject("room"); val gym = json.getJSONObject("gym")
        return BookingReservation(json.getString("id"), json.getString("status"), OffsetDateTime.parse(json.getString("starts_at")), OffsetDateTime.parse(json.getString("ends_at")), json.optionalBookingString("cancellable_until")?.let(OffsetDateTime::parse), room.getString("name"), room.optionalBookingString("avatar_url"), gym.getString("id"), gym.getString("name"), gym.optionalBookingString("avatar_url"), bookingZone(json.optionalBookingString("timezone")))
    }
    private suspend fun getArray(path: String): List<JSONObject> = getObject(path).getJSONArray("data").objects()
    private suspend fun getObject(path: String): JSONObject = withContext(Dispatchers.IO) { connection(path, "GET").useResponse() }
    private suspend fun post(path: String, body: JSONObject?, key: UUID? = null): JSONObject = withContext(Dispatchers.IO) {
        connection(path, "POST", key).also { connection ->
            if (body != null) OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
        }.useResponse()
    }
    private fun connection(path: String, method: String, key: UUID? = null, authenticated: Boolean = true) = (URL(WildforceApiEnvironment.apiUrl(path)).openConnection() as HttpURLConnection).apply {
        requestMethod = method; connectTimeout = 15_000; readTimeout = 20_000
        setRequestProperty("Accept", "application/json"); if (authenticated) setRequestProperty("Authorization", "Bearer $token")
        if (method == "POST") { doOutput = true; setRequestProperty("Content-Type", "application/json") }
        key?.let { setRequestProperty("Idempotency-Key", it.toString()) }
    }
    private fun HttpURLConnection.useResponse(): JSONObject {
        val body = BufferedReader((if (responseCode in 200..299) inputStream else errorStream).reader()).use { it.readText() }
        disconnect()
        if (responseCode !in 200..299) throw BookingHttpException(responseCode, runCatching { JSONObject(body).optString("message") }.getOrDefault("").ifBlank { "No se pudo completar la reserva." })
        return JSONObject(body)
    }
}

private const val SELECTED_BOOKING_GYM_ID = "booking_selected_gym_id"

private fun JSONArray.objects() = List(length()) { getJSONObject(it) }
private fun JSONObject.optionalBookingString(key: String): String? = if (isNull(key)) null else normalizeBookingOptionalString(optString(key))
internal fun contiguousStarts(slots: List<BookingSlot>, duration: Int): List<BookingStart> = slots.indices.mapNotNull { index ->
    if (duration <= 0) return@mapNotNull null
    val start = slots[index].startsAt; val end = start.plusMinutes(duration.toLong()); var cursor = start; var places = Int.MAX_VALUE
    for (slot in slots.drop(index)) { if (!cursor.isBefore(end)) break; if (slot.startsAt.toInstant() != cursor.toInstant() || !slot.isBookable) return@mapNotNull null; places = minOf(places, slot.availablePlaces); cursor = slot.endsAt }
    BookingStart(start, end, places).takeIf { cursor.toInstant() == end.toInstant() }
}

internal fun refreshedBookingSelection(selected: BookingStart?, slots: List<BookingSlot>, duration: Int): BookingStart? =
    selected?.let { old -> contiguousStarts(slots, duration).firstOrNull { it.startsAt.toInstant() == old.startsAt.toInstant() } }

@OptIn(ExperimentalMaterialApi::class)
@Composable internal fun BookingScreen(contentPadding: PaddingValues, onBookingVisibilityChanged: (Boolean) -> Unit = {}) {
    val locale = LocalLocale.current.platformLocale
    val context = androidx.compose.ui.platform.LocalContext.current
    val api = remember { BookingApi(context.applicationContext) }
    val gymCodeScanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }
    var gyms by remember { mutableStateOf<List<BookingGym>>(emptyList()) }
    val bookingPreferences = remember { context.getSharedPreferences("wildforce_booking", Context.MODE_PRIVATE) }
    var selectedGymId by remember { mutableStateOf(bookingPreferences.getString(SELECTED_BOOKING_GYM_ID, null)) }
    var reservations by remember { mutableStateOf<List<BookingReservation>>(emptyList()) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf<String?>(null) }; var actionError by remember { mutableStateOf<String?>(null) }
    var bookingGym by remember { mutableStateOf<BookingGym?>(null) }; var cancellation by remember { mutableStateOf<BookingReservation?>(null) }
    LaunchedEffect(bookingGym != null) { onBookingVisibilityChanged(bookingGym != null) }
    DisposableEffect(Unit) { onDispose { onBookingVisibilityChanged(false) } }
    var joiningGym by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun reload() { loading = true; error = null; runCatching { api.gyms() to api.reservations() }.onSuccess { (nextGyms, nextReservations) -> gyms = nextGyms; selectedGymId = selectedGymId?.takeIf { id -> nextGyms.any { it.id == id } } ?: nextGyms.firstOrNull()?.id; bookingPreferences.edit().putString(SELECTED_BOOKING_GYM_ID, selectedGymId).apply(); reservations = nextReservations }.onFailure { error = it.message ?: "No se pudieron cargar las reservas." }; loading = false }
    LaunchedEffect(Unit) { reload() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { reload() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    bookingGym?.let { gym -> NewBookingScreen(gym, api, contentPadding, onBack = { bookingGym = null }, onBooked = { bookingGym = null; scope.launch { reload() } }); return }
    val selectedGym = gyms.firstOrNull { it.id == selectedGymId }
    val refreshState = rememberPullRefreshState(refreshing = loading, onRefresh = { scope.launch { reload() } })
    Box(Modifier.fillMaxSize().pullRefresh(refreshState)) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            selectedGym?.let { gym ->
                GymHeading(
                    gym = gym,
                    gyms = gyms,
                    onSelect = { next ->
                        selectedGymId = next.id
                        bookingPreferences.edit().putString(SELECTED_BOOKING_GYM_ID, next.id).apply()
                    },
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when {
                    loading -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    error != null -> BookingEmpty("No se pudieron cargar las reservas", error!!, onRetry = { scope.launch { reload() } })
                    selectedGym == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BookingEmpty("No tienes gimnasios disponibles", "Introduce el código que te ha dado tu gimnasio para acceder a sus salas.")
                        OutlinedButton(onClick = { joiningGym = true }) { Text("TENGO UN CÓDIGO DE GIMNASIO") }
                    }
                    else -> {
                    Text("PRÓXIMAS RESERVAS", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
                    val visible = reservations.filter { it.gymId == selectedGym.id }
                    if (visible.none { !it.isCancelled }) BookingEmpty("No hay reservas próximas", "Reserva una sala para entrenar en tu gimnasio.")
                    visible.filter { !it.isCancelled }.forEach { reservation -> ReservationCard(reservation, onCancel = { cancellation = reservation }) }
                    Button(onClick = { bookingGym = selectedGym }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.background)) { Text("NUEVA RESERVA", fontWeight = FontWeight.Bold) }
                    visible.filter { it.isCancelled }.takeIf { it.isNotEmpty() }?.let { cancelled -> Text("CANCELADAS", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textSecondary); cancelled.forEach { ReservationCard(it, {}) } }
                    }
                }
            }
        }
        PullRefreshIndicator(loading, refreshState, Modifier.align(Alignment.TopCenter))
    }
    if (joiningGym) JoinGymDialog(
        onDismiss = { joiningGym = false },
        onScan = { onCode ->
            gymCodeScanner.startScan().addOnSuccessListener { barcode ->
                barcode.rawValue?.let(onCode)
            }
        },
        onJoin = { code ->
            api.joinGym(code)
            joiningGym = false
            reload()
        },
    )
    cancellation?.let { reservation -> AlertDialog(onDismissRequest = { cancellation = null }, title = { Text("¿Cancelar esta reserva?") }, text = { Text("${reservation.roomName} · ${formatDateTime(reservation.startsAt, reservation.gymZone, locale)}") }, confirmButton = { TextButton(onClick = { cancellation = null; scope.launch { runCatching { api.cancel(reservation.id) }.onFailure { actionError = it.message ?: "No se pudo cancelar la reserva." }; reload() } }) { Text("CANCELAR") } }, dismissButton = { TextButton(onClick = { cancellation = null }) { Text("MANTENER") } }) }
    actionError?.let { message ->
        AlertDialog(
            onDismissRequest = { actionError = null },
            title = { Text("Algo salió mal") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { actionError = null }) { Text("OK") } },
        )
    }
}

/** Android counterpart of iOS's JoinGymView for an authenticated user. */
@Composable private fun JoinGymDialog(onDismiss: () -> Unit, onScan: (((String) -> Unit) -> Unit), onJoin: suspend (String) -> Unit) {
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
                    onValueChange = { code = it; localError = null },
                    label = { Text("Código de gimnasio") },
                    singleLine = true,
                    enabled = !submitting,
                    trailingIcon = {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = "Escanear código QR",
                            modifier = Modifier.clickable(enabled = !submitting) {
                                onScan { value ->
                                    val parsed = parseGymJoinCode(value)
                                    if (parsed == null) localError = "Este QR no contiene un código de gimnasio."
                                    else {
                                        code = parsed
                                        localError = null
                                        submitting = true
                                        scope.launch {
                                            runCatching { onJoin(parsed) }.onFailure { localError = it.message ?: "No se pudo unir al gimnasio." }
                                            submitting = false
                                        }
                                    }
                                }
                            },
                        )
                    },
                )
                localError?.let { Text(it, color = Color(0xFFE26D5A), fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = parseGymJoinCode(code) != null && !submitting,
                onClick = {
                    submitting = true
                    scope.launch {
                        runCatching { onJoin(requireNotNull(parseGymJoinCode(code))) }.onFailure { localError = it.message ?: "No se pudo unir al gimnasio." }
                        submitting = false
                    }
                },
            ) { if (submitting) CircularProgressIndicator(Modifier.size(18.dp)) else Text("UNIRSE") }
        },
        dismissButton = { TextButton(enabled = !submitting, onClick = onDismiss) { Text("CANCELAR") } },
    )
}

/** Profile entry point matching iOS's authenticated gym-membership section. */
@Composable
internal fun AuthenticatedJoinGymDialog(onDismiss: () -> Unit, onJoined: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val api = remember { BookingApi(context.applicationContext) }
    val gymCodeScanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }
    JoinGymDialog(
        onDismiss = onDismiss,
        onScan = { onCode ->
            gymCodeScanner.startScan().addOnSuccessListener { barcode -> barcode.rawValue?.let(onCode) }
        },
        onJoin = { code -> api.joinGym(code); onJoined() },
    )
}

@Composable
private fun GymHeading(gym: BookingGym, gyms: List<BookingGym>, onSelect: (BookingGym) -> Unit) {
    var menuOpen by remember(gym.id) { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(WildforceThemeTokens.backgroundSecondary, RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .padding(start = 28.dp, top = 8.dp, end = 28.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RemoteTrainingImage(
            gym.avatarUrl,
            gym.name,
            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)),
            showUnavailableLabel = false,
            unavailableContent = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(WildforceThemeTokens.accent.copy(alpha = .15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Business, contentDescription = null, tint = WildforceThemeTokens.accent, modifier = Modifier.size(20.dp))
                }
            },
        )
        Column(Modifier.weight(1f)) {
            if (gyms.size > 1) {
                Box {
                    Row(
                        Modifier.clickable { menuOpen = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(gym.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
                        Icon(Icons.Filled.ExpandMore, contentDescription = "Cambiar gimnasio", tint = WildforceThemeTokens.textSecondary, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        gyms.forEach { option ->
                            DropdownMenuItem(onClick = { menuOpen = false; onSelect(option) }) {
                                Text(option.name, color = WildforceThemeTokens.textPrimary, fontWeight = if (option.id == gym.id) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            } else {
                Text(gym.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, maxLines = 1)
            }
            Text(gym.address ?: gym.teamName, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, maxLines = 1)
        }
    }
}
@Composable private fun BookingEmpty(title: String, detail: String, onRetry: (() -> Unit)? = null, icon: ImageVector = Icons.Filled.EventAvailable) = Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, Modifier.size(42.dp), WildforceThemeTokens.textSecondary); Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center); Text(detail, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center); onRetry?.let { TextButton(onClick = it) { Text("REINTENTAR") } } }
@Composable
private fun ReservationCard(reservation: BookingReservation, onCancel: () -> Unit) {
    var menuOpen by remember(reservation.id) { mutableStateOf(false) }
    val locale = LocalLocale.current.platformLocale
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = WildforceThemeTokens.backgroundSecondary,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            reservation.roomAvatar?.let {
                RemoteTrainingImage(it, reservation.roomName, Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)), showUnavailableLabel = false)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(reservation.roomName, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.weight(1f))
                    BookingStatusBadge(reservation.status)
                    if (reservation.isCancellable) {
                        Box {
                            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.MoreVert, "Más opciones", tint = WildforceThemeTokens.textPrimary) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(onClick = { menuOpen = false; onCancel() }) {
                                    Icon(Icons.Filled.Close, null, tint = Color(0xFFE26D5A), modifier = Modifier.size(18.dp))
                                    Text("Cancelar reserva", Modifier.padding(start = 10.dp))
                                }
                            }
                        }
                    }
                }
                Text(formatRelativeDay(reservation.startsAt, reservation.gymZone, locale), fontSize = 14.sp, color = WildforceThemeTokens.textSecondary)
                Text("${formatTime(reservation.startsAt, reservation.gymZone, locale)} – ${formatTime(reservation.endsAt, reservation.gymZone, locale)}", fontSize = 14.sp, color = WildforceThemeTokens.textSecondary)
                if (!reservation.isCancelled && reservation.cancellableUntil != null) {
                    Text(
                        if (reservation.isCancellable) "Cancelación gratuita hasta ${formatDateTime(reservation.cancellableUntil, reservation.gymZone, locale)}" else "Ya no se puede cancelar",
                        style = MaterialTheme.typography.caption,
                        color = WildforceThemeTokens.textSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingStatusBadge(status: String) {
    val (label, color) = when {
        status.equals("confirmed", true) -> "CONFIRMADA" to Color(0xFF34C759)
        status.equals("waitlisted", true) -> "EN ESPERA" to Color(0xFFFF9500)
        else -> "CANCELADA" to Color(0xFF8E8E93)
    }
    Text(label, style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.background(color, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 2.dp))
}

@OptIn(ExperimentalMaterialApi::class)
@Composable private fun NewBookingScreen(gym: BookingGym, api: BookingApi, contentPadding: PaddingValues, onBack: () -> Unit, onBooked: () -> Unit) {
    val locale = LocalLocale.current.platformLocale
    var rooms by remember { mutableStateOf<List<BookingRoom>>(emptyList()) }; var selectedRoom by remember { mutableStateOf<BookingRoom?>(null) }; var selectedDate by remember { mutableStateOf(ZonedDateTime.now(gym.zone).toLocalDate()) }; var duration by remember { mutableIntStateOf(0) }; var slots by remember { mutableStateOf<List<BookingSlot>>(emptyList()) }; var selected by remember { mutableStateOf<BookingStart?>(null) }; var loading by remember { mutableStateOf(true) }; var availabilityLoading by remember { mutableStateOf(false) }; var refreshing by remember { mutableStateOf(false) }; var availabilityRequestId by remember { mutableIntStateOf(0) }; var roomError by remember { mutableStateOf<String?>(null) }; var availabilityError by remember { mutableStateOf<String?>(null) }; var error by remember { mutableStateOf<String?>(null) }; var confirm by remember { mutableStateOf(false) }; var reserving by remember { mutableStateOf(false) }; var confirmedReservation by remember { mutableStateOf<BookingReservation?>(null) }; var pendingAttempt by remember { mutableStateOf<BookingAttempt?>(null) }
    var confirmingStart by remember { mutableStateOf<BookingStart?>(null) }
    val scope = rememberCoroutineScope()
    val confirmationSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden, confirmValueChange = { it != ModalBottomSheetValue.Hidden || !reserving })
    BackHandler {
        when {
            reserving -> Unit
            confirmedReservation != null -> scope.launch {
                confirmationSheetState.hide()
                confirm = false
            }
            confirm -> scope.launch { confirmationSheetState.hide(); confirm = false }
            else -> onBack()
        }
    }
    fun selectRoom(room: BookingRoom) {
        if (room == selectedRoom) return
        selectedRoom = room
        if (!room.durations.contains(duration)) duration = room.durations.firstOrNull() ?: 0
        val lastAvailableDay = ZonedDateTime.now(gym.zone).toLocalDate().plusDays(room.windowDays.toLong())
        if (selectedDate.isAfter(lastAvailableDay)) selectedDate = lastAvailableDay
        selected = null
        error = null
    }
    suspend fun loadRooms() {
        loading = true
        roomError = null
        runCatching { api.rooms(gym.id) }
            .onSuccess { next ->
                rooms = next
                val nextRoom = next.firstOrNull { it.id == selectedRoom?.id } ?: next.firstOrNull()
                selectedRoom = null
                nextRoom?.let(::selectRoom)
            }
            .onFailure { roomError = it.message ?: "No se pudieron cargar las salas." }
        loading = false
    }
    suspend fun refreshAvailability() {
        val room = selectedRoom ?: return
        val requestedDate = selectedDate
        val requestId = ++availabilityRequestId
        refreshing = true
        availabilityLoading = true
        availabilityError = null
        runCatching { api.availability(room.id, requestedDate.toString()) }
            .onSuccess { next ->
                // A delayed response for a room or day the user already left must not replace the current grid.
                if (availabilityRequestId == requestId && selectedRoom?.id == room.id && selectedDate == requestedDate) {
                    slots = next
                    selected = refreshedBookingSelection(selected, next, duration)
                }
            }
            .onFailure {
                if (availabilityRequestId == requestId && selectedRoom?.id == room.id && selectedDate == requestedDate) {
                    slots = emptyList()
                    availabilityError = it.message ?: "No se pudo cargar la disponibilidad."
                }
            }
        if (availabilityRequestId == requestId) {
            if (selectedRoom?.id == room.id && selectedDate == requestedDate) availabilityLoading = false
            refreshing = false
        }
    }
    LaunchedEffect(gym.id) {
        // A new gym is a new booking flow, just like a new iOS navigation destination.
        selectedDate = ZonedDateTime.now(gym.zone).toLocalDate()
        rooms = emptyList()
        selectedRoom = null
        duration = 0
        slots = emptyList()
        selected = null
        availabilityError = null
        confirmedReservation = null
        pendingAttempt = null
        loadRooms()
    }
    LaunchedEffect(selectedRoom?.id, selectedDate) { selected = null; slots = emptyList(); availabilityError = null; error = null; refreshAvailability() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) scope.launch { refreshAvailability() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val starts = remember(slots, duration) { contiguousStarts(slots, duration) }
    val refreshState = rememberPullRefreshState(refreshing = refreshing, onRefresh = { scope.launch { refreshAvailability() } })
    LaunchedEffect(confirm, confirmedReservation) {
        if (confirm) confirmationSheetState.show() else confirmationSheetState.hide()
    }
    var sheetWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(confirmationSheetState.currentValue) {
        if (confirmationSheetState.currentValue != ModalBottomSheetValue.Hidden) {
            sheetWasVisible = true
        } else if (sheetWasVisible) {
            sheetWasVisible = false
            val wasBooked = confirmedReservation != null
            confirm = false
            confirmedReservation = null
            error = null
            confirmingStart = null
            if (wasBooked) onBooked()
        }
    }
    ModalBottomSheetLayout(
        sheetState = confirmationSheetState,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetBackgroundColor = WildforceThemeTokens.background,
        sheetContent = {
            confirmedReservation?.let { reservation ->
                BookingSuccessSheet(reservation, locale) {
                    scope.launch {
                        confirmationSheetState.hide()
                        confirm = false
                    }
                }
            } ?: confirmingStart?.let { start ->
                selectedRoom?.let { room ->
                    BookingConfirmationSheet(
                        gym = gym,
                        room = room,
                        start = start,
                        locale = locale,
                        error = error,
                        reserving = reserving,
                        onCancel = { scope.launch { confirmationSheetState.hide(); confirm = false } },
                        onConfirm = {
                            reserving = true
                            error = null
                            scope.launch {
                                val attempt = bookingAttemptFor(pendingAttempt, room.id, start)
                                pendingAttempt = attempt
                                runCatching { api.reserve(room.id, start, attempt.key) }
                                    .onSuccess { reservation -> pendingAttempt = null; confirmedReservation = reservation; reserving = false; refreshAvailability() }
                                    .onFailure { failure ->
                                        error = failure.message ?: "No se pudo confirmar la reserva."
                                        reserving = false
                                        if (isDefiniteBookingRejection(failure)) { pendingAttempt = null; refreshAvailability() }
                                    }
                            }
                        },
                    )
                } ?: Spacer(Modifier.height(1.dp))
            } ?: Spacer(Modifier.height(1.dp))
        },
    ) {
        Box(Modifier.fillMaxSize().pullRefresh(refreshState)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .padding(bottom = if (selectedRoom != null) 76.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Filled.Close, "Volver") }; Column { Text("NUEVA RESERVA", fontSize = 22.sp, fontWeight = FontWeight.Black, color = WildforceThemeTokens.textPrimary); Text(gym.name, color = WildforceThemeTokens.textSecondary) } }
            if (loading) Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } else if (roomError != null) BookingEmpty("No se pudieron cargar las salas", roomError!!, onRetry = { scope.launch { loadRooms() } }, icon = Icons.Filled.WarningAmber) else if (rooms.isEmpty()) BookingEmpty("No hay salas disponibles", "Este gimnasio no tiene salas abiertas para reservar.") else {
                BookingSection("DÍA") { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(bookingWindowDays(ZonedDateTime.now(gym.zone).toLocalDate(), selectedRoom!!.windowDays)) { day -> BookingDayChoice(day, day == selectedDate, onClick = { selectedDate = day }) } } }
                BookingSection("SALA") {
                    val showsImages = rooms.any { it.avatarUrl != null }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(rooms, key = { it.id }) { room ->
                            RoomChoice(room, room == selectedRoom, showsImages = showsImages, locale = locale, onClick = { selectRoom(room) })
                        }
                    }
                }
                if (selectedRoom!!.durations.size > 1) BookingSection("DURACIÓN") {
                    BookingDurationPicker(
                        durations = selectedRoom!!.durations,
                        selection = duration,
                        locale = locale,
                        onSelect = { duration = it; selected = refreshedBookingSelection(selected, slots, it); error = null },
                    )
                }
                BookingSection("HORAS DISPONIBLES") { when { availabilityError != null -> BookingEmpty("No se pudo cargar la disponibilidad", availabilityError!!, onRetry = { scope.launch { refreshAvailability() } }, icon = Icons.Filled.WarningAmber); availabilityLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; starts.isEmpty() -> BookingEmpty("No hay horas disponibles", "Prueba otro día o una duración más corta."); else -> FlowTimes(starts, selected, gym.zone, locale, { selected = it; error = null }) } }
                 if (gym.zone != ZoneId.systemDefault()) {
                     Text(
                         "Las horas se muestran en la zona horaria del gimnasio (${gym.zone.id}).",
                         style = MaterialTheme.typography.caption,
                         color = WildforceThemeTokens.textSecondary,
                     )
                 }
            }
            }
            selectedRoom?.let {
                Button(
                    onClick = { selected?.let { start -> confirmingStart = start; confirm = true } },
                    enabled = selected != null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = WildforceThemeTokens.textPrimary,
                        contentColor = WildforceThemeTokens.background,
                        disabledBackgroundColor = WildforceThemeTokens.textPrimary.copy(alpha = 0.12f),
                        disabledContentColor = WildforceThemeTokens.textSecondary,
                    ),
                ) { Text("CONTINUAR", fontWeight = FontWeight.Bold) }
            }
            PullRefreshIndicator(refreshing, refreshState, Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun BookingConfirmationSheet(
    gym: BookingGym,
    room: BookingRoom,
    start: BookingStart,
    locale: Locale,
    error: String?,
    reserving: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = !reserving, onClick = onCancel) { Text(stringResource(R.string.ios_catalog_19766ed6ccb2f4a3)) }
            Text(stringResource(R.string.ios_catalog_a52a24719590f68b), fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(64.dp))
        }
        Card(Modifier.fillMaxWidth(), backgroundColor = WildforceThemeTokens.backgroundSecondary, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BookingSummaryRow("Sala", room.name)
                Divider()
                BookingSummaryRow("Gimnasio", gym.name)
                Divider()
                BookingSummaryRow("Día", formatRelativeDay(start.startsAt, gym.zone, locale))
                Divider()
                BookingSummaryRow("Hora", "${formatTime(start.startsAt, gym.zone, locale)} – ${formatTime(start.endsAt, gym.zone, locale)}")
            }
        }
        Text("Puedes cancelar gratis hasta ${formatDuration(room.cancellationCutoff, locale)} antes de que empiece.", color = WildforceThemeTokens.textSecondary, fontSize = 13.sp)
        error?.let {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp))
                Text(it, color = Color(0xFFFF9500), fontSize = 13.sp)
            }
        }
        Button(
            onClick = onConfirm,
            enabled = !reserving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.background),
        ) {
            if (reserving) CircularProgressIndicator(Modifier.size(18.dp), color = WildforceThemeTokens.background, strokeWidth = 2.dp) else Text(if (error == null) stringResource(R.string.ios_catalog_a52a24719590f68b).uppercase(locale) else "REINTENTAR", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BookingSummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = WildforceThemeTokens.textSecondary)
        Text(value, color = WildforceThemeTokens.textPrimary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
private fun BookingSuccessSheet(reservation: BookingReservation, locale: Locale, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(72.dp), tint = Color(0xFF34C759))
        Text("RESERVA CONFIRMADA", fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        if (reservation.roomAvatar != null) RemoteTrainingImage(reservation.roomAvatar, reservation.roomName, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)))
        Text(reservation.roomName, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Text("${formatRelativeDay(reservation.startsAt, reservation.gymZone, locale)} · ${formatTime(reservation.startsAt, reservation.gymZone, locale)} – ${formatTime(reservation.endsAt, reservation.gymZone, locale)}", color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.background)) { Text("LISTO", fontWeight = FontWeight.Bold) }
    }
}
@Composable private fun BookingSection(title: String, content: @Composable () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary); content() }
/** Mirrors SwiftUI's segmented duration picker instead of a horizontally scrolling chip list. */
@Composable
private fun BookingDurationPicker(
    durations: List<Int>,
    selection: Int,
    locale: Locale,
    onSelect: (Int) -> Unit,
) {
    val outerShape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(outerShape)
            .background(WildforceThemeTokens.textSecondary.copy(alpha = .12f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        durations.forEach { minutes ->
            val selected = minutes == selection
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) WildforceThemeTokens.textPrimary else Color.Transparent)
                    .clickable { onSelect(minutes) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    formatDuration(minutes, locale),
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
@Composable
private fun BookingDayChoice(day: java.time.LocalDate, selected: Boolean, onClick: () -> Unit) {
    val locale = LocalLocale.current.platformLocale
    Column(
        Modifier
            .width(52.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.backgroundSecondary)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(day.format(DateTimeFormatter.ofPattern("EEE", locale)), style = MaterialTheme.typography.caption, color = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary)
        Text(day.dayOfMonth.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary)
    }
}

@Composable
private fun RoomChoice(room: BookingRoom, selected: Boolean, showsImages: Boolean, locale: Locale, onClick: () -> Unit) {
    val foreground = if (selected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary
    val secondary = if (selected) foreground.copy(alpha = .7f) else WildforceThemeTokens.textSecondary
    Card(modifier = Modifier.width(150.dp).clickable(onClick = onClick), backgroundColor = if (selected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.backgroundSecondary, shape = RoundedCornerShape(14.dp)) {
        Column {
            if (showsImages) {
                RemoteTrainingImage(
                    room.avatarUrl,
                    room.name,
                    Modifier.fillMaxWidth().height(120.dp),
                    showUnavailableLabel = false,
                    unavailableContent = { Box(Modifier.fillMaxSize().background(WildforceThemeTokens.textPrimary.copy(alpha = .08f))) },
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(room.name, fontWeight = FontWeight.Bold, color = foreground, minLines = 2, maxLines = 2)
                Text(stringResource(R.string.ios_catalog_e53c3780fcde4a4f).replace("%d", room.capacity.toString()), color = secondary, fontSize = 12.sp)
                Text("${formatDuration(room.minDuration, locale)} – ${formatDuration(room.maxDuration, locale)}", color = secondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FlowTimes(starts: List<BookingStart>, selected: BookingStart?, zone: ZoneId, locale: Locale, onSelected: (BookingStart) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = ((maxWidth.value + 8f) / 108f).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            starts.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { start ->
                        val isSelected = selected?.startsAt?.toInstant() == start.startsAt.toInstant()
                        val foreground = if (isSelected) WildforceThemeTokens.primaryButtonText else WildforceThemeTokens.textPrimary
                        val placesColor = if (isSelected) foreground.copy(alpha = .7f) else if (start.availablePlaces == 1) Color(0xFFFF9500) else WildforceThemeTokens.textSecondary
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) WildforceThemeTokens.textPrimary else WildforceThemeTokens.backgroundSecondary)
                                .semantics { this.selected = isSelected }.clickable { onSelected(start) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(formatTime(start.startsAt, zone, locale), fontWeight = FontWeight.SemiBold, color = foreground)
                            Text(stringResource(R.string.ios_catalog_885c42f8b9f562c6).replace("%d", start.availablePlaces.toString()), fontSize = 11.sp, color = placesColor)
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
/** Every booking instant is rendered in the gym's time zone, as on iOS. */
private fun formatDateTime(time: OffsetDateTime, zone: ZoneId, locale: Locale) = time.atZoneSameInstant(zone).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
private fun formatRelativeDay(time: OffsetDateTime, zone: ZoneId, locale: Locale): String {
    val date = time.atZoneSameInstant(zone).toLocalDate()
    val today = ZonedDateTime.now(zone).toLocalDate()
    val prefix = when (date) {
        today -> "Hoy, "
        today.plusDays(1) -> "Mañana, "
        else -> ""
    }
    val pattern = DateTimePatternGenerator.getInstance(locale).getBestPattern("dMMMM")
    return prefix + time.atZoneSameInstant(zone).format(DateTimeFormatter.ofPattern(pattern, locale))
}
private fun formatTime(time: OffsetDateTime, zone: ZoneId, locale: Locale) = time.atZoneSameInstant(zone).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
private fun formatDuration(minutes: Int, locale: Locale): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    val measures = mutableListOf<Measure>()
    if (hours > 0) measures += Measure(hours, MeasureUnit.HOUR)
    if (remainingMinutes > 0 || hours == 0) measures += Measure(remainingMinutes, MeasureUnit.MINUTE)
    return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT).formatMeasures(*measures.toTypedArray())
}
