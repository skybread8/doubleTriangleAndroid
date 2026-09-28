package io.codepassion.doubletriangle

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Keeps the language selected in the profile in sync with Android resources. */
object AppLocale {
    fun languageTag(language: String?): String = when (language) {
        "English" -> "en"
        "Català" -> "ca"
        "Français" -> "fr"
        "Italiano" -> "it"
        "Português" -> "pt"
        "Deutsch" -> "de"
        "中文（简体）" -> "zh-Hans"
        "Nederlands" -> "nl"
        "日本語" -> "ja"
        else -> "es"
    }

    fun apply(language: String?) {
        val locales = LocaleListCompat.forLanguageTags(languageTag(language))
        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
