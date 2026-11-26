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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import io.codepassion.doubletriangle.core.model.executionExercises
import io.codepassion.doubletriangle.core.designsystem.localizedBackgroundContext

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
        val restEndAtMillis: Long? = null,
        val pendingFeedback: Boolean = false,
        val selectedFeedback: String = "JUSTO",
    ) {
        fun toJson(): String = JSONObject()
            .put("active", true).put("workoutId", workoutId).put("workoutTitle", workoutTitle)
            .put("exerciseName", exerciseName).put("exerciseIndex", exerciseIndex).put("exerciseCount", exerciseCount)
            .put("setNumber", setNumber).put("targetSets", targetSets).put("reps", reps).put("weightKg", weightKg)
            .put("restRemaining", restRemaining ?: JSONObject.NULL).put("restInitialSeconds", restInitialSeconds)
            .put("restEndAtMillis", restEndAtMillis ?: JSONObject.NULL)
            .put("serverTimeMillis", System.currentTimeMillis())
            .put("pendingFeedback", pendingFeedback).put("selectedFeedback", selectedFeedback)
            .put("exercises", JSONArray(exercises))
            .put("heartRateZoneAge", heartRateZoneAge)
            .put("elapsedSeconds", elapsedSeconds).toString()
    }

    data class Command(
        val operation: String,
        val reps: Int? = null,
        val weightKg: Double? = null,
        val feedback: String? = null,
        val durationMinutes: Int? = null,
        val distanceKm: Double? = null,
        val motion: WatchMotionSnapshotData? = null,
    )
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
        val prefs = context.getSharedPreferences("wildforce_profile", 0)
        val today = Calendar.getInstance()
        val birthYear = prefs.getInt("birth_year", 1995)
        val birthMonth = prefs.getInt("birth_month", 1)
        val birthDay = prefs.getInt("birth_day", 1)
        val birthdayHasPassed = today.get(Calendar.MONTH) + 1 > birthMonth ||
            (today.get(Calendar.MONTH) + 1 == birthMonth && today.get(Calendar.DAY_OF_MONTH) >= birthDay)
        val age = (today.get(Calendar.YEAR) - birthYear - if (birthdayHasPassed) 0 else 1).coerceIn(10, 100)
        state = value.copy(heartRateZoneAge = age)
        start(context)
    }
    internal fun currentState(context: Context): State? {
        val published = state ?: return null
        if (context.getSharedPreferences("wildforce_notification_settings", 0).getBoolean("app_foreground", false)) return published
        val snapshot = WorkoutSessionStore.load(context, published.workoutId) ?: return null
        if (snapshot.showsSummary) return null
        val exercises = snapshot.editableBlocks?.flatMap { it.executionExercises() } ?: return published
        val exercise = exercises.getOrNull(snapshot.exerciseIndex) ?: return null
        val rest = snapshot.restRemaining?.takeIf { it > 0 }
        return published.copy(
            exerciseName = localizedExerciseName(localizedBackgroundContext(context), exercise.imageKey, exercise.name),
            exerciseIndex = snapshot.exerciseIndex, exerciseCount = exercises.size,
            setNumber = (snapshot.completedByExercise[snapshot.exerciseIndex] ?: 0) + 1,
            targetSets = exercise.sets + (snapshot.addedSetsByExercise[snapshot.exerciseIndex] ?: 0),
            reps = snapshot.reps, weightKg = snapshot.weightKg, restRemaining = rest,
            restInitialSeconds = snapshot.restInitialSeconds,
            restEndAtMillis = rest?.let { System.currentTimeMillis() + it * 1_000L },
            elapsedSeconds = snapshot.elapsedSeconds, pendingFeedback = snapshot.pendingFeedback,
            selectedFeedback = snapshot.selectedFeedback ?: "JUSTO",
        )
    }

    internal fun dispatch(context: Context, command: Command): Boolean {
        val done = CountDownLatch(1)
        var accepted = false
        val action = Runnable {
            try {
                val foreground = context.getSharedPreferences("wildforce_notification_settings", 0).getBoolean("app_foreground", false)
                val listener = commandListener
                if (foreground && listener != null) {
                    listener(command)
                    accepted = true
                } else {
                    val workoutId = state?.workoutId ?: WildforceWatchBridge.state?.workoutId
                    val snapshot = workoutId?.let { WorkoutSessionStore.load(context, it) }
                    val updated = snapshot?.let { BackgroundWatchActions.apply(it, command) }
                    if (workoutId != null && updated != null) {
                        WorkoutSessionStore.save(context, workoutId, updated)
                        WorkoutForegroundService.watchSessionChanged()
                        accepted = true
                    } else if (workoutId != null && command.operation in setOf("set_duration", "set_distance", "motion")) {
                        WatchCommandStore.enqueue(context, workoutId, command)
                        accepted = true
                    }
                }
            } finally { done.countDown() }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) action.run() else Handler(Looper.getMainLooper()).post(action)
        return done.await(3, TimeUnit.SECONDS) && accepted
    }
}

