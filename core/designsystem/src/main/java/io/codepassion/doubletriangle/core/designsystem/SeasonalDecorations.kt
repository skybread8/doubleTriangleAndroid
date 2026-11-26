package io.codepassion.doubletriangle.core.designsystem

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDate

/** Local presentation preference, deliberately independent of account synchronization. */
object SeasonalDecorations {
    private const val FILE = "wildforce_seasonal_decorations"
    private const val ENABLED = "enabled"
    fun enabled(context: Context): Boolean = context.getSharedPreferences(FILE, 0).getBoolean(ENABLED, true)
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, 0).edit().putBoolean(ENABLED, enabled).apply()
    }
    fun isChristmas(date: LocalDate): Boolean =
        (date.monthValue == 12 && date.dayOfMonth >= 7) || (date.monthValue == 1 && date.dayOfMonth <= 6)
}

@Composable
fun rememberSeasonalDecorationsEnabled(): Boolean {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("wildforce_seasonal_decorations", 0) }
    var enabled by remember(prefs) { mutableStateOf(SeasonalDecorations.enabled(context)) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> enabled = SeasonalDecorations.enabled(context) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return enabled
}

@Composable
fun rememberChristmasActive(): Boolean {
    val enabled = rememberSeasonalDecorationsEnabled()
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) { today = LocalDate.now(); delay(30_000) }
    }
    // Keep the preview available in debug builds; release builds use the seasonal calendar.
    return enabled && (BuildConfig.DEBUG || SeasonalDecorations.isChristmas(today))
}

/** Decorative only: does not intercept touch or add accessibility nodes. */
@Composable
fun ChristmasSnowflakes(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val color = Color(0xFF598D91).copy(alpha = .65f)
        listOf(Offset(size.width * .18f, size.height * .3f), Offset(size.width * .72f, size.height * .65f)).forEachIndexed { i, center ->
            val r = size.minDimension * if (i == 0) .16f else .11f
            drawLine(color, center.copy(x = center.x - r), center.copy(x = center.x + r), strokeWidth = 1.5f)
            drawLine(color, center.copy(y = center.y - r), center.copy(y = center.y + r), strokeWidth = 1.5f)
            drawLine(color, Offset(center.x - r * .7f, center.y - r * .7f), Offset(center.x + r * .7f, center.y + r * .7f), strokeWidth = 1.5f)
            drawLine(color, Offset(center.x - r * .7f, center.y + r * .7f), Offset(center.x + r * .7f, center.y - r * .7f), strokeWidth = 1.5f)
        }
    }
}

@Composable
fun ChristmasHat(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val hat = Path().apply {
            moveTo(size.width * .12f, size.height * .73f)
            cubicTo(size.width * .22f, size.height * .48f, size.width * .28f, size.height * .09f, size.width * .53f, size.height * .10f)
            cubicTo(size.width * .75f, size.height * .10f, size.width * .82f, size.height * .24f, size.width * .88f, size.height * .43f)
            cubicTo(size.width * .79f, size.height * .35f, size.width * .72f, size.height * .34f, size.width * .70f, size.height * .43f)
            lineTo(size.width * .86f, size.height * .73f)
            close()
        }
        drawPath(hat, Color(0xFFB92635))
        val brim = Path().apply {
            moveTo(size.width * .09f, size.height * .72f)
            quadraticTo(size.width * .49f, size.height * .48f, size.width * .89f, size.height * .72f)
            quadraticTo(size.width * .93f, size.height * .76f, size.width * .90f, size.height * .85f)
            quadraticTo(size.width * .49f, size.height * .61f, size.width * .08f, size.height * .85f)
            quadraticTo(size.width * .05f, size.height * .76f, size.width * .09f, size.height * .72f)
            close()
        }
        drawPath(brim, Color(0xFFE6EEEB))
        drawCircle(Color(0xFFF4F8F6), radius = size.height * .11f, center = Offset(size.width * .89f, size.height * .45f))
    }
}

