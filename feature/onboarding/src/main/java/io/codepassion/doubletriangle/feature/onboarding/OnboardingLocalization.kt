package io.codepassion.doubletriangle.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Resolve presentation text without changing the values stored or sent to the API. */
@Composable
internal fun onboardingLabel(kind: String, storedValue: String, fallback: String, description: Boolean = false): String {
    val property = if (description) "description" else "englishName"
    val id = onboardingSheetResources["$kind:$storedValue:$property"]
    return if (id == null) fallback else stringResource(id)
}
