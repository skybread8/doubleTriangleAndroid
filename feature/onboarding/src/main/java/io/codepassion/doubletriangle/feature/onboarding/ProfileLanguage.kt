package io.codepassion.doubletriangle.feature.onboarding

import android.content.res.Resources
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

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
    "Español" -> io.codepassion.doubletriangle.feature.onboarding.R.string.onboarding_language_spanish
    "Català" -> io.codepassion.doubletriangle.feature.onboarding.R.string.onboarding_language_catalan
    "Français" -> io.codepassion.doubletriangle.feature.onboarding.R.string.onboarding_language_french
    "Italiano" -> io.codepassion.doubletriangle.feature.onboarding.R.string.onboarding_language_italian
    "Português" -> io.codepassion.doubletriangle.feature.onboarding.R.string.onboarding_language_portuguese
    "Deutsch" -> io.codepassion.doubletriangle.feature.onboarding.R.string.onboarding_language_german
    "中文（简体）" -> io.codepassion.doubletriangle.feature.onboarding.R.string.onboarding_language_chinese__simplified_
    "Nederlands" -> io.codepassion.doubletriangle.feature.onboarding.R.string.onboarding_language_dutch
    "日本語" -> io.codepassion.doubletriangle.feature.onboarding.R.string.onboarding_language_japanese
    else -> io.codepassion.doubletriangle.feature.onboarding.R.string.onboarding_language_english
})
