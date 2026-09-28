package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLocaleTest {
    @Test
    fun maps_every_profile_language_to_its_resource_locale() {
        assertEquals("es", AppLocale.languageTag("Español"))
        assertEquals("en", AppLocale.languageTag("English"))
        assertEquals("ca", AppLocale.languageTag("Català"))
        assertEquals("fr", AppLocale.languageTag("Français"))
        assertEquals("it", AppLocale.languageTag("Italiano"))
        assertEquals("pt", AppLocale.languageTag("Português"))
        assertEquals("de", AppLocale.languageTag("Deutsch"))
        assertEquals("zh-Hans", AppLocale.languageTag("中文（简体）"))
        assertEquals("nl", AppLocale.languageTag("Nederlands"))
        assertEquals("ja", AppLocale.languageTag("日本語"))
    }
}
