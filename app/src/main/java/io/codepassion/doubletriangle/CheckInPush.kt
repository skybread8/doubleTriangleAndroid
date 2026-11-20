package io.codepassion.doubletriangle

import android.content.Context
import android.content.Intent
import java.util.UUID

internal fun isCheckInPush(type: String?, id: String?): Boolean =
    type in setOf("check_in_available", "check_in_reminder", "check_in_reviewed") &&
        id != null && runCatching { UUID.fromString(id).toString().equals(id, ignoreCase = true) }.getOrDefault(false)

/** Records an explicit notification tap until the signed-in home screen can sync it. */
internal fun saveCheckInPush(context: Context, intent: Intent?): Boolean {
    val type = intent?.getStringExtra("type") ?: return false
    val id = intent.getStringExtra("check_in_id") ?: return false
    if (!isCheckInPush(type, id)) return false
    context.getSharedPreferences("wildforce_check_in_push", Context.MODE_PRIVATE).edit()
        .putString("id", id).putString("type", type).apply()
    intent.removeExtra("type"); intent.removeExtra("check_in_id")
    return true
}
