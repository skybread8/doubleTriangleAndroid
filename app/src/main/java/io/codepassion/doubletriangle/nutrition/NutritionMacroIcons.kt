package io.codepassion.doubletriangle.nutrition

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.material.Icon
import io.codepassion.wildforce.android.R

/** Platform-neutral replicas of iOS's fish, leaf, drop and flame SF Symbols. */
internal enum class NutritionMacroIcon(@DrawableRes val resourceId: Int) {
    Calories(R.drawable.ic_macro_calories),
    Protein(R.drawable.ic_macro_protein),
    Carbs(R.drawable.ic_macro_carbs),
    Fat(R.drawable.ic_macro_fat),
}

@Composable
internal fun NutritionMacroSymbol(icon: NutritionMacroIcon, tint: Color, modifier: Modifier = Modifier) {
    Icon(painterResource(icon.resourceId), contentDescription = null, tint = tint, modifier = modifier)
}
