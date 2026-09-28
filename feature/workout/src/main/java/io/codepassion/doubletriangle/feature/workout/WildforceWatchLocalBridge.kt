package io.codepassion.doubletriangle.feature.workout

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
        if (enabled(context)) context.startService(Intent(context, WildforceWatchLocalBridgeService::class.java).setAction(ACTION_START))
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
    /**
     * A workout screen is not guaranteed to be composed while Android has the
     * app in the background. Keep a command received in that window instead of
     * silently dropping it; it is consumed as soon as its workout is restored.
     */
    internal fun dispatch(context: Context, command: Command) = Handler(Looper.getMainLooper()).post {
        val listener = commandListener
        if (listener != null) listener(command) else WatchCommandStore.enqueue(context, command)
    }
}

/** Small FIFO persisted alongside the active workout, used only as a fallback
 * when the watch sends a command while the Compose workout is not alive. */
internal object WatchCommandStore {
    private const val PREFS = "wildforce_watch_commands"
    private const val QUEUE = "queue"

    fun enqueue(context: Context, command: WildforceWatchLocalBridge.Command) {
        synchronized(this) {
            val prefs = context.getSharedPreferences(PREFS, 0)
            val queue = runCatching { JSONArray(prefs.getString(QUEUE, "[]")) }.getOrDefault(JSONArray())
            queue.put(JSONObject().put("op", command.operation).put("reps", command.reps).put("weightKg", command.weightKg))
            prefs.edit().putString(QUEUE, queue.toString()).apply()
        }
    }

    fun drain(context: Context): List<WildforceWatchLocalBridge.Command> = synchronized(this) {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val queue = runCatching { JSONArray(prefs.getString(QUEUE, "[]")) }.getOrDefault(JSONArray())
        prefs.edit().remove(QUEUE).apply()
        buildList {
            for (index in 0 until queue.length()) {
                val item = queue.optJSONObject(index) ?: continue
                when (item.optString("op")) {
                    "set_reps" -> add(WildforceWatchLocalBridge.Command("set_reps", reps = item.optInt("reps")))
                    "set_weight" -> add(WildforceWatchLocalBridge.Command("set_weight", weightKg = item.optDouble("weightKg")))
                    "complete_set", "skip_rest", "add_rest" -> add(WildforceWatchLocalBridge.Command(item.getString("op")))
                }
            }
        }
    }
}

class WildforceWatchLocalBridgeService : Service() {
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var socket: ServerSocket? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
            request.startsWith("POST /v1/command") -> { parseCommand(body)?.let { WildforceWatchLocalBridge.dispatch(applicationContext, it) }; "{\"ok\":true}" }
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
}
