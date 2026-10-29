package io.codepassion.doubletriangle.feature.onboarding

import java.net.URI
import java.util.Locale

/** Accepts a typed code or the /join/{code} link embedded in a gym QR. */
fun parseGymJoinCode(text: String): String? {
    val trimmed = text.trim()
    val uri = runCatching { URI(trimmed) }.getOrNull()
    val candidate = if (uri?.scheme != null) {
        val parts = uri.path?.split('/')?.filter { it.isNotEmpty() } ?: return null
        val joinIndex = parts.indexOfLast { it == "join" }
        if (joinIndex < 0) return null
        parts.getOrNull(joinIndex + 1) ?: return null
    } else trimmed
    val code = candidate.uppercase(Locale.ROOT).filter { it in 'A'..'Z' || it in '0'..'9' }
    return code.takeIf { it.isNotEmpty() && it.length <= 16 }
}
