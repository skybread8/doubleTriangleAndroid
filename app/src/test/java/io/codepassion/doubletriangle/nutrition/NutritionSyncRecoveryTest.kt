package io.codepassion.doubletriangle.nutrition

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionSyncRecoveryTest {
    @Test fun missingFoodDoesNotBecomeADeletionWithoutAnExplicitTimestamp() {
        assertFalse(shouldSendNutritionTombstone("nutrition-log-entries", null))
        assertFalse(shouldSendNutritionTombstone("nutrition-log-items", "invalid"))
        assertTrue(shouldSendNutritionTombstone("nutrition-log-entries", "2026-11-24T10:00:00Z"))
        assertTrue(shouldSendNutritionTombstone("other-resource", null))
    }
    @Test fun importedRowsNeverClaimTheIdentityOfADifferentServerRow() {
        val userId = "account"
        val original = "41265b13-6c6d-4720-b107-d362f97e524f"
        val originalLocal = stableRemoteLogId(original)
        // Older Android versions derived a second server UUID from this local ID.
        val generated = nutritionEntryId(userId, originalLocal)
        val mapping = resolveRemoteNutritionIdentities(userId, mapOf(originalLocal to "sync"), mapOf(originalLocal to generated), listOf(original, generated))
        assertEquals(originalLocal, mapping[generated])
        assertTrue(mapping.getValue(original) != mapping.getValue(generated))
        assertEquals(mapping, resolveRemoteNutritionIdentities(userId, mapOf(originalLocal to "sync"), mapOf(originalLocal to generated), listOf(generated, original)))
    }

    @Test fun locallyCreatedRowKeepsItsOutboundIdentityAndRemoteCollisionDoesNotOverwriteIt() {
        val userId = "account"
        val remote = "41265b13-6c6d-4720-b107-d362f97e524f"
        val localId = stableRemoteLogId(remote)
        val outbound = nutritionEntryId(userId, localId)
        val mapping = resolveRemoteNutritionIdentities(userId, mapOf(localId to "manual"), emptyMap(), listOf(remote, outbound))
        assertEquals(localId, mapping[outbound])
        assertTrue(mapping.getValue(remote) != localId)
    }

    @Test fun persistedRemoteIdentitySurvivesARepeatedPull() {
        val remote = "41265b13-6c6d-4720-b107-d362f97e524f"
        val first = resolveRemoteNutritionIdentities("account", emptyMap(), emptyMap(), listOf(remote))
        val local = first.getValue(remote)
        val second = resolveRemoteNutritionIdentities("account", mapOf(local to "sync"), mapOf(local to remote), listOf(remote.uppercase()))
        assertEquals(local, second[remote])
    }
    @Test fun deletingParentPreservesItsFoodRowsOnly() {
        org.junit.Assert.assertEquals(setOf("food-a"), itemIdsForDeletedNutritionEntries(
            mapOf("food-a" to "ENTRY-A", "food-b" to "entry-b"), setOf("entry-a"),
        ))
    }

    @Test fun reconstructsTheSameRemoteIdentityAfterMappingIsLost() {
        val id = nutritionEntryId("user-1", 12345L)
        assertEquals(12345L, localNutritionLogId(id.uppercase(), mapOf(id to 12345L)))
        assertEquals(id, nutritionEntryId("user-1", 12345L))
        assertTrue(id != nutritionEntryId("user-2", 12345L))
    }

    @Test fun uploadsMediaAndEntryBeforeNutritionChildrenOrImages() {
        val parents = setOf("nutrition-log-media", "nutrition-log-entries")
        assertTrue(nutritionPushDependencies(setOf("nutrition-log-items")).containsAll(parents))
        assertTrue(nutritionPushDependencies(setOf("nutrition-log-media")).containsAll(parents))
        assertFalse("nutrition-log-entries" in nutritionPushDependencies(setOf("users")))
    }
    @Test fun downloadedAndNotYetDownloadedPhotosRetainTheirServerIdentity() {
        assertTrue(shouldReuseRemoteNutritionMedia(true, true, true, false, true))
        assertTrue(shouldReuseRemoteNutritionMedia(true, false, false, false, true))
        assertTrue(shouldReuseRemoteNutritionMedia(true, false, false, true, true))
        assertFalse(shouldReuseRemoteNutritionMedia(true, true, false, false, true))
        assertTrue(shouldReuseRemoteNutritionMedia(true, true, false, false, true, isUploadedPhoto = true))
        assertFalse(shouldReuseRemoteNutritionMedia(false, true, true, false, true))
    }
    @Test fun mediaRemainsPendingUntilEveryImageHasBeenProcessed() {
        assertTrue(canAcknowledgeNutritionMedia(false, false))
        assertFalse(canAcknowledgeNutritionMedia(true, false))
        assertFalse(canAcknowledgeNutritionMedia(false, true))
    }
    @Test fun existingNutritionItemCannotBeMovedToAnotherEntry() {
        assertTrue(nutritionItemBelongsToParent("PARENT-A", "parent-a"))
        assertTrue(nutritionItemBelongsToParent(null, "parent-a"))
        assertFalse(nutritionItemBelongsToParent("parent-b", "parent-a"))
    }
}
