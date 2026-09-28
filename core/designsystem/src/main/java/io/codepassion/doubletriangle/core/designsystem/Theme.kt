package io.codepassion.doubletriangle.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AccentLight = Color(0xFF373732)
private val PrimaryLight = Color(0xFF373732)
private val AccentDark = Color(0xFFFFFFFF)
private val OnAccentDark = Color(0xFF101010)
private val OnAccentLight = Color(0xFFFEFEFE)
// Workout uses SwiftUI's warm/orange emphasis, not iOS's AccentColor2 asset
// (that asset is reserved for Nutrition).  Keep the established Android orange
// here because this token is consumed by the training flow.
private val AccentGold = Color(0xFFF28A29)
private val AccentGoldDark = Color(0xFFC96818)
private val AccentGoldLightVariant = Color(0xFFE87E25)
private val AccentRed = Color(0xFFF44545)
private val WarmupAccent = Color(0xFFF08A24)
private val CooldownAccent = Color(0xFF4A8FE7)
private val DeviceBackgroundLight = Color(0xFFF1F2F6)
private val DeviceBackgroundDark = Color(0xFF151616)
private val BackgroundLight = Color(0xFFF7F6FB)
private val BackgroundDark = Color(0xFF151616)
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceDark = Color(0xFF232227)
private val TextLight = Color(0xFF373732)
private val TextDark = Color(0xFFFEFFFF)
private val SecondaryTextLight = Color(0xFF988B8B)
private val SecondaryTextDark = Color(0xFFA7A6AE)

val AntonFontFamily = FontFamily(Font(R.font.anton_regular))
val Exo2FontFamily = FontFamily(
    Font(R.font.exo2_variablefont_wght, weight = FontWeight.Normal),
    Font(R.font.exo2_variablefont_wght, weight = FontWeight.SemiBold),
    Font(R.font.exo2_variablefont_wght, weight = FontWeight.Bold),
)

private val WildforceTypography
    @Composable get() = MaterialTheme.typography.copy(
        h1 = TextStyle(fontFamily = AntonFontFamily, fontSize = 42.sp),
        h2 = TextStyle(fontFamily = AntonFontFamily, fontSize = 36.sp),
        h3 = TextStyle(fontFamily = Exo2FontFamily, fontSize = 30.sp, fontWeight = FontWeight.Bold),
        h4 = TextStyle(fontFamily = AntonFontFamily, fontSize = 30.sp),
        h5 = TextStyle(fontFamily = AntonFontFamily, fontSize = 22.sp),
        h6 = TextStyle(fontFamily = Exo2FontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold),
        subtitle1 = TextStyle(fontFamily = Exo2FontFamily, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        subtitle2 = TextStyle(fontFamily = Exo2FontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        body1 = TextStyle(fontFamily = Exo2FontFamily, fontSize = 16.sp),
        body2 = TextStyle(fontFamily = Exo2FontFamily, fontSize = 14.sp),
        button = TextStyle(fontFamily = Exo2FontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        caption = TextStyle(fontFamily = Exo2FontFamily, fontSize = 12.sp),
        overline = TextStyle(fontFamily = Exo2FontFamily, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    )

@Composable
fun WildforceTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColors(
            primary = AccentDark,
            primaryVariant = AccentGoldDark,
            secondary = AccentGold,
            background = BackgroundDark,
            surface = SurfaceDark,
            onPrimary = OnAccentDark,
            onBackground = TextDark,
            onSurface = TextDark,
            error = AccentRed,
        )
    } else {
        lightColors(
            primary = PrimaryLight,
            primaryVariant = AccentGoldLightVariant,
            secondary = AccentGold,
            background = BackgroundLight,
            surface = SurfaceLight,
            onPrimary = OnAccentLight,
            onBackground = TextLight,
            onSurface = TextLight,
            error = AccentRed,
        )
    }
    MaterialTheme(colors = colors, typography = WildforceTypography, content = content)
}

object WildforceThemeTokens {
    val background: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colors.background
    val backgroundSecondary: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colors.surface
    /** A raised surface for controls that sit above cards, sheets, or imagery. */
    val surfaceElevated: Color
        @Composable @ReadOnlyComposable get() = if (MaterialTheme.colors.isLight) Color.White else Color(0xFF302F35)
    /** Low-contrast fill for unselected chips and secondary controls. */
    val surfaceSubtle: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colors.onSurface.copy(alpha = if (MaterialTheme.colors.isLight) 0.08f else 0.12f)
    val textPrimary: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colors.onBackground
    val textSecondary: Color
        @Composable @ReadOnlyComposable get() = if (MaterialTheme.colors.isLight) SecondaryTextLight else SecondaryTextDark
    val accent: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colors.primary
    /** Warm workout emphasis. Do not map this to iOS AccentColor2 (Nutrition). */
    val accentGold: Color get() = AccentGold
    /** iOS AccentColor3, used for superset connectors and destructive emphasis. */
    val accentRed: Color get() = AccentRed
    val warmupAccent: Color get() = WarmupAccent
    val cooldownAccent: Color get() = CooldownAccent
    val primaryButtonText: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colors.onPrimary
    val primaryButtonShadow: Color
        @Composable @ReadOnlyComposable get() = if (MaterialTheme.colors.isLight) Color.Black else AccentLight
    val deviceBackground: Color
        @Composable @ReadOnlyComposable get() = if (MaterialTheme.colors.isLight) DeviceBackgroundLight else DeviceBackgroundDark
    /** Adaptive circular control used over artwork and image headers. */
    val imageControlBackground: Color
        @Composable @ReadOnlyComposable get() = if (MaterialTheme.colors.isLight) Color.White.copy(alpha = 0.88f) else Color(0xFF1D1D21).copy(alpha = 0.92f)
    val imageControlContent: Color
        @Composable @ReadOnlyComposable get() = if (MaterialTheme.colors.isLight) Color(0xFF151515) else TextDark
}
