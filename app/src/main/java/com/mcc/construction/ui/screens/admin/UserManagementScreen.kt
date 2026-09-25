package com.mcc.construction.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.AccentButton
import com.mcc.construction.ui.components.ActionFeedbackHost
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.ChoiceChipGroup
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.InfoRow
import com.mcc.construction.ui.components.ListDetailLayout
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.PrimaryButton
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SearchField
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.components.StatusChip
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.components.selectedOutline
import com.mcc.construction.ui.descriptionRes
import com.mcc.construction.ui.icon
import com.mcc.construction.ui.labelRes
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

/** Callbacks for User Management. */
data class UserManagementActions(
    val onQueryChange: (String) -> Unit = {},
    val onRoleFilterChange: (UserRole?) -> Unit = {},
    val onAccountFilterChange: (AccountFilter) -> Unit = {},
    val onSelectUser: (Int) -> Unit = {},
    val onCloseUser: () -> Unit = {},
    val onChangeRole: (Int, UserRole) -> Unit = { _, _ -> },
    val onRequestSuspend: (Int) -> Unit = {},
    val onDismissSuspend: () -> Unit = {},
    val onConfirmSuspend: () -> Unit = {},
    val onRestore: (Int) -> Unit = {},
    val onMessageShown: () -> Unit = {}
)

