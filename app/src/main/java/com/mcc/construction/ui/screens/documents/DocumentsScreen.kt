package com.mcc.construction.ui.screens.documents

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.SegmentedFilter
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

private const val ALL = "All"

@Composable
fun DocumentsScreen(navController: NavHostController) {
    var filter by rememberSaveable { mutableStateOf(ALL) }
    val documents = MockRepository.documents

    // Filter by project rather than file type: on site the question is "what
    // belongs to this job", not "show me every PDF".
    val options = remember(documents) {
        listOf(ALL) + documents.map { it.project }.distinct()
    }
    val filtered = if (filter == ALL) documents else documents.filter { it.project == filter }

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
                icon = Icons.Filled.FolderOpen,
                title = "No documents",
                message = "Nothing has been filed against this project yet."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Spacing.screenH,
                    end = Spacing.screenH,
                    top = Spacing.xs,
                    bottom = Spacing.xxxl
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { document ->
                    ListRow(
                        title = document.name,
                        subtitle = "${document.project} · ${document.sizeLabel}",
                        meta = "Updated ${document.updated}",
                        leading = { LeadingIcon(Icons.Filled.Description, Tone.Info) },
                        trailing = { StatusPill(document.type, Tone.Neutral) }
                    )
                }
            }
        }
    }
}
