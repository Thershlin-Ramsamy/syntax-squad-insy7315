package com.mcc.construction.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.di.DemoAccount
import com.mcc.construction.ui.components.AppTextField
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.icon
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.BrandWhite
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors

/** Sign-in wired to its ViewModel. */
@Composable
fun LoginRoute(
    onSignedIn: (UserRole) -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.signedInAs) {
        state.signedInAs?.let { role ->
            onSignedIn(role)
            viewModel.onNavigated()
        }
    }

    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onDemoAccountSelected = viewModel::onDemoAccountSelected,
        onSubmit = viewModel::onSubmit,
        onForgotPassword = onForgotPassword
    )
}

/**
 * Sign-in with a work email and password. The role comes back with the
 * account, so there is no role picker: a worker and a manager use the same
 * screen and land on different dashboards.
 *
 * While the app runs on fake data, a row of demo accounts fills in the details
 * in one tap. The list comes from the app container, so it disappears on its
 * own once a real backend is plugged in.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onDemoAccountSelected: (DemoAccount) -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
    ) {
        // A landscape desktop window is wide but short. Stacked vertically the
        // form runs past ~700dp, which pushes Sign in off the bottom — the
        // primary action should not need a scroll to find. Below that height
        // the brand block goes horizontal and the demo accounts sit side by
        // side, which brings the button back into view without changing the
        // layout on a phone.
        val shortWindow = maxHeight < 720.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    // A sign-in field stretched across 1200dp is unreadable and
                    // looks unfinished; widened a little on short windows so the
                    // three demo accounts fit on one row.
                    .widthIn(max = if (shortWindow) 620.dp else Spacing.formMaxWidth)
                    .padding(
                        horizontal = Spacing.screenH,
                        vertical = if (shortWindow) Spacing.xl else Spacing.xxxl
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandHeader(shortWindow)
                Spacer(Modifier.height(if (shortWindow) Spacing.xl else Spacing.xxxl))

                SignInForm(state, onEmailChange, onPasswordChange, onTogglePasswordVisibility, onSubmit)

                Spacer(Modifier.height(Spacing.lg))
                PrimaryButton(
                    text = stringResource(
                        if (state.isSubmitting) R.string.login_submitting else R.string.login_submit
                    ),
                    icon = Icons.AutoMirrored.Filled.Login,
                    loading = state.isSubmitting,
                    onClick = onSubmit
                )
                Spacer(Modifier.height(Spacing.sm))
                TextButton(
                    onClick = onForgotPassword,
                    modifier = Modifier.heightIn(min = Spacing.touchTarget)
                ) {
                    Text(
                        text = stringResource(R.string.login_forgot_password),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (state.demoAccounts.isNotEmpty()) {
                    Spacer(Modifier.height(if (shortWindow) Spacing.md else Spacing.xl))
                    DemoAccounts(
                        accounts = state.demoAccounts,
                        selectedEmail = state.email.trim(),
                        sideBySide = shortWindow,
                        onSelect = onDemoAccountSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(shortWindow: Boolean) {
    val logoWidth = if (shortWindow) 160.dp else 220.dp
    val mark = @Composable {
        // The official lockup is fixed navy-and-gold artwork on transparency, so
        // it always needs a light ground. Against the white light-theme surface
        // this plate is invisible; in dark mode it becomes the card that keeps
        // the logo legible.
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(BrandWhite)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            Image(
                painter = painterResource(R.drawable.mcc_logo_full_lockup),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(logoWidth)
            )
        }
    }
    val title = @Composable {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    val subtitle = @Composable {
        Text(
            text = stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (shortWindow) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            mark()
            Spacer(Modifier.width(Spacing.lg))
            Column {
                title()
                subtitle()
            }
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            mark()
            Spacer(Modifier.height(Spacing.lg))
            title()
            Spacer(Modifier.height(Spacing.xs))
            subtitle()
        }
    }
}

@Composable
private fun SignInForm(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit
) {
    AppTextField(
        value = state.email,
        onValueChange = onEmailChange,
        label = stringResource(R.string.login_email_label),
        placeholder = stringResource(R.string.login_email_placeholder),
        leadingIcon = Icons.Filled.Mail,
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next,
        isError = state.emailError != null,
        supportingText = state.emailError?.let { loginErrorMessage(it) },
        enabled = !state.isSubmitting
    )
    Spacer(Modifier.height(Spacing.lg))
    AppTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        label = stringResource(R.string.login_password_label),
        placeholder = stringResource(R.string.login_password_placeholder),
        leadingIcon = Icons.Filled.Lock,
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
        onImeAction = onSubmit,
        visualTransformation = if (state.isPasswordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisibility) {
                Icon(
                    imageVector = if (state.isPasswordVisible) {
                        Icons.Filled.VisibilityOff
                    } else {
                        Icons.Filled.Visibility
                    },
                    contentDescription = stringResource(
                        if (state.isPasswordVisible) R.string.login_hide_password
                        else R.string.login_show_password
                    )
                )
            }
        },
        isError = state.passwordError != null,
        supportingText = state.passwordError?.let { loginErrorMessage(it) },
        enabled = !state.isSubmitting
    )
}

@Composable
private fun DemoAccounts(
    accounts: List<DemoAccount>,
    selectedEmail: String,
    sideBySide: Boolean,
    onSelect: (DemoAccount) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.login_demo_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = stringResource(R.string.login_demo_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.md))
        if (sideBySide) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                accounts.forEach { account ->
                    DemoAccountOption(
                        account = account,
                        selected = account.email.equals(selectedEmail, ignoreCase = true),
                        compact = true,
                        onClick = { onSelect(account) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                accounts.forEach { account ->
                    DemoAccountOption(
                        account = account,
                        selected = account.email.equals(selectedEmail, ignoreCase = true),
                        compact = false,
                        onClick = { onSelect(account) }
                    )
                }
            }
        }
    }
}

/**
 * One demo account. [compact] centres the icon over the role so three fit
 * across a row on short windows; otherwise the email sits beside the role.
 */
