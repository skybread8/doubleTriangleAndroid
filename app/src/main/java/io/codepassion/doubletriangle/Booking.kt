package io.codepassion.doubletriangle

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.provider.CalendarContract
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.alpha
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
internal fun bookingRequestTimestamp(value: OffsetDateTime): String =
    value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx", Locale.ROOT))
internal data class BookingReservation(val id: String, val status: String, val startsAt: OffsetDateTime, val endsAt: OffsetDateTime, val cancellableUntil: OffsetDateTime?, val roomName: String, val roomAvatar: String?, val gymId: String, val gymName: String, val gymAvatar: String?, val gymZone: ZoneId) {
    val isCancelled get() = status.equals("cancelled", true)
    fun isCancellableAt(now: OffsetDateTime): Boolean = !isCancelled && cancellableUntil?.isAfter(now) == true
    val isCancellable get() = isCancellableAt(OffsetDateTime.now())
}
internal fun overlapsActiveReservation(start: BookingStart, reservations: List<BookingReservation>): Boolean =
    reservations.any { reservation ->
        !reservation.isCancelled && start.startsAt.toInstant() < reservation.endsAt.toInstant() &&
            reservation.startsAt.toInstant() < start.endsAt.toInstant()
    }

internal data class BookingCalendarEvent(val title: String, val location: String, val beginMillis: Long, val endMillis: Long, val timeZone: String)

internal fun bookingCalendarEvent(reservation: BookingReservation, address: String?): BookingCalendarEvent = BookingCalendarEvent(
    title = "${reservation.roomName} · ${reservation.gymName}",
    location = listOfNotNull(reservation.gymName, address?.trim()?.takeIf(String::isNotEmpty)).joinToString(", "),
    beginMillis = reservation.startsAt.toInstant().toEpochMilli(),
    endMillis = reservation.endsAt.toInstant().toEpochMilli(),
    timeZone = reservation.gymZone.id,
)

private fun openBookingCalendar(context: Context, reservation: BookingReservation, address: String?) {
    val event = bookingCalendarEvent(reservation, address)
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, event.title)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.beginMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endMillis)
        .putExtra(CalendarContract.Events.EVENT_TIMEZONE, event.timeZone)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.booking_no_calendar_app, Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.booking_no_calendar_app, Toast.LENGTH_LONG).show()
    }
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
        try {
            val data = connection("gym-memberships/codes/$normalized", "GET", authenticated = false).useResponse().getJSONObject("data")
            GymJoinCodeInfo(normalized, data.getJSONObject("team").getString("name"))
        } catch (error: BookingHttpException) {
            if (error.status != 422) throw error
            val data = connection("coach/join-codes/$normalized", "GET", authenticated = false).useResponse().getJSONObject("data")
            GymJoinCodeInfo(normalized, data.getJSONObject("coach").getString("name"), isCoach = true)
        }
    }
    suspend fun joinCoach(code: String) { post("coach/join", JSONObject().put("code", code.trim().uppercase())) }
    suspend fun reserve(roomId: String, start: BookingStart, idempotencyKey: UUID): BookingReservation = post("booking/rooms/$roomId/reservations", JSONObject().put("starts_at", bookingRequestTimestamp(start.startsAt)).put("ends_at", bookingRequestTimestamp(start.endsAt)), idempotencyKey).getJSONObject("data").let(::reservation)
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
        if (responseCode !in 200..299) {
            val payload = runCatching { JSONObject(body) }.getOrNull()
            throw BookingHttpException(responseCode, payload?.optString("message").orEmpty(), payload?.optString("code")?.takeIf(String::isNotBlank))
        }
        return JSONObject(body)
    }
}

