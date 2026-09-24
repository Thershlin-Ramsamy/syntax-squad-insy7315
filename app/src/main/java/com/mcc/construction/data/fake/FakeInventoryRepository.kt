package com.mcc.construction.data.fake

import com.mcc.construction.data.model.Inventory
import com.mcc.construction.data.model.Supplier
import com.mcc.construction.data.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeInventoryRepository(seed: FakeSeedData) : InventoryRepository {

    private val items = MutableStateFlow(seed.inventory)
    private val suppliers = seed.suppliers

    override fun observeInventory(): Flow<List<Inventory>> = items

    override fun observeInventoryForProject(projectId: Int): Flow<List<Inventory>> =
        items.map { list -> list.filter { it.projectId == projectId } }

    override fun observeItem(materialId: Int): Flow<Inventory?> =
        items.map { list -> list.firstOrNull { it.materialId == materialId } }

    override fun observeSuppliers(): Flow<List<Supplier>> = flowOf(suppliers)

    override fun observeSupplier(supplierId: Int): Flow<Supplier?> =
        flowOf(suppliers.firstOrNull { it.supplierId == supplierId })

    override suspend fun adjustStock(materialId: Int, change: Int) {
        items.update { list ->
            list.map {
                if (it.materialId == materialId) {
                    it.copy(quantityAvailable = (it.quantityAvailable + change).coerceAtLeast(0))
                } else {
                    it
                }
            }
        }
    }
}
