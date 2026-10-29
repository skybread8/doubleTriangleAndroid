package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutExperienceTest {
    @Test
    fun `completion rewards match ios session and milestone awards`() {
        assertEquals(25, workoutCompletionXpAward())
        assertEquals(75, workoutCompletionXpAward(completesPlan = true))
        assertEquals(175, workoutCompletionXpAward(completesPlan = true, completesMesocycle = true))
    }
}
