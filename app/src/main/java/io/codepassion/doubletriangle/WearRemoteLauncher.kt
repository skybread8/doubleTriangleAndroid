package io.codepassion.doubletriangle

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.Executors

/** Starts the companion Wear OS activity on every connected watch, when available. */
object WearRemoteLauncher {
    private val executor = Executors.newSingleThreadExecutor()

    /** Mirrors iOS WatchAvailability: the Wear entry point is only shown when
     * a compatible watch node is actually connected. */
    fun checkAvailability(context: Context, onResult: (Boolean) -> Unit) {
        runCatching {
            Wearable.getNodeClient(context.applicationContext).connectedNodes
                .addOnCompleteListener { task ->
                    onResult(task.isSuccessful && task.result.orEmpty().isNotEmpty())
                }
        }.onFailure {
            onResult(false)
        }
    }

    fun startWorkout(context: Context) {
        val motionEnabled = context.getSharedPreferences("wildforce_profile_details", 0)
            .getBoolean("wear_auto_tracking_enabled", false)
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(Uri.parse("wildforce://workout/start?motion=${if (motionEnabled) 1 else 0}"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        runCatching {
            RemoteActivityHelper(context.applicationContext, executor)
                .startRemoteActivity(intent)
                .addListener({
                    // RemoteActivityHelper reports delivery failures asynchronously.
                    // The phone workout remains the source of truth, so a missing watch
                    // must never prevent the local session from starting.
                }, executor)
        }.onFailure {
            Toast.makeText(context, "No se pudo abrir Wear OS", Toast.LENGTH_SHORT).show()
        }
    }
}
