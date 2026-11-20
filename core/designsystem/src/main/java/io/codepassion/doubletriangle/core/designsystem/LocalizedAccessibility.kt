package io.codepassion.doubletriangle.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun Modifier.localizedContentDescription(@StringRes id: Int, vararg args: Any): Modifier {
    val description = if (args.isEmpty()) stringResource(id) else stringResource(id, *args)
    return semantics { contentDescription = description }
}
