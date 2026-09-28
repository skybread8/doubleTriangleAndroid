package io.codepassion.doubletriangle.feature.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingCustomFocusesTest {
    @Test
    fun gymEquipmentPresetsMatchIosDefaults() {
        assertEquals(Equipment.entries.toSet(), GymType.BigGym.defaultEquipment)
        assertEquals(
            setOf(
                Equipment.Bodyweight, Equipment.Dumbbells, Equipment.Kettlebells,
                Equipment.FlatBench, Equipment.AdjustableBench, Equipment.SquatRack,
                Equipment.OlympicBarbell, Equipment.EzBar, Equipment.PullUpBar,
            ),
            GymType.HomeGym.defaultEquipment,
        )
        assertEquals(setOf(Equipment.Bodyweight), GymType.BodyweightOnly.defaultEquipment)
        assertTrue(Equipment.DipStation in GymType.SmallGym.defaultEquipment)
        assertTrue(Equipment.ChestPressMachine in GymType.SmallGym.defaultEquipment)
    }

    @Test
    fun automaticSplitPrefillsTwoDaysWithUpperLower() {
        val result = normalizedCustomFocuses(
            current = emptyMap(),
            workoutDays = setOf(WorkoutWeekday.Monday, WorkoutWeekday.Wednesday),
            previousSplit = TrainingSplitPreference.Automatic,
        )

        assertEquals(WorkoutFocus.UpperBody, result[WorkoutWeekday.Monday])
        assertEquals(WorkoutFocus.LowerBody, result[WorkoutWeekday.Wednesday])
    }

    @Test
    fun previousSplitProvidesTheSameInitialPatternAsIos() {
        val result = normalizedCustomFocuses(
            current = emptyMap(),
            workoutDays = setOf(WorkoutWeekday.Monday, WorkoutWeekday.Wednesday, WorkoutWeekday.Friday),
            previousSplit = TrainingSplitPreference.FullBody,
        )

        assertEquals(setOf(WorkoutFocus.FullBody), result.values.toSet())
    }

    @Test
    fun unsupportedCustomFocusesAreRemovedButValidChoicesArePreserved() {
        val result = normalizedCustomFocuses(
            current = mapOf(
                WorkoutWeekday.Monday to WorkoutFocus.Mobility,
                WorkoutWeekday.Wednesday to WorkoutFocus.Core,
                WorkoutWeekday.Friday to WorkoutFocus.Recovery,
            ),
            workoutDays = setOf(WorkoutWeekday.Monday, WorkoutWeekday.Wednesday),
            previousSplit = TrainingSplitPreference.PushPullLegs,
        )

        assertEquals(WorkoutFocus.Push, result[WorkoutWeekday.Monday])
        assertEquals(WorkoutFocus.Core, result[WorkoutWeekday.Wednesday])
        assertFalse(WorkoutFocus.Mobility in result.values)
        assertFalse(WorkoutFocus.Recovery in result.values)
    }
}