@Composable
private fun DemoAccountOption(
    account: DemoAccount,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = (if (selected) Tone.Info else Tone.Neutral).colors()
    val shape = MaterialTheme.shapes.medium
    val surface = modifier
        .heightIn(min = 64.dp)
        .clip(shape)
        .background(
            if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
        .border(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
            shape = shape
        )
        .clickable(role = Role.Button, onClick = onClick)
        .semantics { this.selected = selected }

    val roleLabel = stringResource(account.role.labelRes)

    if (compact) {
        Column(
            modifier = surface.padding(Spacing.sm),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(account.role.icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = roleLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        return
    }

    Row(
        modifier = surface
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(account.role.icon, contentDescription = null, tint = c.ink, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = roleLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = account.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun loginErrorMessage(error: LoginError): String = stringResource(
    when (error) {
        LoginError.EMAIL_REQUIRED -> R.string.error_email_required
        LoginError.EMAIL_INVALID -> R.string.error_email_invalid
        LoginError.UNKNOWN_ACCOUNT -> R.string.error_unknown_account
        LoginError.PASSWORD_REQUIRED -> R.string.error_password_required
        LoginError.PASSWORD_INCORRECT -> R.string.error_password_incorrect
    }
)

private val previewAccounts = listOf(
    DemoAccount(UserRole.WORKER, "worker@mcc.co.za"),
    DemoAccount(UserRole.MANAGER, "manager@mcc.co.za"),
    DemoAccount(UserRole.ADMINISTRATOR, "admin@mcc.co.za")
)

@DevicePreviews
@Composable
private fun LoginScreenPreview() {
    PreviewSurface {
        LoginScreen(
            state = LoginUiState(demoAccounts = previewAccounts),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onDemoAccountSelected = {},
            onSubmit = {},
            onForgotPassword = {}
        )
    }
}

@DevicePreviews
@Composable
private fun LoginScreenErrorPreview() {
    PreviewSurface {
        LoginScreen(
            state = LoginUiState(
                email = "sipho@mcc.co.za",
                emailError = LoginError.UNKNOWN_ACCOUNT,
                passwordError = LoginError.PASSWORD_REQUIRED,
                demoAccounts = previewAccounts
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onDemoAccountSelected = {},
            onSubmit = {},
            onForgotPassword = {}
        )
    }
}
