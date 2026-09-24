package com.mcc.construction.ui.screens.login

import com.mcc.construction.data.fake.testContainer
import com.mcc.construction.ui.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val viewModel = ForgotPasswordViewModel(testContainer().authRepository)

    @Test
    fun emptyEmailIsRejected() {
        viewModel.onSubmit()

        assertEquals(LoginError.EMAIL_REQUIRED, viewModel.uiState.value.emailError)
        assertFalse(viewModel.uiState.value.isLinkSent)
    }

    @Test
    fun malformedEmailIsRejected() {
        viewModel.onEmailChange("not-an-email")
        viewModel.onSubmit()

        assertEquals(LoginError.EMAIL_INVALID, viewModel.uiState.value.emailError)
    }

    @Test
    fun anyWellFormedEmailGetsTheSameConfirmation() {
        viewModel.onEmailChange("nobody@example.com")
        viewModel.onSubmit()

        assertTrue(viewModel.uiState.value.isLinkSent)
    }

    @Test
    fun tryAnotherEmailReturnsToTheForm() {
        viewModel.onEmailChange("worker@mcc.co.za")
        viewModel.onSubmit()
        viewModel.onTryAnotherEmail()

        assertFalse(viewModel.uiState.value.isLinkSent)
        assertEquals("worker@mcc.co.za", viewModel.uiState.value.email)
    }
}
