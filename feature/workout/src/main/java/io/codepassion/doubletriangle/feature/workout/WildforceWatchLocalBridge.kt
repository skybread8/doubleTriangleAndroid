package io.codepassion.doubletriangle.feature.workout

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Calendar
import java.util.concurrent.Executors

/** Opt-in, phone-local bridge for the Zepp Side Service. It never binds to Wi-Fi/mobile data. */
object WildforceWatchLocalBridge {
    const val PORT = 39481
    private const val PREFS = "wildforce_watch"
    private const val ENABLED = "enabled"
    const val ACTION_START = "io.codepassion.doubletriangle.watch.local.START"

    data class State(
        val workoutId: String, val workoutTitle: String, val exerciseName: String,
        val exerciseIndex: Int, val exerciseCount: Int, val setNumber: Int, val targetSets: Int,
        val reps: Int, val weightKg: Double, val restRemaining: Int?, val restInitialSeconds: Int,
        val exercises: List<String>,
        val elapsedSeconds: Int,
        val heartRateZoneAge: Int = 30,
    ) {
        fun toJson(): String = JSONObject()
            .put("active", true).put("workoutId", workoutId).put("workoutTitle", workoutTitle)
            .put("exerciseName", exerciseName).put("exerciseIndex", exerciseIndex).put("exerciseCount", exerciseCount)
            .put("setNumber", setNumber).put("targetSets", targetSets).put("reps", reps).put("weightKg", weightKg)
            .put("restRemaining", restRemaining ?: JSONObject.NULL).put("restInitialSeconds", restInitialSeconds)
            .put("exercises", JSONArray(exercises))
            .put("heartRateZoneAge", heartRateZoneAge)
            .put("elapsedSeconds", elapsedSeconds).toString()
    }

    data class Command(val operation: String, val reps: Int? = null, val weightKg: Double? = null)
    @Volatile var state: State? = null
    @Volatile var commandListener: ((Command) -> Unit)? = null

    fun enabled(context: Context): Boolean = context.getSharedPreferences(PREFS, 0).getBoolean(ENABLED, false)
    fun setEnabled(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(ENABLED, value).apply()
        if (!value) stop(context) else if (state != null) start(context)
    }
    fun start(context: Context) {
        if (enabled(context)) ContextCompat.startForegroundService(context, Intent(context, WildforceWatchLocalBridgeService::class.java).setAction(ACTION_START))
    }
    fun stop(context: Context) {
        context.stopService(Intent(context, WildforceWatchLocalBridgeService::class.java))
        state = null
    }
    fun publish(context: Context, value: State) {
        val birthYear = context.getSharedPreferences("wildforce_profile", 0).getInt("birth_year", 1995)
        val age = (Calendar.getInstance().get(Calendar.YEAR) - birthYear).coerceIn(10, 100)
        state = value.copy(heartRateZoneAge = age)
        start(context)
    }
    internal fun dispatch(command: Command) = Handler(Looper.getMainLooper()).post { commandListener?.invoke(command) }
}

class WildforceWatchLocalBridgeService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var socket: ServerSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(4103, notification())
        if (socket == null) executor.execute(::serve)
        return START_NOT_STICKY
    }

    private fun serve() {
        runCatching {
            // Zepp Side Service uses 127.0.0.1, so bind explicitly to IPv4.
            ServerSocket(WildforceWatchLocalBridge.PORT, 8, java.net.InetAddress.getByName("127.0.0.1")).also { socket = it }.use { server ->
                while (!server.isClosed) runCatching { server.accept().use(::handle) }
            }
        }
    }

    private fun handle(client: Socket) {
        val reader = client.getInputStream().bufferedReader(StandardCharsets.UTF_8)
        val request = reader.readLine() ?: return
        var contentLength = 0
        while (true) {
            val line = reader.readLine() ?: return
            if (line.isEmpty()) break
            if (line.startsWith("Content-Length:", ignoreCase = true)) contentLength = line.substringAfter(':').trim().toIntOrNull() ?: 0
        }
        val body = if (contentLength > 0) CharArray(contentLength).also { reader.read(it) }.concatToString() else ""
        val response = when {
            request.startsWith("GET /v1/state") -> WildforceWatchLocalBridge.state?.toJson() ?: "{\"active\":false}"
            request.startsWith("POST /v1/command") -> { parseCommand(body)?.let(WildforceWatchLocalBridge::dispatch); "{\"ok\":true}" }
            else -> "{\"error\":\"not_found\"}"
        }
        val bytes = response.toByteArray(StandardCharsets.UTF_8)
        client.getOutputStream().use { out ->
            out.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
            out.write(bytes)
        }
    }

    private fun parseCommand(body: String): WildforceWatchLocalBridge.Command? = runCatching {
        val json = JSONObject(body)
        when (json.optString("op")) {
            "set_reps" -> WildforceWatchLocalBridge.Command("set_reps", reps = json.optInt("reps"))
            "set_weight" -> WildforceWatchLocalBridge.Command("set_weight", weightKg = json.optDouble("weightKg"))
            "complete_set" -> WildforceWatchLocalBridge.Command("complete_set")
            "skip_rest" -> WildforceWatchLocalBridge.Command("skip_rest")
            "add_rest" -> WildforceWatchLocalBridge.Command("add_rest")
            else -> null
        }
    }.getOrNull()

    override fun onDestroy() { runCatching { socket?.close() }; socket = null; executor.shutdownNow(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun notification() : android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("wildforce_watch_local", "Wildforce Watch", NotificationManager.IMPORTANCE_LOW))
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: Intent()
        return NotificationCompat.Builder(this, "wildforce_watch_local")
            .setSmallIcon(R.drawable.ic_wildforce_notification).setContentTitle("Wildforce Watch activo")
            .setContentText("Sincronización local durante el entrenamiento")
            .setContentIntent(PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).setOngoing(true).build()
    }
}
