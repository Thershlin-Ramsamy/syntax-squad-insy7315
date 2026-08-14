package com.mcc.construction.ui.screens.login

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcc.construction.ui.components.AppTextField
import com.mcc.construction.ui.components.DetailScaffold
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

@Composable
fun ForgotPasswordScreen(navController: NavHostController) {
    var email by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DetailScaffold(title = "Reset password", onBack = { navController.popBackStack() }) {
        ScreenPadding(modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 460.dp)) {
            if (sent) {
                // Confirmation deliberately does not say whether the address exists —
                // revealing that is an account-enumeration weakness even in a prototype.
                EmptyState(
                    icon = Icons.Filled.MarkEmailRead,
                    title = "Check your email",
                    message = "If an account exists for $email, a reset link is on its way. " +
                        "The link expires in 30 minutes.",
                    tone = Tone.Success,
                    actionLabel = "Back to sign in",
                    onAction = { navController.popBackStack() }
                )
            } else {
                Text(
                    text = "Forgot your password?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "Enter the email address linked to your MCC account and we'll " +
                        "send you a link to set a new password.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.xxl))
                AppTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    label = "Work email",
                    placeholder = "name@mccconstruction.co.za",
                    leadingIcon = Icons.Filled.Mail,
                    keyboardType = KeyboardType.Email,
                    isError = error != null,
                    supportingText = error
                )
                Spacer(Modifier.height(Spacing.xxl))
                PrimaryButton(
                    text = "Send reset link",
                    onClick = {
                        if (!email.contains("@") || !email.contains(".")) {
                            error = "Enter a valid email address."
                        } else {
                            sent = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
