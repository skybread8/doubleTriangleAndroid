package io.codepassion.doubletriangle.nutrition

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class OptionalHealthDataTest {
    @Test
    fun `provider failure falls back while successful data is retained`() = runBlocking {
        val failure = IllegalArgumentException("startTime must be before endTime")
        var reported: Exception? = null
        val result = optionalHealthData<Int>(onFailure = { reported = it }) { throw failure }
        assertNull(result)
        assertSame(failure, reported)
        assertEquals(42, optionalHealthData(onFailure = { fail("unexpected failure") }) { 42 })
    }

    @Test
    fun `cancellation propagates instead of starting plan generation`() = runBlocking {
        val cancellation = CancellationException("cancelled")
        try {
            optionalHealthData<Int>(onFailure = { fail("cancellation must not be handled") }) { throw cancellation }
            fail("expected cancellation")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}
