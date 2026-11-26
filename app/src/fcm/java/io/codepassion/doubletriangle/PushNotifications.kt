package io.codepassion.doubletriangle

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.codepassion.doubletriangle.core.model.WildforceApiEnvironment
import io.codepassion.wildforce.android.BuildConfig
import io.codepassion.wildforce.android.R
import io.codepassion.doubletriangle.core.designsystem.localizedBackgroundContext
import io.codepassion.doubletriangle.feature.workout.WorkoutNotificationPreferences
import io.codepassion.doubletriangle.core.designsystem.R as SharedR
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

/** Registers FCM's installation token for the signed-in account, including token rotation. */
internal object CheckInPushTransport {
    private val executor = Executors.newSingleThreadExecutor()
    private var started = false
    private var lastAccountToken: String? = null
    private var accountListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var settingsListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    @Synchronized fun start(context: Context) {
        val app = context.applicationContext
        if (started) { refresh(app); return }
        started = true
        val account = app.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE)
        lastAccountToken = account.getString("token", null)
        accountListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == "token" || key == "id") {
                val next = account.getString("token", null)
                val previous = lastAccountToken
                lastAccountToken = next
                if (previous != null && next != previous) executor.execute { unregister(app, previous) }
                if (next != null) refresh(app)
            }
        }
        account.registerOnSharedPreferenceChangeListener(accountListener)
        val settings = app.getSharedPreferences("wildforce_notification_settings", Context.MODE_PRIVATE)
        settingsListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == "enabled") refresh(app)
        }
        settings.registerOnSharedPreferenceChangeListener(settingsListener)
        refresh(app)
    }

    fun refresh(context: Context) {
        if (!pushAllowed(context)) {
            val auth = context.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("token", null)
            if (auth != null) executor.execute { unregister(context.applicationContext, auth) }
            return
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> onToken(context, token) }
            .addOnFailureListener { Log.w("PushNotifications", "Unable to obtain an FCM token", it) }
    }

    fun onToken(context: Context, token: String) {
        val app = context.applicationContext
        app.getSharedPreferences("wildforce_push", Context.MODE_PRIVATE).edit().putString("fcm_token", token).apply()
        executor.execute {
            val auth = app.getSharedPreferences("wildforce_account", Context.MODE_PRIVATE).getString("token", null) ?: return@execute
            if (!pushAllowed(app)) { unregister(app, auth); return@execute }
            val installation = installationId(app)
            runCatching {
                val body = JSONObject().put("platform", "android").put("environment", "production")
                    .put("token", token).put("app_version", BuildConfig.VERSION_NAME)
                request("PUT", "account/push-devices/$installation", auth, body)
            }.onFailure { Log.w("PushNotifications", "Unable to register this device", it) }
        }
    }

    private fun pushAllowed(context: Context): Boolean =
        WorkoutNotificationPreferences.enabled(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun unregister(context: Context, auth: String) {
        runCatching { request("DELETE", "account/push-devices/${installationId(context)}", auth, null) }
            .onFailure { Log.w("PushNotifications", "Unable to unregister this device", it) }
    }

    @Synchronized private fun installationId(context: Context): String {
        // Installation identity must not be copied to another device by backup.
        val file = java.io.File(context.noBackupFilesDir, "push_installation_id")
        val existing = runCatching { file.readText().trim() }.getOrNull()
        if (existing != null && runCatching { UUID.fromString(existing) }.isSuccess) return existing
        return UUID.randomUUID().toString().also { file.writeText(it) }
    }

    private fun request(method: String, path: String, auth: String, body: JSONObject?) {
        val connection = URL(WildforceApiEnvironment.apiUrl(path)).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000; connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $auth")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            }
            check(connection.responseCode in 200..299 || method == "DELETE" && connection.responseCode == 404) { "Device registration failed (${connection.responseCode})" }
        } finally { connection.disconnect() }
    }
}

class CheckInMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) { CheckInPushTransport.onToken(applicationContext, token) }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!WorkoutNotificationPreferences.enabled(this) || !NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        val type = message.data["type"] ?: return
        val id = message.data["check_in_id"] ?: return
        if (!isCheckInPush(type, id)) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val strings = localizedBackgroundContext(this)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("check_ins", strings.getString(SharedR.string.check_in), NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("type", type).putExtra("check_in_id", id)
        val pending = PendingIntent.getActivity(this, id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, "check_ins")
            .setSmallIcon(R.drawable.ic_wildforce_notification)
            .setContentTitle(message.notification?.title ?: strings.getString(SharedR.string.check_in))
            .setContentText(message.notification?.body ?: strings.getString(SharedR.string.check_in))
            .setContentIntent(pending).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(this).notify(id.hashCode(), notification) }
        catch (_: SecurityException) { Log.i("PushNotifications", "Notification permission not granted") }
    }
}
