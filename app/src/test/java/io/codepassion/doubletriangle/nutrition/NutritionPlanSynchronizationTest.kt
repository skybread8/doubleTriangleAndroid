package io.codepassion.doubletriangle.nutrition

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NutritionPlanSynchronizationTest {
    @Test fun synchronizesBeforeGenerating() = runBlocking {
        val calls = mutableListOf<String>()
        val result = generateNutritionPlanAfterSync(true, { calls += "sync" }, { calls += "generate"; "plan" })
        assertEquals(listOf("sync", "generate"), calls)
        assertEquals("plan", result)
    }

    @Test fun syncFailurePreventsGeneration() {
        val calls = mutableListOf<String>()
        assertThrows(IllegalStateException::class.java) {
            runBlocking { generateNutritionPlanAfterSync(true, { calls += "sync"; error("failed") }, { calls += "generate" }) }
        }
        assertEquals(listOf("sync"), calls)
    }

    @Test fun missingProfilePreventsSyncAndGeneration() {
        val calls = mutableListOf<String>()
        assertThrows(IllegalStateException::class.java) {
            runBlocking { generateNutritionPlanAfterSync(false, { calls += "sync" }, { calls += "generate" }) }
        }
        assertEquals(emptyList<String>(), calls)
    }
}
