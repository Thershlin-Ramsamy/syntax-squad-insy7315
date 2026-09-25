package com.mcc.construction.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.ChoiceChipGroup
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.components.SearchField
import com.mcc.construction.ui.components.SectionHeader
import com.mcc.construction.ui.format.formatDate
import com.mcc.construction.ui.format.relativeTime
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun AuditLogRoute(viewModel: AuditLogViewModel = viewModel(factory = AuditLogViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AuditLogScreen(state, viewModel::onQueryChange, viewModel::onActorFilterChange)
}

/** Every audited action, newest first and grouped by day, searchable and filterable by person. */
@Composable
fun AuditLogScreen(
    state: AuditLogUiState,
    onQueryChange: (String) -> Unit,
    onActorFilterChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.entries == null) {
        LoadingState(modifier)
        return
    }
    ScreenScaffold(modifier = modifier, scrollable = false) {
        SearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            label = stringResource(R.string.audit_search),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.md))
        val today = state.now.toLocalDate()
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = Spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            item(key = "filters") {
                ChoiceChipGroup(
                    options = listOf<Int?>(null) + state.actors.map { it.userId },
                    selected = state.actorFilter,
                    onSelect = onActorFilterChange,
                    label = { id ->
                        if (id == null) stringResource(R.string.audit_everyone) else state.actors.first { it.userId == id }.fullName
                    }
                )
            }
            if (state.visible.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Filled.History,
                        title = stringResource(R.string.audit_empty_title),
                        message = stringResource(R.string.audit_empty_message)
                    )
                }
            }
            state.visible.groupBy { it.log.timestamp.toLocalDate() }.forEach { (day, entries) ->
                item(key = "day-$day") {
                    SectionHeader(
                        title = dayLabel(day, today),
                        modifier = Modifier.padding(top = Spacing.md)
                    )
                }
                items(entries, key = { it.log.logId }) { entry ->
                    AppCard(Modifier.fillMaxWidth()) { AuditEntryRow(entry, state.now) }
                }
            }
        }
    }
}

@Composable
private fun dayLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today -> stringResource(R.string.dashboard_today)
    today.minusDays(1) -> stringResource(R.string.audit_yesterday)
    else -> day.formatDate()
}

/** Who did what, and when. Shared with the administration dashboard. */
@Composable
internal fun AuditEntryRow(entry: AuditEntry, now: LocalDateTime) {
    Row(verticalAlignment = Alignment.Top) {
        Avatar(entry.actorName.ifEmpty { "?" })
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(entry.log.action, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = stringResource(R.string.audit_by_when, entry.actorName, relativeTime(entry.log.timestamp, now)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@DevicePreviews
@Composable
private fun AuditLogPreview() {
    val now = LocalDateTime.now()
    PreviewSurface {
        AuditLogScreen(
            AuditLogUiState(
                entries = previewAudit(now),
                actors = listOf(
                    User(1, UserRole.ADMINISTRATOR, "Riaan", "van Wyk", "admin@mcc.co.za", "", "Systems Administrator", "MCC-0002"),
                    User(2, UserRole.MANAGER, "Naledi", "Khumalo", "manager@mcc.co.za", "", "Site Manager", "MCC-1008")
                ),
                now = now
            ),
            onQueryChange = {},
            onActorFilterChange = {}
        )
    }
}
