package com.mcc.construction.ui.screens.login

import com.mcc.construction.data.fake.testContainer
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val container = testContainer()
    private val viewModel = LoginViewModel(container.authRepository, container.demoAccounts)

    @Test
    fun emptyEmailShowsRequiredError() {
        viewModel.onPasswordChange("pw")
        viewModel.onSubmit()

        assertEquals(LoginError.EMAIL_REQUIRED, viewModel.uiState.value.emailError)
        assertNull(viewModel.uiState.value.signedInAs)
    }

    @Test
    fun malformedEmailShowsInvalidError() {
        viewModel.onEmailChange("worker.mcc.co.za")
        viewModel.onPasswordChange("pw")
        viewModel.onSubmit()

        assertEquals(LoginError.EMAIL_INVALID, viewModel.uiState.value.emailError)
    }

    @Test
    fun emptyPasswordShowsRequiredError() {
        viewModel.onEmailChange("worker@mcc.co.za")
        viewModel.onSubmit()

        assertEquals(LoginError.PASSWORD_REQUIRED, viewModel.uiState.value.passwordError)
    }

    @Test
    fun unknownEmailShowsUnknownAccountError() {
        viewModel.onEmailChange("someone@mcc.co.za")
        viewModel.onPasswordChange("pw")
        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(LoginError.UNKNOWN_ACCOUNT, state.emailError)
        assertFalse(state.isSubmitting)
        assertNull(state.signedInAs)
    }

    @Test
    fun workerSignInRoutesToWorker() {
        viewModel.onEmailChange("worker@mcc.co.za")
        viewModel.onPasswordChange("anything")
        viewModel.onSubmit()

        assertEquals(UserRole.WORKER, viewModel.uiState.value.signedInAs)
    }

    @Test
    fun managerSignInRoutesToManager() {
        viewModel.onEmailChange("manager@mcc.co.za")
        viewModel.onPasswordChange("anything")
        viewModel.onSubmit()

        assertEquals(UserRole.MANAGER, viewModel.uiState.value.signedInAs)
    }

    @Test
    fun editingAFieldClearsItsError() {
        viewModel.onSubmit()
        viewModel.onEmailChange("w")

        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun demoAccountFillsInEverythingNeededToSignIn() {
        val manager = container.demoAccounts.single { it.role == UserRole.MANAGER }

        viewModel.onDemoAccountSelected(manager)
        viewModel.onSubmit()

        assertEquals(UserRole.MANAGER, viewModel.uiState.value.signedInAs)
    }

    @Test
    fun onNavigatedConsumesTheSignal() {
        viewModel.onEmailChange("worker@mcc.co.za")
        viewModel.onPasswordChange("pw")
        viewModel.onSubmit()
        viewModel.onNavigated()

        assertNull(viewModel.uiState.value.signedInAs)
    }
}
