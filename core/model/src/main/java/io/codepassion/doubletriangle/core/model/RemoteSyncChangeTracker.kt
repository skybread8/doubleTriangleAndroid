package io.codepassion.doubletriangle.core.model

import android.content.Context

/**
 * Shared per-resource outbound-change state.  It replaces the old single
 * `remote_sync_pending` flag so a save in one feature cannot overwrite a
 * complete graph imported by another device.
 */
object RemoteSyncChangeTracker {
    private const val preferencesName = "wildforce_sync_state"
    private const val dirtyResourcesKey = "dirty_resources"

    fun mark(context: Context, vararg resources: String) {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        preferences.edit().putStringSet(
            dirtyResourcesKey,
            preferences.getStringSet(dirtyResourcesKey, emptySet()).orEmpty() + resources.asIterable(),
        ).apply()
    }

    fun dirty(context: Context): Set<String> = context
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        .getStringSet(dirtyResourcesKey, emptySet())
        .orEmpty()
        .toSet()

    fun clear(context: Context, resource: String) {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        preferences.edit().putStringSet(dirtyResourcesKey, preferences.getStringSet(dirtyResourcesKey, emptySet()).orEmpty() - resource).apply()
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().remove(dirtyResourcesKey).apply()
    }
}
