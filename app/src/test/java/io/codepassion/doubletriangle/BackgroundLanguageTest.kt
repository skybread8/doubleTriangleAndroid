package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.core.designsystem.backgroundLanguageTag
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundLanguageTest {
    @Test
    fun profileLanguagesResolveForBackgroundNotifications() {
        val languages = mapOf(
            "English" to "en", "Español" to "es", "Català" to "ca", "Français" to "fr",
            "Italiano" to "it", "Português" to "pt", "Deutsch" to "de",
            "中文（简体）" to "zh-Hans", "Nederlands" to "nl", "日本語" to "ja",
            "zh_CN" to "zh-CN", "fr-CA" to "fr-CA",
        )
        languages.forEach { (profile, expected) -> assertEquals(profile, expected, backgroundLanguageTag(profile)) }
    }
}
