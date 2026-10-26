package io.codepassion.doubletriangle.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.codepassion.doubletriangle.core.designsystem.WildforceThemeTokens

/**
 * Keeps the large active-workout scene lambda out of ActiveWorkoutScreen's
 * generated Compose method. The visual surface and its content are unchanged.
 */
@Composable
internal fun ActiveWorkoutSurface(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(WildforceThemeTokens.background),
    ) {
        content()
    }
}