internal fun bookingErrorResource(error: Throwable): Int = when ((error as? BookingHttpException)?.code) {
        "room_full" -> io.codepassion.doubletriangle.core.designsystem.R.string.someone_just_took_the_last_place_for_this_time__availability_has_been_refreshed_
        "time_not_available" -> io.codepassion.doubletriangle.core.designsystem.R.string.this_time_is_no_longer_available__the_room_may_have_been_closed_
        "starts_in_past" -> io.codepassion.doubletriangle.core.designsystem.R.string.this_time_has_already_started__choose_a_later_one_
        "outside_booking_window" -> io.codepassion.doubletriangle.core.designsystem.R.string.this_time_is_outside_the_room_s_booking_window_
        "invalid_duration" -> io.codepassion.doubletriangle.core.designsystem.R.string.this_duration_isn_t_allowed_in_this_room_
        "client_already_reserved" -> io.codepassion.doubletriangle.core.designsystem.R.string.you_already_have_another_reservation_at_this_time_
        "room_not_reservable" -> io.codepassion.doubletriangle.core.designsystem.R.string.this_room_can_t_be_booked_right_now_
        "not_active_client" -> io.codepassion.doubletriangle.core.designsystem.R.string.your_gym_membership_is_no_longer_active_
        "cancellation_closed" -> io.codepassion.doubletriangle.core.designsystem.R.string.it_s_too_late_to_cancel_this_reservation_
        else -> if (error is java.io.IOException) io.codepassion.doubletriangle.core.designsystem.R.string.you_seem_to_be_offline__connect_to_the_internet_and_try_again_
            else io.codepassion.doubletriangle.core.designsystem.R.string.the_operation_could_not_be_completed__please_try_again_
    }

private fun bookingErrorMessage(context: android.content.Context, error: Throwable): String =
    context.getString(bookingErrorResource(error))

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
    var gyms by remember { mutableStateOf<List<BookingGym>>(emptyList()) }
    val bookingPreferences = remember { context.getSharedPreferences("wildforce_booking", Context.MODE_PRIVATE) }
    var selectedGymId by remember { mutableStateOf(bookingPreferences.getString(SELECTED_BOOKING_GYM_ID, null)) }
    var reservations by remember { mutableStateOf<List<BookingReservation>>(emptyList()) }; var loading by remember { mutableStateOf(true) }; var error by remember { mutableStateOf<String?>(null) }; var actionError by remember { mutableStateOf<String?>(null) }
    var bookingGym by remember { mutableStateOf<BookingGym?>(null) }; var cancellation by remember { mutableStateOf<BookingReservation?>(null) }
    var cancellingReservationId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(bookingGym != null) { onBookingVisibilityChanged(bookingGym != null) }
    DisposableEffect(Unit) { onDispose { onBookingVisibilityChanged(false) } }
    var joiningGym by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun reload() { loading = true; error = null; runCatching { api.gyms() to api.reservations() }.onSuccess { (nextGyms, nextReservations) -> gyms = nextGyms; selectedGymId = selectedGymId?.takeIf { id -> nextGyms.any { it.id == id } } ?: nextGyms.firstOrNull()?.id; bookingPreferences.edit().putString(SELECTED_BOOKING_GYM_ID, selectedGymId).apply(); reservations = nextReservations }.onFailure { error = bookingErrorMessage(context, it) }; loading = false }
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
                    error != null -> BookingEmpty(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.couldn_t_load_your_reservations), error!!, onRetry = { scope.launch { reload() } })
                    selectedGym == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BookingEmpty(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.join_your_gym), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.enter_the_code_your_gym_gave_you__or_scan_its_qr_))
                        OutlinedButton(onClick = { joiningGym = true }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.i_have_a_gym_code).uppercase()) }
                    }
                    else -> {
                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.upcoming_reservations), fontSize = 18.sp, color = WildforceThemeTokens.textPrimary)
                    val visible = reservations.filter { it.gymId == selectedGym.id }
                    if (visible.none { !it.isCancelled }) BookingEmpty(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.no_upcoming_reservations), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.book_a_room_to_train_at_your_gym_))
                    visible.filter { !it.isCancelled }.forEach { reservation -> ReservationCard(reservation, cancelling = cancellingReservationId == reservation.id, onCancel = { cancellation = reservation }, onCalendar = { openBookingCalendar(context, reservation, selectedGym.address) }) }
                    Button(onClick = { bookingGym = selectedGym }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.background)) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_new_booking).uppercase(), fontWeight = FontWeight.Bold) }
                    visible.filter { it.isCancelled }.takeIf { it.isNotEmpty() }?.let { cancelled -> Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_cancelled), fontSize = 18.sp, color = WildforceThemeTokens.textPrimary, modifier = Modifier.padding(top = 16.dp)); cancelled.forEach { ReservationCard(it, onCancel = {}, onCalendar = {}) } }
                    }
                }
            }
        }
        PullRefreshIndicator(loading, refreshState, Modifier.align(Alignment.TopCenter))
    }
    if (joiningGym) JoinGymDialog(
        onDismiss = { joiningGym = false },
        onJoin = { code ->
            api.joinGym(code)
            joiningGym = false
            reload()
        },
    )
    cancellation?.let { reservation ->
        AlertDialog(
            onDismissRequest = { cancellation = null },
            title = { Text(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.cancel_this_reservation_)) },
            text = { Text("${reservation.roomName} · ${formatDateTime(reservation.startsAt, reservation.gymZone, locale)}") },
            confirmButton = {
                TextButton(onClick = {
                    cancellation = null
                    cancellingReservationId = reservation.id
                    scope.launch {
                        try {
                            runCatching { api.cancel(reservation.id) }.onFailure {
                                if (it is kotlinx.coroutines.CancellationException) throw it
                                actionError = bookingErrorMessage(context, it)
                            }
                            reload()
                        } finally {
                            cancellingReservationId = null
                        }
                    }
                }) { Text(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.cancel_reservation), color = MaterialTheme.colors.error) }
            },
            dismissButton = { TextButton(onClick = { cancellation = null }) { Text(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.keep_it)) } },
        )
    }
    actionError?.let { message ->
        AlertDialog(
            onDismissRequest = { actionError = null },
            title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_something_went_wrong)) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { actionError = null }) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_565339bc4d33d728)) } },
        )
    }
}

