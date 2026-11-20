package io.codepassion.doubletriangle.core.designsystem

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Resolves the profile language for services and receivers, including before the activity starts. */
fun localizedBackgroundContext(context: Context): Context {
    val language = context.getSharedPreferences("wildforce_profile", Context.MODE_PRIVATE)
        .getString("app_language", null)?.trim() ?: return context
    val tag = backgroundLanguageTag(language)
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(tag))
    return context.createConfigurationContext(configuration)
}

fun backgroundLanguageTag(language: String): String = when (language.trim()) {
        "English" -> "en"
        "Español" -> "es"
        "Català" -> "ca"
        "Français" -> "fr"
        "Italiano" -> "it"
        "Português" -> "pt"
        "Deutsch" -> "de"
        "中文（简体）" -> "zh-Hans"
        "Nederlands" -> "nl"
        "日本語" -> "ja"
        else -> language.trim().replace('_', '-')
    }
