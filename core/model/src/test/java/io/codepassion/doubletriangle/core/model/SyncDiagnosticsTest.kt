package io.codepassion.doubletriangle.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDiagnosticsTest {
    @Test fun errorDetailsKeepTheFailureButRemoveCredentials() {
        val result = SyncDiagnostics.errorDetail(IllegalStateException("timeout Bearer my-private-token email test@example.com password=secret"))
        assertTrue(result.contains("timeout"))
        assertFalse(result.contains("my-private-token"))
        assertFalse(result.contains("test@example.com"))
        assertFalse(result.contains("secret"))
    }
}