/** Android counterpart of iOS's JoinGymView for an authenticated user. */
@Composable private fun JoinGymDialog(onDismiss: () -> Unit, onJoin: suspend (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var code by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.join_your_gym).uppercase()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_enter_the_code_your_gym_gave_you__or_scan_its_qr_), color = WildforceThemeTokens.textSecondary)
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; localError = null },
                    label = { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_gym_code)) },
                    singleLine = true,
                    enabled = !submitting,
                    trailingIcon = {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_scan_the_gym_s_qr),
                            modifier = Modifier.clickable(enabled = !submitting) {
                                runCatching {
                                    val options = GmsBarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                        .enableAutoZoom()
                                        .build()
                                    GmsBarcodeScanning.getClient(context, options).startScan()
                                    .addOnSuccessListener { barcode -> barcode.rawValue?.let { value ->
                                    val parsed = parseGymJoinCode(value)
                                    if (parsed == null) localError = context.getString(io.codepassion.doubletriangle.core.designsystem.R.string.this_qr_is_not_a_gym_join_code_)
                                    else {
                                        code = parsed
                                        localError = null
                                        submitting = true
                                        scope.launch {
                                            runCatching { onJoin(parsed) }.onFailure { localError = bookingErrorMessage(context, it) }
                                            submitting = false
                                        }
                                    }
                                    } }
                                    .addOnFailureListener { localError = context.getString(io.codepassion.doubletriangle.core.designsystem.R.string.the_operation_could_not_be_completed__please_try_again_) }
                                }.onFailure { localError = context.getString(io.codepassion.doubletriangle.core.designsystem.R.string.the_operation_could_not_be_completed__please_try_again_) }
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
                        runCatching { onJoin(requireNotNull(parseGymJoinCode(code))) }.onFailure { localError = bookingErrorMessage(context, it) }
                        submitting = false
                    }
                },
            ) { if (submitting) CircularProgressIndicator(Modifier.size(18.dp)) else Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.join).uppercase()) }
        },
        dismissButton = { TextButton(enabled = !submitting, onClick = onDismiss) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_cancel).uppercase()) } },
    )
}

