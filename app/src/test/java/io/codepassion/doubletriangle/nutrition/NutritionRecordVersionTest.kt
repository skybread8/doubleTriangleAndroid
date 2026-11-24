package io.codepassion.doubletriangle.nutrition

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionRecordVersionTest {
    @Test fun eachFoodResolvesItsOwnConflict() {
        val t1 = Instant.parse("2026-01-01T10:00:00Z")
        val t2 = t1.plusSeconds(10)
        val localA = MealItem("Local A", 100.0, 1.0, 2.0, 3.0, remoteId = "a", updatedAt = t2)
        val localB = MealItem("Local B", 100.0, 1.0, 2.0, 3.0, remoteId = "b", updatedAt = t1)
        val remoteA = localA.copy(name = "Old A", updatedAt = t1)
        val remoteB = localB.copy(name = "New B", updatedAt = t2)
        val result = mergeNutritionItems(listOf(localA, localB), listOf(
            RemoteNutritionItem(0, remoteA, false), RemoteNutritionItem(1, remoteB, false),
        ), emptyMap())
        assertEquals(listOf("Local A", "New B"), result.items.map { it.name })
    }

    @Test fun localFoodDeletionWinsUntilARemoteVersionIsNewer() {
        val t1 = Instant.parse("2026-01-01T10:00:00Z")
        val t2 = t1.plusSeconds(10)
        val food = MealItem("A", 100.0, 1.0, 2.0, 3.0, remoteId = "a", updatedAt = t1)
        assertEquals(emptyList<MealItem>(), mergeNutritionItems(emptyList(), listOf(RemoteNutritionItem(0, food, false)), mapOf("a" to t2)).items)
        val restored = mergeNutritionItems(emptyList(), listOf(RemoteNutritionItem(0, food.copy(updatedAt = t2.plusSeconds(1)), false)), mapOf("a" to t2))
        assertEquals(listOf("A"), restored.items.map { it.name })
        assertEquals(setOf("a"), restored.resolvedDeletions)
    }

    @Test fun editingOneFoodDoesNotChangeTheParentOrSiblingVersion() {
        val t1 = Instant.parse("2026-01-01T10:00:00Z")
        val t2 = t1.plusSeconds(10)
        val first = MealItem("A", 100.0, 1.0, 2.0, 3.0, remoteId = "a", updatedAt = t1)
        val second = MealItem("B", 100.0, 1.0, 2.0, 3.0, remoteId = "b", updatedAt = t1)
        val meal = MealLog(1, "Meal", 200, 2, 4, 6, items = listOf(first, second), updatedAt = t1)
        val result = stampEditedMeals(listOf(meal), listOf(meal.copy(items = listOf(first.copy(name = "Edited"), second))), t2).single()
        assertEquals(t1, result.updatedAt)
        assertEquals(t2, result.items[0].updatedAt)
        assertEquals(t1, result.items[1].updatedAt)
    }

    @Test fun oldEntryOrEqualTombstoneCannotEraseNewerLocalMeal() {
        val local = Instant.parse("2026-01-02T10:00:00Z")
        val older = Instant.parse("2026-01-01T10:00:00Z")
        assertEquals(false, shouldApplyNutritionEntry(local, older, false))
        assertEquals(false, shouldApplyNutritionEntry(local, local, true))
        assertEquals(true, shouldApplyNutritionEntry(local, local, false))
        assertEquals(false, shouldApplyNutritionEntry(local, null, true))
        assertEquals(true, shouldApplyNutritionEntry(local, local.plusSeconds(1), true))
    }

    @Test fun savingDayOnlyChangesVersionOfEditedMeal() {
        val oldTime = Instant.parse("2026-01-01T10:00:00Z")
        val newTime = Instant.parse("2026-01-02T10:00:00Z")
        val a = MealLog(1, "A", 100, 1, 2, 3, updatedAt = oldTime)
        val b = MealLog(2, "B", 200, 2, 3, 4, updatedAt = oldTime)
        val result = stampEditedMeals(listOf(a, b), listOf(a, b.copy(name = "Edited")), newTime)
        assertEquals(oldTime, result[0].updatedAt)
        assertEquals(newTime, result[1].updatedAt)
    }

    @Test fun newMealReceivesItsOwnModificationTime() {
        val now = Instant.parse("2026-01-02T10:00:00Z")
        val meal = MealLog(1, "A", 100, 1, 2, 3)
        assertEquals(now, stampEditedMeals(emptyList(), listOf(meal), now).single().updatedAt)
    }
}
