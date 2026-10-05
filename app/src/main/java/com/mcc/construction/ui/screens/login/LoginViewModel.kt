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
import com.mcc.construction.ui.common.backendAttempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginError {
    EMAIL_REQUIRED,
    EMAIL_INVALID,
    UNKNOWN_ACCOUNT,
    ACCOUNT_SUSPENDED,
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
    val signedInAs: UserRole? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    demoAccounts: List<DemoAccount>
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        LoginUiState(
            demoAccounts = demoAccounts
        )
    )

    val uiState: StateFlow<LoginUiState> =
        mutableState.asStateFlow()

    fun onEmailChange(email: String) {
        mutableState.update {
            it.copy(
                email = email,
                emailError = null
            )
        }
    }

    fun onPasswordChange(password: String) {
        mutableState.update {
            it.copy(
                password = password,
                passwordError = null
            )
        }
    }

    fun onTogglePasswordVisibility() {
        mutableState.update {
            it.copy(
                isPasswordVisible =
                    !it.isPasswordVisible
            )
        }
    }

    fun onDemoAccountSelected(
        account: DemoAccount
    ) {
        mutableState.update {
            it.copy(
                email = account.email,
                password = DEMO_PASSWORD,
                emailError = null,
                passwordError = null
            )
        }
    }

    fun onSubmit() {
        val current = mutableState.value

        if (current.isSubmitting) return

        val email = current.email.trim()

        val emailError = when {
            email.isEmpty() ->
                LoginError.EMAIL_REQUIRED

            !isPlausibleEmail(email) ->
                LoginError.EMAIL_INVALID

            else -> null
        }

        val passwordError = if (
            current.password.isBlank()
        ) {
            LoginError.PASSWORD_REQUIRED
        } else {
            null
        }

        if (
            emailError != null ||
            passwordError != null
        ) {
            mutableState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError
                )
            }

            return
        }

        mutableState.update {
            it.copy(
                isSubmitting = true,
                emailError = null,
                passwordError = null
            )
        }

        viewModelScope.launch {
            try {
                val result = backendAttempt {
                    authRepository.signIn(
                        email,
                        current.password
                    )
                } ?: return@launch

                mutableState.update {
                    when (result) {
                        is SignInResult.Success ->
                            it.copy(
                                signedInAs = result.session.role
                            )

                        SignInResult.UnknownAccount ->
                            it.copy(
                                emailError =
                                    LoginError.UNKNOWN_ACCOUNT
                            )

                        SignInResult.AccountSuspended ->
                            it.copy(
                                emailError =
                                    LoginError.ACCOUNT_SUSPENDED
                            )

                        SignInResult.InvalidPassword ->
                            it.copy(
                                passwordError =
                                    LoginError.PASSWORD_INCORRECT
                            )
                    }
                }
            } finally {
                mutableState.update {
                    it.copy(isSubmitting = false)
                }
            }
        }
    }

    fun onNavigated() {
        mutableState.update {
            it.copy(
                signedInAs = null,
                password = ""
            )
        }
    }

    companion object {
        private const val DEMO_PASSWORD = "demo"

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()

                LoginViewModel(
                    c.authRepository,
                    c.demoAccounts
                )
            }
        }
    }
}