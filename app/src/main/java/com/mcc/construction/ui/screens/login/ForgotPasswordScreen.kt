package com.mcc.construction.ui.screens.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.ui.components.AppTextField
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SecondaryButton
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

/** Forgot password wired to its ViewModel. */
@Composable
fun ForgotPasswordRoute(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel(factory = ForgotPasswordViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ForgotPasswordScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::onSubmit,
        onTryAnotherEmail = viewModel::onTryAnotherEmail,
        onBack = onBack
    )
}

@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordUiState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onTryAnotherEmail: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(
        modifier = modifier,
        title = stringResource(R.string.forgot_title),
        onBack = onBack,
        maxContentWidth = Spacing.formMaxWidth
    ) {
        if (state.isLinkSent) {
            // Deliberately does not say whether the address has an account.
            EmptyState(
                icon = Icons.Filled.MarkEmailRead,
                title = stringResource(R.string.forgot_sent_title),
                message = stringResource(R.string.forgot_sent_message, state.email.trim()),
                tone = Tone.Success
            )
            PrimaryButton(text = stringResource(R.string.forgot_back_to_sign_in), onClick = onBack)
            Spacer(Modifier.height(Spacing.md))
            SecondaryButton(
                text = stringResource(R.string.forgot_try_another),
                onClick = onTryAnotherEmail,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            Text(
                text = stringResource(R.string.forgot_heading),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.forgot_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xxl))
            AppTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = stringResource(R.string.login_email_label),
                placeholder = stringResource(R.string.login_email_placeholder),
                leadingIcon = Icons.Filled.Mail,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Send,
                onImeAction = onSubmit,
                isError = state.emailError != null,
                supportingText = state.emailError?.let { forgotErrorMessage(it) },
                enabled = !state.isSubmitting
            )
            Spacer(Modifier.height(Spacing.xxl))
            PrimaryButton(
                text = stringResource(R.string.forgot_submit),
                icon = Icons.AutoMirrored.Filled.Send,
                loading = state.isSubmitting,
                onClick = onSubmit
            )
        }
    }
}

@Composable
private fun forgotErrorMessage(error: LoginError): String = stringResource(
    if (error == LoginError.EMAIL_REQUIRED) R.string.error_email_required
    else R.string.error_email_invalid
)

@DevicePreviews
@Composable
private fun ForgotPasswordFormPreview() {
    PreviewSurface {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(email = "sipho.dlamini", emailError = LoginError.EMAIL_INVALID),
            onEmailChange = {},
            onSubmit = {},
            onTryAnotherEmail = {},
            onBack = {}
        )
    }
}

@DevicePreviews
@Composable
private fun ForgotPasswordSentPreview() {
    PreviewSurface {
        ForgotPasswordScreen(
            state = ForgotPasswordUiState(email = "worker@mcc.co.za", isLinkSent = true),
            onEmailChange = {},
            onSubmit = {},
            onTryAnotherEmail = {},
            onBack = {}
        )
    }
}
