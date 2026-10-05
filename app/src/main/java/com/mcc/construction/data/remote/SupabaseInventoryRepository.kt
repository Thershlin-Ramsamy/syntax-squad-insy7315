package com.mcc.construction.data.remote

import com.mcc.construction.data.model.Inventory
import com.mcc.construction.data.model.Supplier
import com.mcc.construction.data.remote.dto.InventoryRow
import com.mcc.construction.data.remote.dto.SupplierRow
import com.mcc.construction.data.repository.InventoryRepository
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseInventoryRepository(
    private val reads: BackendReadStore,
    private val writes: BackendRepositorySupport
) : InventoryRepository {

    override fun observeInventory(): Flow<List<Inventory>> {
        return reads.observeRows<InventoryRow>(
            table = "inventory",
            primaryKey = "material_id"
        ).map { rows ->
            rows.map { row ->
                mapInventory(row)
            }.sortedBy {
                it.materialId
            }
        }
    }

    override fun observeInventoryForProject(
        projectId: Int
    ): Flow<List<Inventory>> {
        return observeInventory().map { inventory ->
            inventory.filter {
                it.projectId == projectId
            }
        }
    }

    override fun observeItem(
        materialId: Int
    ): Flow<Inventory?> {
        return observeInventory().map { inventory ->
            inventory.firstOrNull {
                it.materialId == materialId
            }
        }
    }

    override fun observeSuppliers(): Flow<List<Supplier>> {
        return reads.observeRows<SupplierRow>(
            table = "suppliers",
            primaryKey = "supplier_id"
        ).map { rows ->
            rows.map { row ->
                mapSupplier(row)
            }.sortedWith(
                compareBy<Supplier> {
                    it.companyName.lowercase()
                }.thenBy {
                    it.supplierId
                }
            )
        }
    }

    override fun observeSupplier(
        supplierId: Int
    ): Flow<Supplier?> {
        return observeSuppliers().map { suppliers ->
            suppliers.firstOrNull {
                it.supplierId == supplierId
            }
        }
    }

    override suspend fun adjustStock(
        materialId: Int,
        change: Int
    ) {
        require(materialId > 0) {
            "A positive material ID is required."
        }

        if (change == 0) {
            return
        }

        writes.execute(
            action = BackendAction.STOCK_ADJUST,
            payload = buildJsonObject {
                put("material_id", materialId)
                put("change", change)
            }
        )
    }

    private fun mapInventory(
        row: InventoryRow
    ): Inventory {
        return Inventory(
            materialId = row.materialId,
            projectId = row.projectId,
            supplierId = row.supplierId,
            materialName = row.materialName,
            quantityAvailable = row.quantityAvailable,
            minimumQuantity = row.minimumQuantity,
            unitType = row.unitType,
            unitCost = BigDecimal(row.unitCost.content)
        )
    }

    private fun mapSupplier(
        row: SupplierRow
    ): Supplier {
        return Supplier(
            supplierId = row.supplierId,
            companyName = row.companyName,
            contactPerson = row.contactPerson,
            phone = row.phone,
            email = row.email
        )
    }
}