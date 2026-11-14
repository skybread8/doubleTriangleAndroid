package io.codepassion.doubletriangle.nutrition

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionSyncRecoveryTest {
    @Test fun reconstructsTheSameRemoteIdentityAfterMappingIsLost() {
        val id = nutritionEntryId("user-1", 12345L)
        assertEquals(12345L, localNutritionLogId(id.uppercase(), mapOf(id to 12345L)))
        assertEquals(id, nutritionEntryId("user-1", 12345L))
        assertTrue(id != nutritionEntryId("user-2", 12345L))
    }

    @Test fun identifiesOnlyMissingEntriesEvenWhenCaseDiffers() {
        assertEquals(setOf("missing"), missingNutritionLogIds(listOf("EXISTING", "MISSING", "deleted"), setOf("existing", "deleted")))
    }
    @Test fun recoversOnlyLogsAbsentFromServerIncludingTombstones() {
        assertTrue(hasUnsentNutritionLogs(listOf("existing", "missing"), setOf("existing")))
        assertFalse(hasUnsentNutritionLogs(listOf("EXISTING", "deleted"), setOf("existing", "deleted")))
        assertFalse(hasUnsentNutritionLogs(emptyList(), emptySet()))
    }

    @Test fun uploadsMediaAndEntryBeforeNutritionChildrenOrImages() {
        val parents = setOf("nutrition-log-media", "nutrition-log-entries")
        assertTrue(nutritionPushDependencies(setOf("nutrition-log-items")).containsAll(parents))
        assertTrue(nutritionPushDependencies(setOf("nutrition-log-media")).containsAll(parents))
        assertFalse("nutrition-log-entries" in nutritionPushDependencies(setOf("users")))
    }
}