@Composable
fun UserManagementRoute(viewModel: UserManagementViewModel = viewModel(factory = UserManagementViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UserManagementScreen(
        state = state,
        twoPane = LocalWindowLayout.current.isExpanded,
        actions = UserManagementActions(
            onQueryChange = viewModel::onQueryChange,
            onRoleFilterChange = viewModel::onRoleFilterChange,
            onAccountFilterChange = viewModel::onAccountFilterChange,
            onSelectUser = viewModel::onSelectUser,
            onCloseUser = viewModel::onCloseUser,
            onChangeRole = viewModel::onChangeRole,
            onRequestSuspend = viewModel::onRequestSuspend,
            onDismissSuspend = viewModel::onDismissSuspend,
            onConfirmSuspend = viewModel::onConfirmSuspend,
            onRestore = viewModel::onRestore,
            onMessageShown = viewModel::onMessageShown
        )
    )
}

/**
 * Every account, searchable and filterable. Picking one opens its role and
 * access controls: beside the list on a wide window, in a dialog on a phone.
 */
@Composable
fun UserManagementScreen(
    state: UserManagementUiState,
    twoPane: Boolean,
    actions: UserManagementActions,
    modifier: Modifier = Modifier
) {
    if (state.users == null) {
        LoadingState(modifier)
        return
    }
    ActionFeedbackHost(state.message, actions.onMessageShown, modifier) {
        if (twoPane) {
            ListDetailLayout(
                selected = state.paneUser,
                emptyIcon = Icons.Filled.ManageAccounts,
                emptyTitle = stringResource(R.string.users_pick_title),
                emptyMessage = stringResource(R.string.users_pick_message),
                list = { UserColumn(state, actions, highlight = true) },
                detail = { user ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = Spacing.xxl, bottom = Spacing.xxxl)
                    ) { AccountPanel(user, state, actions) }
                }
            )
        } else {
            ScreenScaffold(scrollable = false) { UserColumn(state, actions, highlight = false) }
            state.selectedUser?.let { user ->
                AlertDialog(
                    onDismissRequest = actions.onCloseUser,
                    title = { Text(stringResource(R.string.users_manage_title)) },
                    text = { Column(Modifier.verticalScroll(rememberScrollState())) { AccountPanel(user, state, actions) } },
                    confirmButton = { TextButton(onClick = actions.onCloseUser) { Text(stringResource(R.string.action_close)) } }
                )
            }
        }
    }

    state.confirmSuspendId?.let { id ->
        val user = state.users.firstOrNull { it.userId == id } ?: return@let
        AlertDialog(
            onDismissRequest = actions.onDismissSuspend,
            icon = { Icon(Icons.Filled.Block, contentDescription = null) },
            iconContentColor = MaterialTheme.colorScheme.error,
            title = { Text(stringResource(R.string.users_suspend_title, user.fullName)) },
            text = { Text(stringResource(R.string.users_suspend_message)) },
            confirmButton = {
                TextButton(onClick = actions.onConfirmSuspend) {
                    Text(stringResource(R.string.users_suspend), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = actions.onDismissSuspend) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun ColumnScope.UserColumn(state: UserManagementUiState, actions: UserManagementActions, highlight: Boolean) {
    SearchField(
        query = state.query,
        onQueryChange = actions.onQueryChange,
        label = stringResource(R.string.users_search),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(Spacing.md))
    val selected = if (highlight) state.paneUser?.userId else null
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = Spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item(key = "filters") {
            Column {
                ChoiceChipGroup(
                    options = listOf(null) + UserRole.entries,
                    selected = state.roleFilter,
                    onSelect = actions.onRoleFilterChange,
                    label = { role ->
                        val name = if (role == null) stringResource(R.string.filter_all) else stringResource(role.labelRes)
                        stringResource(R.string.filter_with_count, name, state.countFor(role))
                    }
                )
                Spacer(Modifier.height(Spacing.sm))
                ChoiceChipGroup(
                    options = AccountFilter.entries.toList(),
                    selected = state.accountFilter,
                    onSelect = actions.onAccountFilterChange,
                    label = { stringResource(it.labelRes) }
                )
            }
        }
        if (state.visible.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Filled.PersonSearch,
                    title = stringResource(R.string.users_empty_title),
                    message = stringResource(R.string.users_empty_message)
                )
            }
        }
        items(state.visible, key = { it.userId }) { user ->
            ListRow(
                title = user.fullName,
                subtitle = user.email,
                meta = stringResource(user.role.labelRes),
                onClick = { actions.onSelectUser(user.userId) },
                leading = { Avatar(user.fullName) },
                trailing = {
                    if (user.isActive) {
                        StatusPill(stringResource(R.string.users_active), Tone.Success, showDot = true)
                    } else {
                        StatusPill(stringResource(R.string.employee_suspended), Tone.Danger, showDot = true)
                    }
                },
                modifier = Modifier.selectedOutline(user.userId == selected)
            )
        }
    }
}

/** Account details, role picker and suspend or restore. */
@Composable
private fun AccountPanel(user: User, state: UserManagementUiState, actions: UserManagementActions) {
    val self = state.isSelf(user)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(user.fullName, size = 56.dp)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(user.fullName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(user.jobTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusChip(
            text = stringResource(if (user.isActive) R.string.users_active else R.string.employee_suspended),
            tone = if (user.isActive) Tone.Success else Tone.Danger
        )
    }
    Spacer(Modifier.height(Spacing.lg))
    AppCard(Modifier.fillMaxWidth()) {
        InfoRow(Icons.Filled.Email, stringResource(R.string.profile_email), user.email)
        Spacer(Modifier.height(Spacing.md))
        InfoRow(Icons.Filled.Phone, stringResource(R.string.profile_phone), user.phone)
        Spacer(Modifier.height(Spacing.md))
        InfoRow(Icons.Filled.Badge, stringResource(R.string.profile_employee_number), user.employeeNumber)
    }
    if (self) {
        Spacer(Modifier.height(Spacing.lg))
        Banner(
            title = stringResource(R.string.users_self_title),
            message = stringResource(R.string.users_self_message),
            icon = Icons.Filled.ManageAccounts,
            tone = Tone.Info
        )
        return
    }

    Spacer(Modifier.height(Spacing.sectionGap))
    SectionHeader(title = stringResource(R.string.users_role))
    Spacer(Modifier.height(Spacing.blockGap))
    ChoiceChipGroup(
        options = UserRole.entries.toList(),
        selected = user.role,
        onSelect = { role -> actions.onChangeRole(user.userId, role) },
        label = { stringResource(it.labelRes) },
        icon = { it.icon }
    )
    Spacer(Modifier.height(Spacing.sm))
    Text(
        text = stringResource(user.role.descriptionRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(Spacing.sectionGap))
    SectionHeader(title = stringResource(R.string.users_access))
    Spacer(Modifier.height(Spacing.blockGap))
    if (user.isActive) {
        AccentButton(
            text = stringResource(R.string.users_suspend),
            onClick = { actions.onRequestSuspend(user.userId) },
            icon = Icons.Filled.Block
        )
    } else {
        PrimaryButton(
            text = stringResource(R.string.users_restore),
            onClick = { actions.onRestore(user.userId) },
            icon = Icons.Filled.LockOpen,
            loading = state.isSaving
        )
    }
}

private val AccountFilter.labelRes: Int
    get() = when (this) {
        AccountFilter.ALL -> R.string.users_filter_all
        AccountFilter.ACTIVE -> R.string.users_filter_active
        AccountFilter.SUSPENDED -> R.string.users_filter_suspended
    }

private fun previewState(): UserManagementUiState {
    fun user(id: Int, role: UserRole, first: String, last: String, job: String, active: Boolean = true) =
        User(id, role, first, last, "${first.lowercase()}@mcc.co.za", "+27 82 000 0000", job, "MCC-$id", active)
    return UserManagementUiState(
        users = listOf(
            user(1, UserRole.ADMINISTRATOR, "Riaan", "van Wyk", "Systems Administrator"),
            user(2, UserRole.MANAGER, "Naledi", "Khumalo", "Site Manager"),
            user(9, UserRole.MANAGER, "Pieter", "Coetzee", "Quantity Surveyor", active = false),
            user(5, UserRole.WORKER, "Sipho", "Dlamini", "Site Operative")
        ),
        currentUserId = 1,
        selectedUserId = 5
    )
}

@DevicePreviews
@Composable
private fun UserManagementPreview() {
    PreviewSurface {
        UserManagementScreen(previewState(), twoPane = LocalWindowLayout.current.isExpanded, actions = UserManagementActions())
    }
}
