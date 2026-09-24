package com.mcc.construction.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.FilterChip
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
import com.mcc.construction.data.legacy.AccountStatus
import com.mcc.construction.data.legacy.ManagedUser
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.data.legacy.label
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.SecondaryButton
import com.mcc.construction.ui.components.SegmentedFilter
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.tone

private const val ALL = "All"

/**
 * User management.
 *
 * The two administrator actions CLAUDE.md calls for — changing a role and
 * suspending an account — are performed inline on the card rather than behind a
 * detail screen, because both are one-decision changes and the list is short.
 * Both write through [MockRepository], so the effect is visible immediately and
 * survives navigation within the session.
 */
@Composable
fun UserManagementScreen() {
    var filter by rememberSaveable { mutableStateOf(ALL) }
    val users = MockRepository.managedUsers

    val options = remember { listOf(ALL) + AccountStatus.entries.map { it.label } }
    val filtered = if (filter == ALL) users else users.filter { it.status.label == filter }

    Column(Modifier.fillMaxSize()) {
        SegmentedFilter(
            options = options,
            selected = filter,
            onSelect = { filter = it },
            modifier = Modifier.padding(
                horizontal = Spacing.screenH,
                vertical = Spacing.md
            )
        )

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Groups,
                title = "No $filter accounts",
                message = "No user accounts currently have this status."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.screenH,
                    end = Spacing.screenH,
                    top = Spacing.xs,
                    bottom = Spacing.xxxl
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
            ) {
                items(filtered, key = { it.id }) { user -> UserCard(user) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserCard(user: ManagedUser) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(user.name)
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(user.status.label, user.status.tone)
        }

        Spacer(Modifier.height(Spacing.lg))

        Text(
            text = "Role",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Spacing.sm))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            UserRole.entries.forEach { role ->
                FilterChip(
                    selected = user.role == role,
                    onClick = { MockRepository.changeUserRole(user.id, role) },
                    label = { Text(role.label) }
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        Text(
            text = "Last active ${user.lastActive}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(Spacing.md))

        val suspended = user.status == AccountStatus.SUSPENDED
        SecondaryButton(
            text = if (suspended) "Reinstate account" else "Suspend account",
            icon = if (suspended) Icons.Filled.CheckCircle else Icons.Filled.Block,
            onClick = { MockRepository.toggleUserSuspended(user.id) }
        )
    }
}
