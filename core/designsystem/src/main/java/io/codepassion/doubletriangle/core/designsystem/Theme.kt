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
private val PrimaryLight = Color(0xFF343430)
private val AccentDark = Color(0xFFF3F4F4)
private val OnAccentDark = Color(0xFF101010)
private val OnAccentLight = Color(0xFFFEFEFE)
private val AccentGold = Color(0xFFF28A29)
private val AccentGoldDark = Color(0xFFC96818)
private val AccentGoldLightVariant = Color(0xFFE87E25)
private val AccentRed = Color(0xFFF54545)
private val BackgroundLight = Color(0xFFF9F7FF)
private val BackgroundDark = Color(0xFF151616)
private val SurfaceLight = Color(0xFFFDFDFD)
private val SurfaceDark = Color(0xFF231F22)
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
    val textPrimary: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colors.onBackground
    val textSecondary: Color
        @Composable @ReadOnlyComposable get() = if (MaterialTheme.colors.isLight) SecondaryTextLight else SecondaryTextDark
    val accent: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colors.primary
    val accentGold: Color get() = AccentGold
}
