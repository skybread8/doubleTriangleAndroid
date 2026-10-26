package io.codepassion.doubletriangle.nutrition

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

/** Visual counterpart of iOS's `carrot.fill` nutrition symbol. */
@Composable
internal fun NutritionCarrotIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val leaves = Path().apply {
            moveTo(w * .50f, h * .34f)
            cubicTo(w * .39f, h * .28f, w * .31f, h * .16f, w * .33f, h * .08f)
            cubicTo(w * .42f, h * .10f, w * .48f, h * .17f, w * .50f, h * .24f)
            cubicTo(w * .52f, h * .16f, w * .59f, h * .09f, w * .68f, h * .08f)
            cubicTo(w * .69f, h * .18f, w * .61f, h * .29f, w * .50f, h * .34f)
            close()
        }
        drawPath(leaves, tint)

        val carrot = Path().apply {
            moveTo(w * .32f, h * .28f)
            cubicTo(w * .41f, h * .34f, w * .59f, h * .34f, w * .68f, h * .28f)
            cubicTo(w * .69f, h * .48f, w * .61f, h * .73f, w * .50f, h * .92f)
            cubicTo(w * .39f, h * .73f, w * .31f, h * .48f, w * .32f, h * .28f)
            close()
        }
        drawPath(carrot, tint)
    }
}
