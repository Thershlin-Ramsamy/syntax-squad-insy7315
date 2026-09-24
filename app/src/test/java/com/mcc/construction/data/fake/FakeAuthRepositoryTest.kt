package com.mcc.construction.data.fake

import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.SignInResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAuthRepositoryTest {

    private val auth = testContainer().authRepository

    @Test
    fun workerDemoAccountSignsInAsWorker() = runTest {
        val result = auth.signIn("worker@mcc.co.za", "anything")

        assertTrue(result is SignInResult.Success)
        assertEquals(UserRole.WORKER, auth.session.value?.role)
    }

    @Test
    fun managerDemoAccountSignsInAsManager() = runTest {
        auth.signIn("manager@mcc.co.za", "anything")

        assertEquals(UserRole.MANAGER, auth.session.value?.role)
    }

    @Test
    fun emailMatchIgnoresCaseAndSurroundingSpaces() = runTest {
        val result = auth.signIn("  Worker@MCC.co.za ", "pw")

        assertTrue(result is SignInResult.Success)
    }

    @Test
    fun unknownEmailIsRejectedAndLeavesNoSession() = runTest {
        val result = auth.signIn("nobody@mcc.co.za", "pw")

        assertEquals(SignInResult.UnknownAccount, result)
        assertNull(auth.session.value)
    }

    @Test
    fun blankPasswordIsRejected() = runTest {
        val result = auth.signIn("worker@mcc.co.za", " ")

        assertEquals(SignInResult.InvalidPassword, result)
        assertNull(auth.session.value)
    }

    @Test
    fun signOutClearsTheSession() = runTest {
        auth.signIn("worker@mcc.co.za", "pw")
        auth.signOut()

        assertNull(auth.session.value)
    }
}
