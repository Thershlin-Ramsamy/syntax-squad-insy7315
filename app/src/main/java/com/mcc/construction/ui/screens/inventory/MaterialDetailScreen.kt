package com.mcc.construction.ui.screens.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.mcc.construction.data.legacy.MockRepository
import com.mcc.construction.data.legacy.StockLevel
import com.mcc.construction.ui.components.AppCard
import com.mcc.construction.ui.components.Banner
import com.mcc.construction.ui.components.DetailScaffold
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.Eyebrow
import com.mcc.construction.ui.components.InfoRow
import com.mcc.construction.ui.components.KeyValueRow
import com.mcc.construction.ui.components.ProgressTrack
import com.mcc.construction.ui.components.ScreenPadding
import com.mcc.construction.ui.components.StatusPill
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.tone

@Composable
fun MaterialDetailScreen(navController: NavHostController, materialId: String) {
    val material = MockRepository.getMaterial(materialId)

    DetailScaffold(
        title = "Material",
        onBack = { navController.popBackStack() }
    ) {
        if (material == null) {
            EmptyState(
                icon = Icons.Filled.SearchOff,
                title = "Material not found",
                message = "This item may have been removed from the inventory.",
                actionLabel = "Go back",
                onAction = { navController.popBackStack() }
            )
            return@DetailScaffold
        }

        // Stock shown against twice the reorder level, so a healthy item sits near
        // the middle of the track and a low one is visibly close to empty.
        val fullMark = (material.reorderLevel * 2).coerceAtLeast(1)
        val fill = material.quantity.toFloat() / fullMark

        ScreenPadding {
            Eyebrow(material.category)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = material.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(Spacing.sm))
            StatusPill(material.stockLevel.label, material.stockLevel.tone, showDot = true)

            Spacer(Modifier.height(Spacing.xl))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "On hand",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${material.quantity} ${material.unit}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                ProgressTrack(fill, tone = material.stockLevel.tone)
                Spacer(Modifier.height(Spacing.lg))
                KeyValueRow("Reorder level", "${material.reorderLevel} ${material.unit}")
                Spacer(Modifier.height(Spacing.md))
                KeyValueRow("Unit cost", material.unitCost)
            }

            if (material.stockLevel != StockLevel.IN_STOCK) {
                Spacer(Modifier.height(Spacing.lg))
                Banner(
                    title = if (material.stockLevel == StockLevel.OUT) {
                        "Out of stock"
                    } else {
                        "Below reorder level"
                    },
                    message = "Raise a purchase order with ${material.supplier} " +
                        "before this holds up work on site.",
                    icon = Icons.Filled.WarningAmber,
                    tone = material.stockLevel.tone
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                InfoRow(Icons.Filled.Place, "Stored at", material.location)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.LocalShipping, "Supplier", material.supplier)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.Inventory2, "Last delivery", material.lastDelivery)
                Spacer(Modifier.height(Spacing.md))
                InfoRow(Icons.Filled.Category, "Category", material.category)
            }
        }
    }
}
