package com.mcc.construction.ui.screens.login

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcc.construction.data.MockRepository
import com.mcc.construction.data.UserRole
import com.mcc.construction.navigation.NavMenus
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.AppTextField
import com.mcc.construction.ui.components.BrandMark
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.theme.NavyBlue
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone
import com.mcc.construction.ui.theme.colors

/**
 * Sign-in.
 *
 * The role selector is a prototype affordance: there is no real authentication
 * in Task 1, so choosing a role here is what lets one build demonstrate the
 * Worker, Manager and Administrator interfaces. In a real build the role would
 * come back from the auth response and this control would not exist.
 */
@Composable
fun LoginScreen(navController: NavHostController) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var selectedRole by rememberSaveable { mutableStateOf(UserRole.WORKER) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (username.isBlank() || password.isBlank()) {
            error = "Enter your username and password to continue."
            return
        }
        MockRepository.currentRole.value = selectedRole
        navController.navigate(NavMenus.homeRoute(selectedRole)) {
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // A landscape desktop window is wide but short. Stacked vertically the
        // form runs past ~700dp, which pushes Sign In off the bottom — the
        // primary action should not need a scroll to find. Below that height
        // the brand block goes horizontal and the role options sit side by
        // side, which reclaims roughly 250dp and brings the button back into
        // view without changing the layout on a phone.
        val shortWindow = maxHeight < 720.dp
        val brandSize = if (shortWindow) 48.dp else 72.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
        Column(
            modifier = Modifier
                // Caps the form on tablets — a sign-in field stretched across
                // 1200dp is unreadable and looks unfinished. Widened when the
                // role options are laid out in a row so three fit comfortably.
                .widthIn(max = if (shortWindow) 620.dp else 460.dp)
                .padding(
                    horizontal = Spacing.screenH,
                    vertical = if (shortWindow) Spacing.xl else Spacing.xxxl
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val mark = @Composable {
                Box(
                    modifier = Modifier
                        .size(brandSize)
                        .clip(CircleShape)
                        .background(NavyBlue),
                    contentAlignment = Alignment.Center
                ) {
                    BrandMark(
                        modifier = Modifier.size(brandSize * 0.61f),
                        grooveColor = NavyBlue
                    )
                }
            }
            val title = @Composable {
                Text(
                    text = "Sign in",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            val subtitle = @Composable {
                Text(
                    text = "MCC Construction Site Management",
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
                Spacer(Modifier.height(Spacing.xl))
            } else {
                mark()
                Spacer(Modifier.height(Spacing.lg))
                title()
                Spacer(Modifier.height(Spacing.xs))
                subtitle()
                Spacer(Modifier.height(Spacing.xxxl))
            }

            AppTextField(
                value = username,
                onValueChange = { username = it; error = null },
                label = "Username",
                placeholder = "s.dlamini",
                leadingIcon = Icons.Filled.Person
            )
            Spacer(Modifier.height(Spacing.lg))
            AppTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = "Password",
                placeholder = "Enter your password",
                leadingIcon = Icons.Filled.Lock,
                keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                            else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password"
                            else "Show password"
                        )
                    }
                },
                isError = error != null,
                supportingText = error
            )

            Spacer(Modifier.height(Spacing.xl))

            Text(
                text = "Continue as",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.sm))
            if (shortWindow) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    UserRole.entries.forEach { role ->
                        RoleOption(
                            role = role,
                            icon = role.icon,
                            selected = selectedRole == role,
                            onClick = { selectedRole = role },
                            compact = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                UserRole.entries.forEach { role ->
                    RoleOption(
                        role = role,
                        icon = role.icon,
                        selected = selectedRole == role,
                        onClick = { selectedRole = role }
                    )
                    Spacer(Modifier.height(Spacing.sm))
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton(text = "Sign In", onClick = { submit() })
            Spacer(Modifier.height(if (shortWindow) Spacing.sm else Spacing.lg))
            Text(
                text = "Forgot password?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable { navController.navigate(Routes.FORGOT_PASSWORD) }
                    .padding(Spacing.sm)
            )
        }
        }
    }
}

private val UserRole.icon: ImageVector
    get() = when (this) {
        UserRole.WORKER -> Icons.Filled.Engineering
        UserRole.MANAGER -> Icons.Filled.SupervisorAccount
        UserRole.ADMINISTRATOR -> Icons.Filled.AdminPanelSettings
    }

/**
 * One selectable role.
 *
 * [compact] drops the blurb and centres icon over label so three options fit
 * across a single row — used on short windows where the stacked cards would
 * push the sign-in button below the fold.
 */
@Composable
private fun RoleOption(
    role: UserRole,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val tone = if (selected) Tone.Info else Tone.Neutral
    val c = tone.colors()

    val surface = Modifier
        .height(64.dp)
        .clip(MaterialTheme.shapes.medium)
        .background(
            if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
        .border(
            width = if (selected) 0.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.outlineVariant,
            shape = MaterialTheme.shapes.medium
        )
        .clickable(onClick = onClick)

    if (compact) {
        Column(
            modifier = modifier
                .then(surface)
                .padding(horizontal = Spacing.sm),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.ink,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = role.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(surface)
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = c.ink,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(
                text = role.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = role.blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