/** Profile entry point matching iOS's authenticated gym-membership section. */
@Composable
internal fun AuthenticatedJoinGymDialog(onDismiss: () -> Unit, onJoined: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val api = remember { BookingApi(context.applicationContext) }
    JoinGymDialog(
        onDismiss = onDismiss,
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
                        Icon(Icons.Filled.ExpandMore, contentDescription = androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.gym), tint = WildforceThemeTokens.textSecondary, modifier = Modifier.size(18.dp))
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
@Composable private fun BookingEmpty(title: String, detail: String, onRetry: (() -> Unit)? = null, icon: ImageVector = Icons.Filled.EventAvailable) = Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, Modifier.size(42.dp), WildforceThemeTokens.textSecondary); Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center); Text(detail, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center); onRetry?.let { TextButton(onClick = it) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_retry).uppercase()) } } }
@Composable
private fun ReservationCard(reservation: BookingReservation, cancelling: Boolean = false, onCancel: () -> Unit, onCalendar: () -> Unit) {
    var menuOpen by remember(reservation.id) { mutableStateOf(false) }
    val locale = LocalLocale.current.platformLocale
    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (cancelling) .4f else if (reservation.isCancelled) .6f else 1f),
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
                    if (!reservation.isCancelled) {
                        Box {
                            IconButton(onClick = { menuOpen = true }, enabled = !cancelling, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.MoreVert, androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_more_options), tint = WildforceThemeTokens.textPrimary) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(onClick = { menuOpen = false; onCalendar() }) {
                                    Icon(Icons.Filled.CalendarMonth, null, tint = WildforceThemeTokens.textPrimary, modifier = Modifier.size(18.dp))
                                    Text(stringResource(R.string.booking_add_to_calendar), Modifier.padding(start = 10.dp))
                                }
                                if (reservation.isCancellable) DropdownMenuItem(onClick = { menuOpen = false; onCancel() }) {
                                    Icon(Icons.Filled.Close, null, tint = Color(0xFFE26D5A), modifier = Modifier.size(18.dp))
                                    Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.cancel_reservation), Modifier.padding(start = 10.dp))
                                }
                            }
                        }
                    }
                }
                Text(formatRelativeDay(reservation.startsAt, reservation.gymZone, locale), fontSize = 14.sp, color = WildforceThemeTokens.textSecondary)
                Text("${formatTime(reservation.startsAt, reservation.gymZone, locale)} – ${formatTime(reservation.endsAt, reservation.gymZone, locale)}", fontSize = 14.sp, color = WildforceThemeTokens.textSecondary)
                if (!reservation.isCancelled && reservation.cancellableUntil != null) {
                    Text(
                        if (reservation.isCancellable) androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.free_cancellation_until___, formatDateTime(reservation.cancellableUntil, reservation.gymZone, locale)) else androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.cancellation_is_no_longer_possible),
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
    val (labelId, color) = when {
        status.equals("confirmed", true) -> io.codepassion.doubletriangle.core.designsystem.R.string.confirmed to Color(0xFF34C759)
        status.equals("waitlisted", true) -> io.codepassion.doubletriangle.core.designsystem.R.string.waitlisted to Color(0xFFFF9500)
        else -> io.codepassion.doubletriangle.core.designsystem.R.string.cancelled to Color(0xFF8E8E93)
    }
    val label = stringResource(labelId).uppercase(LocalLocale.current.platformLocale)
    Text(label, style = MaterialTheme.typography.overline, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.background(color, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 2.dp))
}

