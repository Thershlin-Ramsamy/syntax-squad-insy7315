package com.mcc.construction.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.ui.common.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    /** Only EMAIL_REQUIRED or EMAIL_INVALID — see [ForgotPasswordViewModel.onSubmit]. */
    val emailError: LoginError? = null,
    val isSubmitting: Boolean = false,
    val isLinkSent: Boolean = false
)

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    /**
     * Checks the address is shaped like an email, then asks for a reset link.
     * The result never says whether an account exists — that would let anyone
     * test which addresses are registered.
     */
    fun onSubmit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val email = current.email.trim()
        val error = when {
            email.isEmpty() -> LoginError.EMAIL_REQUIRED
            !isPlausibleEmail(email) -> LoginError.EMAIL_INVALID
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(emailError = error) }
            return
        }

        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            authRepository.requestPasswordReset(email)
            _uiState.update { it.copy(isSubmitting = false, isLinkSent = true) }
        }
    }

    /** Back to the form, for a mistyped address. */
    fun onTryAnotherEmail() {
        _uiState.update { it.copy(isLinkSent = false) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { ForgotPasswordViewModel(appContainer().authRepository) }
        }
    }
}
