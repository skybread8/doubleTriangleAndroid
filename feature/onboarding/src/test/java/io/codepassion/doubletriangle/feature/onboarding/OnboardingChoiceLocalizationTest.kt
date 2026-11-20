package io.codepassion.doubletriangle.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test
import io.codepassion.doubletriangle.core.designsystem.R as SharedR

class OnboardingChoiceLocalizationTest {
    @Test
    fun untranslatedSheetChoicesUseFallbackWithoutChangingOtherResources() {
        assertEquals(R.string.onboarding_choice_automatic, onboardingChoiceResource(SharedR.string.automatic))
        assertEquals(R.string.onboarding_choice_male, onboardingChoiceResource(SharedR.string.male))
        assertEquals(R.string.onboarding_detail_00, onboardingChoiceResource(SharedR.string.sedentary))
        assertEquals(R.string.onboarding_equipment_00, onboardingChoiceResource(SharedR.string.full_body))
        assertEquals(SharedR.string._d_min, onboardingChoiceResource(SharedR.string._d_min))
    }
}