@OptIn(ExperimentalMaterialApi::class)
@Composable private fun NewBookingScreen(gym: BookingGym, api: BookingApi, contentPadding: PaddingValues, onBack: () -> Unit, onBooked: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val locale = LocalLocale.current.platformLocale
    var rooms by remember { mutableStateOf<List<BookingRoom>>(emptyList()) }; var selectedRoom by remember { mutableStateOf<BookingRoom?>(null) }; var selectedDate by remember { mutableStateOf(ZonedDateTime.now(gym.zone).toLocalDate()) }; var duration by remember { mutableIntStateOf(0) }; var slots by remember { mutableStateOf<List<BookingSlot>>(emptyList()) }; var selected by remember { mutableStateOf<BookingStart?>(null) }; var loading by remember { mutableStateOf(true) }; var availabilityLoading by remember { mutableStateOf(false) }; var refreshing by remember { mutableStateOf(false) }; var availabilityRequestId by remember { mutableIntStateOf(0) }; var roomError by remember { mutableStateOf<String?>(null) }; var availabilityError by remember { mutableStateOf<String?>(null) }; var error by remember { mutableStateOf<String?>(null) }; var confirm by remember { mutableStateOf(false) }; var reserving by remember { mutableStateOf(false) }; var confirmedReservation by remember { mutableStateOf<BookingReservation?>(null) }; var pendingAttempt by remember { mutableStateOf<BookingAttempt?>(null) }
    var confirmingStart by remember { mutableStateOf<BookingStart?>(null) }
    var existingReservations by remember { mutableStateOf<List<BookingReservation>>(emptyList()) }
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
            .onFailure { roomError = bookingErrorMessage(context, it) }
        loading = false
    }
    suspend fun refreshAvailability() {
        val room = selectedRoom ?: return
        val requestedDate = selectedDate
        val requestId = ++availabilityRequestId
        refreshing = true
        availabilityLoading = true
        availabilityError = null
        runCatching { api.availability(room.id, requestedDate.toString()) to api.reservations() }
            .onSuccess { (next, reservations) ->
                // A delayed response for a room or day the user already left must not replace the current grid.
                if (availabilityRequestId == requestId && selectedRoom?.id == room.id && selectedDate == requestedDate) {
                    slots = next
                    existingReservations = reservations
                    selected = refreshedBookingSelection(selected, next, duration)?.takeUnless { overlapsActiveReservation(it, reservations) }
                }
            }
            .onFailure {
                if (availabilityRequestId == requestId && selectedRoom?.id == room.id && selectedDate == requestedDate) {
                    slots = emptyList()
                    selected = null
                    availabilityError = bookingErrorMessage(context, it)
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
                BookingSuccessSheet(reservation, locale, onCalendar = { openBookingCalendar(context, reservation, gym.address) }) {
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
                                if (availabilityLoading || availabilityError != null || overlapsActiveReservation(start, existingReservations)) {
                                    error = context.getString(if (overlapsActiveReservation(start, existingReservations)) io.codepassion.doubletriangle.core.designsystem.R.string.you_already_have_another_reservation_at_this_time_ else io.codepassion.doubletriangle.core.designsystem.R.string.couldn_t_load_availability)
                                    reserving = false
                                    return@launch
                                }
                                val attempt = bookingAttemptFor(pendingAttempt, room.id, start)
                                pendingAttempt = attempt
                                runCatching { api.reserve(room.id, start, attempt.key) }
                                    .onSuccess { reservation -> pendingAttempt = null; confirmedReservation = reservation; reserving = false; refreshAvailability() }
                                    .onFailure { failure ->
                                        error = bookingErrorMessage(context, failure)
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
            Box(Modifier.fillMaxWidth().heightIn(min = 56.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.action_back), tint = WildforceThemeTokens.textPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 56.dp)) {
                    Text(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_new_booking), fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, textAlign = TextAlign.Center)
                    Text(gym.name, style = MaterialTheme.typography.caption, color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
            if (loading) Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } else if (roomError != null) BookingEmpty(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.couldn_t_load_the_rooms), roomError!!, onRetry = { scope.launch { loadRooms() } }, icon = Icons.Filled.WarningAmber) else if (rooms.isEmpty()) BookingEmpty(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.no_rooms_to_book), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.this_gym_has_no_rooms_open_for_booking_yet_)) else {
                BookingSection(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_day)) { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(bookingWindowDays(ZonedDateTime.now(gym.zone).toLocalDate(), selectedRoom!!.windowDays)) { day -> BookingDayChoice(day, day == selectedDate, onClick = { selectedDate = day }) } } }
                BookingSection(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.room)) {
                    val showsImages = rooms.any { it.avatarUrl != null }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(rooms, key = { it.id }) { room ->
                            RoomChoice(room, room == selectedRoom, showsImages = showsImages, locale = locale, onClick = { selectRoom(room) })
                        }
                    }
                }
                if (selectedRoom!!.durations.size > 1) BookingSection(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_duration)) {
                    BookingDurationPicker(
                        durations = selectedRoom!!.durations,
                        selection = duration,
                        locale = locale,
                        onSelect = { duration = it; selected = refreshedBookingSelection(selected, slots, it)?.takeUnless { start -> overlapsActiveReservation(start, existingReservations) }; error = null },
                    )
                }
                BookingSection(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.available_times)) { when { availabilityError != null -> BookingEmpty(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.couldn_t_load_availability), availabilityError!!, onRetry = { scope.launch { refreshAvailability() } }, icon = Icons.Filled.WarningAmber); availabilityLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; starts.isEmpty() -> BookingEmpty(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.no_times_available), androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.try_another_day_or_a_shorter_duration_)); else -> FlowTimes(starts, selected, existingReservations, gym.zone, locale, { selected = it; error = null }) } }
                 if (gym.zone != ZoneId.systemDefault()) {
                     Text(
                         androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.times_are_shown_in_the_gym_s_time_zone______, gym.zone.id),
                         style = MaterialTheme.typography.caption,
                         color = WildforceThemeTokens.textSecondary,
                     )
                 }
            }
            }
            selectedRoom?.let {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(WildforceThemeTokens.background).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Button(
                        onClick = { selected?.let { start -> confirmingStart = start; confirm = true } },
                        enabled = selected != null,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = WildforceThemeTokens.textPrimary,
                            contentColor = WildforceThemeTokens.background,
                            disabledBackgroundColor = WildforceThemeTokens.textPrimary.copy(alpha = 0.12f),
                            disabledContentColor = WildforceThemeTokens.textSecondary,
                        ),
                    ) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_key_continue).uppercase(locale), fontWeight = FontWeight.Bold) }
                }
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
            TextButton(enabled = !reserving, onClick = onCancel) { Text(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_cancel)) }
            Text(stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_confirm_reservation), fontWeight = FontWeight.SemiBold, color = WildforceThemeTokens.textPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.width(64.dp))
        }
        Card(Modifier.fillMaxWidth(), backgroundColor = WildforceThemeTokens.backgroundSecondary, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BookingSummaryRow(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.room), room.name)
                Divider()
                BookingSummaryRow(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.gym), gym.name)
                Divider()
                BookingSummaryRow(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_day), formatRelativeDay(start.startsAt, gym.zone, locale))
                Divider()
                BookingSummaryRow(androidx.compose.ui.res.stringResource(io.codepassion.wildforce.android.R.string.booking_time_of_day), "${formatTime(start.startsAt, gym.zone, locale)} – ${formatTime(start.endsAt, gym.zone, locale)}")
            }
        }
        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.android_fallback_2040464160c3581e, formatDuration(room.cancellationCutoff, locale)), color = WildforceThemeTokens.textSecondary, fontSize = 13.sp)
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
            if (reserving) CircularProgressIndicator(Modifier.size(18.dp), color = WildforceThemeTokens.background, strokeWidth = 2.dp) else Text(if (error == null) stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_confirm_reservation).uppercase(locale) else stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_retry).uppercase(locale), fontWeight = FontWeight.Bold)
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
private fun BookingSuccessSheet(reservation: BookingReservation, locale: Locale, onCalendar: () -> Unit, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(72.dp), tint = Color(0xFF34C759))
        Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.you_re_booked), style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        if (reservation.roomAvatar != null) RemoteTrainingImage(reservation.roomAvatar, reservation.roomName, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)))
        Text(reservation.roomName, fontWeight = FontWeight.Bold, color = WildforceThemeTokens.textPrimary)
        Text("${formatRelativeDay(reservation.startsAt, reservation.gymZone, locale)} · ${formatTime(reservation.startsAt, reservation.gymZone, locale)} – ${formatTime(reservation.endsAt, reservation.gymZone, locale)}", color = WildforceThemeTokens.textSecondary, textAlign = TextAlign.Center)
        TextButton(onClick = onCalendar) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp), tint = WildforceThemeTokens.textPrimary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.booking_add_to_calendar), color = WildforceThemeTokens.textPrimary)
        }
        Spacer(Modifier.height(14.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(backgroundColor = WildforceThemeTokens.textPrimary, contentColor = WildforceThemeTokens.background)) { Text(androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.done).uppercase(locale), fontWeight = FontWeight.Bold) }
    }
}
@Composable private fun BookingSection(title: String, content: @Composable () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, fontSize = 18.sp, color = WildforceThemeTokens.textPrimary); content() }
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
private fun FlowTimes(starts: List<BookingStart>, selected: BookingStart?, reservations: List<BookingReservation>, zone: ZoneId, locale: Locale, onSelected: (BookingStart) -> Unit) {
    val places = stringResource(R.string.ios_catalog_885c42f8b9f562c6)
    val reserved = stringResource(R.string.booking_already_reserved)
    io.codepassion.doubletriangle.core.designsystem.TimeChoiceGrid(
        options = starts,
        selected = starts.firstOrNull { it.startsAt.toInstant() == selected?.startsAt?.toInstant() },
        label = { formatTime(it.startsAt, zone, locale) },
        detail = { if (overlapsActiveReservation(it, reservations)) "✓ $reserved" else places.replace("%d", it.availablePlaces.toString()) },
        detailColor = { if (overlapsActiveReservation(it, reservations)) { if (MaterialTheme.colors.isLight) Color(0xFF16823B) else Color(0xFF34C759) } else if (it.availablePlaces == 1) Color(0xFFFF9500) else WildforceThemeTokens.textSecondary },
        isEnabled = { !overlapsActiveReservation(it, reservations) },
        onSelect = onSelected,
    )
}
/** Every booking instant is rendered in the gym's time zone, as on iOS. */
private fun formatDateTime(time: OffsetDateTime, zone: ZoneId, locale: Locale) = time.atZoneSameInstant(zone).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale))
@Composable
private fun formatRelativeDay(time: OffsetDateTime, zone: ZoneId, locale: Locale): String {
    val date = time.atZoneSameInstant(zone).toLocalDate()
    val today = ZonedDateTime.now(zone).toLocalDate()
    val prefix = when (date) {
        today -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.ui_sheet_today) + ", "
        today.plusDays(1) -> androidx.compose.ui.res.stringResource(io.codepassion.doubletriangle.core.designsystem.R.string.tomorrow) + ", "
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
