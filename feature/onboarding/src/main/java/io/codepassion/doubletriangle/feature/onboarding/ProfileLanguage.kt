package io.codepassion.doubletriangle.feature.onboarding

import android.content.res.Resources
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.codepassion.doubletriangle.core.designsystem.R

/** Profile labels for the language codes used by the shared iOS and server model. */
fun profileLanguageName(language: String): String = when (language.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')) {
    "en", "english" -> "English"
    "es", "español" -> "Español"
    "ca", "català" -> "Català"
    "fr", "français" -> "Français"
    "it", "italiano" -> "Italiano"
    "pt", "português" -> "Português"
    "de", "deutsch" -> "Deutsch"
    "zh", "中文（简体）" -> "中文（简体）"
    "nl", "nederlands" -> "Nederlands"
    "ja", "日本語" -> "日本語"
    else -> "English"
}

/** Match iOS: use the device language for onboarding, falling back to English. */
fun deviceLanguageName(): String = profileLanguageName(Resources.getSystem().configuration.locales[0]?.toLanguageTag().orEmpty())

@Composable
internal fun localizedLanguageName(language: String): String = stringResource(when (profileLanguageName(language)) {
    "Español" -> R.string.spanish
    "Català" -> R.string.catalan
    "Français" -> R.string.french
    "Italiano" -> R.string.italian
    "Português" -> R.string.portuguese
    "Deutsch" -> R.string.german
    "中文（简体）" -> R.string.chinese__simplified_
    "Nederlands" -> R.string.dutch
    "日本語" -> R.string.japanese
    else -> R.string.english
})
