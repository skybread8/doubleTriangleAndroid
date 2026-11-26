package io.codepassion.doubletriangle.feature.workout

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseProfileVersionTest {
    @Test fun remoteProfileWinsOnlyWhenNewerOrLocalTimeIsUnknown() {
        val older = "2026-11-01T10:00:00Z"
        val newer = "2026-11-01T11:00:00Z"
        assertTrue(exerciseProfileRemoteWins(older, newer))
        assertTrue(exerciseProfileRemoteWins(null, newer))
        assertFalse(exerciseProfileRemoteWins(newer, older))
        assertFalse(exerciseProfileRemoteWins(newer, newer))
        assertFalse(exerciseProfileRemoteWins(newer, null))
    }
}
