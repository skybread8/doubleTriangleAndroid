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

    @Synchronized
    fun mark(context: Context, vararg resources: String) {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val editor = preferences.edit().putStringSet(
            dirtyResourcesKey,
            preferences.getStringSet(dirtyResourcesKey, emptySet()).orEmpty() + resources.asIterable(),
        )
        resources.distinct().forEach { resource ->
            val key = "dirty_generation:$resource"
            editor.putLong(key, preferences.getLong(key, 0L) + 1L)
        }
        editor.apply()
    }

    @Synchronized
    fun dirty(context: Context): Set<String> = context
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        .getStringSet(dirtyResourcesKey, emptySet())
        .orEmpty()
        .toSet()

    @Synchronized
    fun generation(context: Context, resource: String): Long = context
        .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        .getLong("dirty_generation:$resource", 0L)

    @Synchronized
    fun clearIfUnchanged(context: Context, resource: String, generation: Long): Boolean {
        if (generation(context, resource) != generation) return false
        clear(context, resource)
        return true
    }

    @Synchronized
    fun clear(context: Context, resource: String) {
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        preferences.edit().putStringSet(dirtyResourcesKey, preferences.getStringSet(dirtyResourcesKey, emptySet()).orEmpty() - resource).apply()
    }

    @Synchronized
    fun clearAll(context: Context) {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().remove(dirtyResourcesKey).apply()
    }
}
