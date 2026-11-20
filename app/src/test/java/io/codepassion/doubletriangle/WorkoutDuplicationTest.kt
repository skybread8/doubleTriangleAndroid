package io.codepassion.doubletriangle

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WorkoutDuplicationTest {
    @Test fun duplicatePrecedesGraphPull() = runBlocking {
        val calls = mutableListOf<String>()
        val result = duplicateThenPull("key", { calls += "post:$it"; "new-id" }, { calls += "pull:$it"; "copy" })
        assertEquals("copy", result)
        assertEquals(listOf("post:key", "pull:new-id"), calls)
    }

    @Test fun failedPullCanRetryWithSameKey() {
        val keys = mutableListOf<String>()
        assertThrows(IllegalStateException::class.java) {
            runBlocking { duplicateThenPull("key", { keys += it; "new-id" }, { error("pull failed") }) }
        }
        runBlocking { duplicateThenPull("key", { keys += it; "new-id" }, { it }) }
        assertEquals(listOf("key", "key"), keys)
    }
}
