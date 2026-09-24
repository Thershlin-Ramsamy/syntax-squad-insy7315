package com.mcc.construction.data.model

import java.math.BigDecimal

enum class StockLevel { IN_STOCK, LOW, OUT }

/**
 * A row of the INVENTORY table: one material held for one project.
 *
 * [unitCost] is not in the ERD yet. It lets the material screens show rand
 * values and is listed in docs/ARCHITECTURE.md under "Fields beyond the ERD".
 */
data class Inventory(
    val materialId: Int,
    val projectId: Int,
    val supplierId: Int,
    val materialName: String,
    val quantityAvailable: Int,
    val minimumQuantity: Int,
    val unitType: String,
    val unitCost: BigDecimal
) {
    /** Mirrors the class diagram's `checkLowStock()`. */
    val stockLevel: StockLevel
        get() = when {
            quantityAvailable <= 0 -> StockLevel.OUT
            quantityAvailable <= minimumQuantity -> StockLevel.LOW
            else -> StockLevel.IN_STOCK
        }
}
