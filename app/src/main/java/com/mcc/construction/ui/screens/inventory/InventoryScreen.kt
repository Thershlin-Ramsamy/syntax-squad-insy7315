package com.mcc.construction.ui.screens.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.data.legacy.StockLevel
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.SegmentedFilter
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.tone

private const val ALL = "All"

@Composable
fun InventoryScreen(navController: NavHostController) {
    var filter by rememberSaveable { mutableStateOf(ALL) }
    val materials = MockRepository.materials

    val options = remember { listOf(ALL) + StockLevel.entries.map { it.label } }
    val filtered =
        if (filter == ALL) materials else materials.filter { it.stockLevel.label == filter }

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
                icon = Icons.Filled.Inventory2,
                title = "Nothing $filter",
                message = "No materials currently sit at this stock level."
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
                items(filtered, key = { it.id }) { material ->
                    ListRow(
                        title = material.name,
                        subtitle = "${material.quantity} ${material.unit} · ${material.category}",
                        meta = material.location,
                        onClick = {
                            navController.navigate(Routes.materialDetail(material.id))
                        },
                        leading = {
                            LeadingIcon(Icons.Filled.Inventory2, material.stockLevel.tone)
                        },
                        trailing = {
                            StatusPill(material.stockLevel.label, material.stockLevel.tone)
                        }
                    )
                }
            }
        }
    }
}
