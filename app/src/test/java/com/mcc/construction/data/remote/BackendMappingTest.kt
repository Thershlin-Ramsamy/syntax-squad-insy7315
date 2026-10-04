package com.mcc.construction.data.remote

import com.mcc.construction.data.remote.dto.BackendDates
import com.mcc.construction.data.remote.dto.InventoryRow
import com.mcc.construction.data.remote.dto.toModel
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BackendMappingTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun utcTimestampIsConvertedToSouthAfricanTime() {
        val result = BackendDates.localDateTime(
            "2026-10-04T22:30:00+00:00"
        )

        assertEquals(
            LocalDateTime.of(2026, 10, 5, 0, 30),
            result
        )
    }

    @Test
    fun explicitSouthAfricanOffsetPreservesLocalTime() {
        val result = BackendDates.localDateTime(
            "2026-10-04T08:15:00+02:00"
        )

        assertEquals(
            LocalDateTime.of(2026, 10, 4, 8, 15),
            result
        )
    }

    @Test
    fun numericCurrencyIsDecodedWithoutFloatingPointConversion() {
        val source = """
            {
              "material_id": 1,
              "project_id": 2,
              "supplier_id": 3,
              "material_name": "Concrete",
              "quantity_available": 100,
              "minimum_quantity": 10,
              "unit_type": "bags",
              "unit_cost": 1234567890.12,
              "updated_at": "2026-10-04T08:00:00+00:00"
            }
        """.trimIndent()

        val row = json.decodeFromString<InventoryRow>(source)
        val model = row.toModel()

        assertEquals(
            BigDecimal("1234567890.12"),
            model.unitCost
        )
    }
}