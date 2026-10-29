package io.codepassion.doubletriangle.feature.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseMediaKeyTest {
    @Test
    fun `legacy shorthand keys resolve to the iOS media keys`() {
        assertEquals("barbellBenchPress", canonicalExerciseMediaKey("benchPress"))
        assertEquals("barbellOverheadPress", canonicalExerciseMediaKey("overheadPress"))
        assertEquals("barbellBentOverRow", canonicalExerciseMediaKey("bentOverRow"))
        assertEquals("barbellRomanianDeadlift", canonicalExerciseMediaKey("romanianDeadlift"))
        assertEquals("barbellInclineBenchPress", canonicalExerciseMediaKey("inclineBenchPress"))
        assertEquals("dumbbellChestFly", canonicalExerciseMediaKey("chestFly"))
        assertEquals("lyingLegCurl", canonicalExerciseMediaKey("hamstringCurl"))
    }

    @Test
    fun `existing canonical and coach keys are preserved`() {
        assertEquals("barbellBenchPress", canonicalExerciseMediaKey("barbellBenchPress"))
        assertEquals("coach:123", canonicalExerciseMediaKey("coach:123"))
        assertNull(canonicalExerciseMediaKey("  "))
    }

    @Test
    fun `tutorial images use canonical exercise key and requested gender`() {
        assertEquals("https://khbsrnhvonggicfmhcpg.supabase.co/storage/v1/object/public/wildfit/tutorial/barbellBenchPress_male.png", exerciseTutorialImageUrl("benchPress", "male"))
        assertEquals("https://khbsrnhvonggicfmhcpg.supabase.co/storage/v1/object/public/wildfit/tutorial/barbellBenchPress_female.png", exerciseTutorialImageUrl("benchPress"))
        assertNull(exerciseTutorialImageUrl(null))
        assertNull(exerciseTutorialImageUrl("coach:123"))
    }
}
