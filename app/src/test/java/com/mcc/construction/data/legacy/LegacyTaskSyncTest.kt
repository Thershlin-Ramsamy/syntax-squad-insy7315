package com.mcc.construction.data.legacy

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyTaskSyncTest {

    @Test
    fun statusesFromTheRepositoryReachTheLegacyList() {
        val before = MockRepository.tasks.single { it.id == "T-102" }.status

        MockRepository.syncTaskStatuses(mapOf(102 to TaskStatus.COMPLETED))

        assertEquals(TaskStatus.COMPLETED, MockRepository.tasks.single { it.id == "T-102" }.status)
        MockRepository.syncTaskStatuses(mapOf(102 to before))
    }
}
