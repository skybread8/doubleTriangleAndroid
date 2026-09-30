package io.codepassion.doubletriangle.core.model

import java.net.URL

/**
 * The single source of truth for Wildforce service endpoints on Android.
 *
 * Override `WILDFORCE_API_BASE_URL` and `WILDFORCE_PUBLIC_MEDIA_BASE_URL` in
 * local.properties to target a local or staging backend without editing source.
 */
object WildforceApiEnvironment {
    val apiBaseUrl: String = BuildConfig.WILDFORCE_API_BASE_URL.trimEnd('/')
    val publicMediaBaseUrl: String = BuildConfig.WILDFORCE_PUBLIC_MEDIA_BASE_URL.trimEnd('/')

    fun apiUrl(path: String): String = "$apiBaseUrl/${path.trimStart('/')}"

    fun publicMediaUrl(path: String): URL = URL("$publicMediaBaseUrl/${path.trimStart('/')}")
}
