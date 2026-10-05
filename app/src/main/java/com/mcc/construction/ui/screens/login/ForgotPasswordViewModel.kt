package com.mcc.construction.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.ui.common.appContainer
import com.mcc.construction.ui.common.backendAttempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: LoginError? = null,
    val isSubmitting: Boolean = false,
    val isLinkSent: Boolean = false
)

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val mutableState =
        MutableStateFlow(ForgotPasswordUiState())

    val uiState: StateFlow<ForgotPasswordUiState> =
        mutableState.asStateFlow()

    fun onEmailChange(email: String) {
        mutableState.update {
            it.copy(
                email = email,
                emailError = null
            )
        }
    }

    fun onSubmit() {
        val current = mutableState.value

        if (current.isSubmitting) return

        val email = current.email.trim()

        val error = when {
            email.isEmpty() ->
                LoginError.EMAIL_REQUIRED

            !isPlausibleEmail(email) ->
                LoginError.EMAIL_INVALID

            else -> null
        }

        if (error != null) {
            mutableState.update {
                it.copy(emailError = error)
            }

            return
        }

        mutableState.update {
            it.copy(isSubmitting = true)
        }

        viewModelScope.launch {
            try {
                backendAttempt {
                    authRepository.requestPasswordReset(
                        email
                    )
                } ?: return@launch

                mutableState.update {
                    it.copy(isLinkSent = true)
                }
            } finally {
                mutableState.update {
                    it.copy(isSubmitting = false)
                }
            }
        }
    }

    fun onTryAnotherEmail() {
        mutableState.update {
            it.copy(isLinkSent = false)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ForgotPasswordViewModel(
                    appContainer().authRepository
                )
            }
        }
    }
}