/** A static garland keeps the decoration visible without introducing motion. */
@Composable
fun ChristmasGarland(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val wire = Path().apply {
            moveTo(0f, size.height * .12f)
            for (i in 0 until 8) {
                val start = size.width * i / 8f
                val end = size.width * (i + 1) / 8f
                quadraticTo((start + end) / 2f, size.height * .85f, end, size.height * .12f)
            }
        }
        drawPath(wire, Color(0xFF326E59), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
        val colors = listOf(Color(0xFFC34448), Color(0xFFD6A143), Color(0xFF438A76), Color(0xFFC34448))
        for (i in 0 until 4) {
            val x = size.width * (i + .5f) / 4f
            drawLine(Color(0xFF779A85), Offset(x, size.height * .18f), Offset(x, size.height * .65f), 1.dp.toPx())
            drawCircle(colors[i], radius = 3.5.dp.toPx(), center = Offset(x, size.height * .72f))
            drawCircle(Color.White.copy(alpha = .7f), radius = 1.dp.toPx(), center = Offset(x - 1.dp.toPx(), size.height * .68f))
        }
        for (i in 0 until 16) {
            val x = size.width * (i + .5f) / 16f
            val y = if (i % 2 == 0) size.height * .42f else size.height * .56f
            drawCircle(colors[i % colors.size], radius = 2.8.dp.toPx(), center = Offset(x, y))
            drawCircle(Color.White.copy(alpha = .65f), radius = .8.dp.toPx(), center = Offset(x - 1.dp.toPx(), y - 1.dp.toPx()))
        }
    }
}

/** Snow is drawn behind content, without affecting touch targets or accessibility. */
@Composable
fun Modifier.christmasSnowfall(color: Color = Color(0xFF679EAE)): Modifier {
    // Respect Android's animator setting and avoid a distracting frozen pattern.
    if (!ValueAnimator.areAnimatorsEnabled()) return this
    val transition = rememberInfiniteTransition(label = "christmas-snow")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "snowfall",
    )
    return drawBehind {
        val phase = progress.value
        for (i in 0 until 96) {
            val seedX = ((i * 193 + 19) % 997) / 997f
            val seedY = ((i * 137 + 43) % 991) / 991f
            val speed = if (i % 4 == 0) 2f else 1f
            val y = ((seedY + phase * speed) % 1f) * size.height
            val sway = kotlin.math.sin((phase * 2f * Math.PI + i * .9f).toFloat()) * (if (i % 4 == 0) 7.dp else 4.dp).toPx()
            val x = seedX * size.width + sway
            val radius = (if (i % 11 == 0) 2.9f else if (i % 4 == 0) 2f else 1.25f).dp.toPx()
            val center = Offset(x, y)
            // A faint halo softens the flakes without making them look outlined.
            drawCircle(color.copy(alpha = .08f), radius * 1.5f, center)
            drawCircle(color.copy(alpha = if (i % 4 == 0) .38f else .26f), radius, center)
        }
    }
}

/** Snow and lights along the image edge; the artwork has no input semantics. */
@Composable
fun ChristmasWorkoutTrim(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val trimHeight = 30.dp.toPx().coerceAtMost(size.height)
        val snow = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, trimHeight * .16f)
            for (i in 12 downTo 0) {
                val x = size.width * i / 12f
                quadraticTo(x + size.width / 24f, trimHeight * (if (i % 2 == 0) .33f else .22f), x, trimHeight * .14f)
            }
            close()
        }
        drawPath(snow, Color(0xFFF1F9FA).copy(alpha = .94f))
        val wire = Path().apply {
            moveTo(size.width * .29f, trimHeight * .17f)
            quadraticTo(size.width * .50f, trimHeight * .65f, size.width * .71f, trimHeight * .17f)
        }
        drawPath(wire, Color(0xFFBFE1CF), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx()))
        val colors = listOf(Color(0xFFFF6866), Color(0xFFFFD079), Color(0xFFB9E7D4))
        for (i in 0 until 7) {
            val t = i / 6f
            val x = size.width * (.29f + .42f * t)
            val y = trimHeight * (.17f + .24f * (1f - (2f * t - 1f) * (2f * t - 1f)))
            drawCircle(colors[i % colors.size], radius = 2.2.dp.toPx(), center = Offset(x, y))
        }
        val flakeColor = Color.White.copy(alpha = .85f)
        listOf(Offset(size.width * .28f, trimHeight * .75f), Offset(size.width * .72f, trimHeight * .78f)).forEach { center ->
            val arm = 3.dp.toPx()
            drawLine(flakeColor, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), 1.dp.toPx())
            drawLine(flakeColor, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), 1.dp.toPx())
        }
    }
}
