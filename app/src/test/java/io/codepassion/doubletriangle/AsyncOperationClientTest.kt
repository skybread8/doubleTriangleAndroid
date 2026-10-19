package io.codepassion.doubletriangle

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AsyncOperationClientTest {
    @Test fun mapsEveryBackendAsyncOperationStatus() {
        assertEquals(AsyncOperationStatus.Queued, parseAsyncOperationStatus("queued"))
        assertEquals(AsyncOperationStatus.Processing, parseAsyncOperationStatus("processing"))
        assertEquals(AsyncOperationStatus.Completed, parseAsyncOperationStatus("completed"))
        assertEquals(AsyncOperationStatus.Failed, parseAsyncOperationStatus("failed"))
    }

    @Test fun rejectsUnknownBackendAsyncOperationStatus() {
        try {
            parseAsyncOperationStatus("cancelled")
            fail("Expected an unknown operation status to be rejected")
        } catch (_: IllegalArgumentException) {
            // The server contract intentionally has no cancelled status.
        }
    }
}
