package com.mcc.construction.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.SignInResult
import com.mcc.construction.di.DemoAccount
import com.mcc.construction.ui.common.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why a sign-in field was rejected. The screen turns each into a message. */
enum class LoginError {
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    UNKNOWN_ACCOUNT,
    PASSWORD_REQUIRED,
    PASSWORD_INCORRECT
}

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val emailError: LoginError? = null,
    val passwordError: LoginError? = null,
    val isSubmitting: Boolean = false,
    val demoAccounts: List<DemoAccount> = emptyList(),
    /** Set once sign-in succeeds; the screen navigates, then calls [LoginViewModel.onNavigated]. */
    val signedInAs: UserRole? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    demoAccounts: List<DemoAccount>
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState(demoAccounts = demoAccounts))
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    /**
     * Fills in a demo account in one tap, so the prototype can be shown without
     * typing. Any password is accepted by the fake repository.
     */
    fun onDemoAccountSelected(account: DemoAccount) {
        _uiState.update {
            it.copy(
                email = account.email,
                password = DEMO_PASSWORD,
                emailError = null,
                passwordError = null
            )
        }
    }

    fun onSubmit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val email = current.email.trim()
        val emailError = when {
            email.isEmpty() -> LoginError.EMAIL_REQUIRED
            !isPlausibleEmail(email) -> LoginError.EMAIL_INVALID
            else -> null
        }
        val passwordError = if (current.password.isBlank()) LoginError.PASSWORD_REQUIRED else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = authRepository.signIn(email, current.password)
            _uiState.update {
                when (result) {
                    is SignInResult.Success ->
                        it.copy(isSubmitting = false, signedInAs = result.session.role)

                    SignInResult.UnknownAccount ->
                        it.copy(isSubmitting = false, emailError = LoginError.UNKNOWN_ACCOUNT)

                    SignInResult.InvalidPassword ->
                        it.copy(isSubmitting = false, passwordError = LoginError.PASSWORD_INCORRECT)
                }
            }
        }
    }

    /** Clears the sign-in signal so returning to this screen does not navigate again. */
    fun onNavigated() {
        _uiState.update { it.copy(signedInAs = null, password = "") }
    }

    companion object {
        private const val DEMO_PASSWORD = "demo"

        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                LoginViewModel(container.authRepository, container.demoAccounts)
            }
        }
    }
}