/** Small FIFO persisted alongside the active workout, used only as a fallback
 * when the watch sends a command while the Compose workout is not alive. */
internal object WatchCommandStore {
    private const val PREFS = "wildforce_watch_commands"
    private const val QUEUE = "queue"

    fun enqueue(context: Context, workoutId: String, command: WildforceWatchLocalBridge.Command) {
        synchronized(this) {
            val prefs = context.getSharedPreferences(PREFS, 0)
            val queue = runCatching { JSONArray(prefs.getString(QUEUE, "[]")) }.getOrDefault(JSONArray())
            val item = JSONObject().put("workoutId", workoutId).put("op", command.operation).put("reps", command.reps)
                .put("weightKg", command.weightKg).put("feedback", command.feedback)
                .put("durationMinutes", command.durationMinutes).put("distanceKm", command.distanceKm)
            command.motion?.let { motion ->
                item.put("motion", JSONObject().put("status", motion.statusCode)
                    .put("count", motion.autoDetectedRepCount).put("confidence", motion.confidencePermille)
                    .put("reps", JSONArray().also { reps -> motion.reps.forEach { rep ->
                        reps.put(JSONObject().put("duration", rep.durationMillis).put("range", rep.rangeOfMotionPermille).put("velocity", rep.velocityCode))
                    } }))
            }
            queue.put(item)
            prefs.edit().putString(QUEUE, queue.toString()).apply()
        }
    }

    fun drain(context: Context, workoutId: String): List<WildforceWatchLocalBridge.Command> = synchronized(this) {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val queue = runCatching { JSONArray(prefs.getString(QUEUE, "[]")) }.getOrDefault(JSONArray())
        prefs.edit().remove(QUEUE).apply()
        buildList {
            for (index in 0 until queue.length()) {
                val item = queue.optJSONObject(index) ?: continue
                if (item.optString("workoutId") != workoutId) continue
                val motion = item.optJSONObject("motion")?.let { data ->
                    val reps = data.optJSONArray("reps") ?: JSONArray()
                    WatchMotionSnapshotData(data.optInt("status"), data.optInt("count"),
                        if (data.has("confidence")) data.optInt("confidence") else null,
                        (0 until reps.length()).mapNotNull { i -> reps.optJSONObject(i)?.let { rep ->
                            WatchMotionRepData(rep.optInt("duration"), rep.optInt("range"), rep.optInt("velocity"))
                        } })
                }
                add(WildforceWatchLocalBridge.Command(
                    operation = item.optString("op"),
                    reps = if (item.has("reps")) item.optInt("reps") else null,
                    weightKg = if (item.has("weightKg")) item.optDouble("weightKg") else null,
                    feedback = item.optString("feedback").takeIf { item.has("feedback") },
                    durationMinutes = if (item.has("durationMinutes")) item.optInt("durationMinutes") else null,
                    distanceKm = if (item.has("distanceKm")) item.optDouble("distanceKm") else null,
                    motion = motion,
                ))
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
        client.soTimeout = 3_000
        val input = client.getInputStream().buffered()
        val header = java.io.ByteArrayOutputStream()
        var matched = 0
        val end = byteArrayOf(13, 10, 13, 10)
        while (matched < end.size && header.size() < 8192) {
            val value = input.read()
            if (value < 0) return
            header.write(value)
            matched = if (value == end[matched].toInt()) matched + 1 else if (value == 13) 1 else 0
        }
        if (matched != end.size) return
        val lines = header.toString(StandardCharsets.UTF_8.name()).split("\r\n")
        val request = lines.firstOrNull() ?: return
        val contentLength = lines.firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
            ?.substringAfter(':')?.trim()?.toIntOrNull() ?: 0
        if (contentLength !in 0..4096) return
        val bytes = ByteArray(contentLength)
        var offset = 0
        while (offset < bytes.size) {
            val count = input.read(bytes, offset, bytes.size - offset)
            if (count < 0) return
            offset += count
        }
        val body = String(bytes, StandardCharsets.UTF_8)
        val response = when {
            request.startsWith("GET /v1/state") -> WildforceWatchLocalBridge.currentState(applicationContext)?.toJson() ?: "{\"active\":false}"
            request.startsWith("POST /v1/command") -> { val accepted = parseCommand(body)?.let { WildforceWatchLocalBridge.dispatch(applicationContext, it) } == true; if (accepted) "{\"ok\":true}" else "{\"ok\":false}" }
            else -> "{\"error\":\"not_found\"}"
        }
        val responseBytes = response.toByteArray(StandardCharsets.UTF_8)
        client.getOutputStream().use { out ->
            out.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${responseBytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
            out.write(responseBytes)
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
            "set_feedback" -> WildforceWatchLocalBridge.Command("set_feedback", feedback = json.optString("feedback"))
            "complete_feedback" -> WildforceWatchLocalBridge.Command("complete_feedback")
            else -> null
        }
    }.getOrNull()

    override fun onDestroy() { runCatching { socket?.close() }; socket = null; executor.shutdownNow(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
