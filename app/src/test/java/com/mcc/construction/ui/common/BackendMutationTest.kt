package com.mcc.construction.ui.common

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class BackendMutationTest {
    @Test
    fun failureClearsLoadingWithoutAdvancingSuccess() = runTest {
        var loading = true
        var success = false
        var failures = 0

        runBackendMutation(
            onFailure = { failures++ },
            onFinally = { loading = false }
        ) {
            val transportFailed = true

            if (transportFailed) {
                throw IOException("Transport interrupted")
            }

            success = true
        }

        assertFalse(loading)
        assertFalse(success)
        assertEquals(1, failures)
    }

    @Test
    fun cancellationPropagatesAndStillClearsLoading() = runTest {
        var cleared = false
        var reported = false

        try {
            runBackendMutation(
                onFailure = { reported = true },
                onFinally = { cleared = true }
            ) {
                throw CancellationException("Screen closed")
            }

            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertTrue(cleared)
            assertFalse(reported)
        }
    }
}