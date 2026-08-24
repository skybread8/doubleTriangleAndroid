package io.codepassion.doubletriangle

import io.codepassion.doubletriangle.core.model.ExerciseSetStyle
import io.codepassion.doubletriangle.core.model.WorkoutBlockType
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutPlanGeneratorTest {
    @Test
    fun `maps every AI set style and keeps legacy plans compatible`() {
        val styles = mapOf(
            "warmup" to ExerciseSetStyle.Warmup,
            "straight" to ExerciseSetStyle.Straight,
            "topSetBackoff" to ExerciseSetStyle.TopSetBackoff,
            "top_set_backoff" to ExerciseSetStyle.TopSetBackoff,
            "ascendingPyramid" to ExerciseSetStyle.AscendingPyramid,
            "dropSet" to ExerciseSetStyle.DropSet,
            "restPause" to ExerciseSetStyle.RestPause,
            "intervals" to ExerciseSetStyle.Intervals,
            "tempo" to ExerciseSetStyle.Tempo,
            "" to ExerciseSetStyle.Straight,
            "unknown" to ExerciseSetStyle.Straight,
        )

        styles.forEach { (wireValue, expected) ->
            assertEquals(expected, WorkoutPlanGenerator.parseSetStyle(wireValue))
        }
    }

    @Test
    fun `maps iOS workout block wire values`() {
        assertEquals(WorkoutBlockType.Warmup, WorkoutPlanGenerator.parseBlockType("warmup"))
        assertEquals(WorkoutBlockType.Standard, WorkoutPlanGenerator.parseBlockType("standard"))
        assertEquals(WorkoutBlockType.Superset, WorkoutPlanGenerator.parseBlockType("SUPERSET"))
        assertEquals(WorkoutBlockType.Cooldown, WorkoutPlanGenerator.parseBlockType("cooldown"))
        assertEquals(WorkoutBlockType.Standard, WorkoutPlanGenerator.parseBlockType("unknown"))
    }
}
