package com.mcc.construction.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mcc.construction.data.AuditSeverity
import com.mcc.construction.data.MockRepository
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.Eyebrow
import com.mcc.construction.ui.components.SegmentedFilter
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.label
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.tone

private const val ALL = "All"

@Composable
fun AuditLogScreen() {
    var filter by rememberSaveable { mutableStateOf(ALL) }
    val entries = MockRepository.auditLog

    val options = remember { listOf(ALL) + AuditSeverity.entries.map { it.label } }
    val filtered = if (filter == ALL) entries else entries.filter { it.severity.label == filter }

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
                icon = Icons.AutoMirrored.Filled.FactCheck,
                title = "No $filter events",
                message = "Nothing at this severity has been recorded."
            )
            return@Column
        }

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
            items(filtered, key = { it.id }) { entry ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow(entry.timestamp)
                            Spacer(Modifier.height(Spacing.xxs))
                            Text(
                                text = entry.action,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        StatusPill(entry.severity.label, entry.severity.tone)
                    }
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = entry.target,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = "${entry.actor} · ${entry.ipAddress}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
