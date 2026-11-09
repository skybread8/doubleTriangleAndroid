package io.codepassion.doubletriangle.feature.workout

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSearchTest {
    @Test fun searchesNamesAndAliasesIgnoringCaseAndAccents() {
        val names = listOf("EZ Bar Skull Crusher", "French Press", "Press francés")
        assertTrue(exerciseNamesMatch(names, "FRENCH"))
        assertTrue(exerciseNamesMatch(names, "frances"))
        assertTrue(exerciseNamesMatch(names, ""))
        assertFalse(exerciseNamesMatch(names, "squat"))
    }
}
