package com.mcc.construction.ui.screens.messages

import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.WORKER_ID
import com.mcc.construction.ui.screens.keepCollecting
import com.mcc.construction.ui.screens.workerContainer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MessagesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun conversationsAreNewestFirstWithUnreadCounts() = runTest {
        val c = workerContainer()
        val vm = MessagesViewModel(c.authRepository, c.messageRepository)
        keepCollecting(vm.uiState)

        val list = vm.uiState.value.conversations!!
        assertEquals(list.sortedByDescending { it.lastMessage.timestamp }, list)
        assertEquals(3, vm.uiState.value.totalUnread)
        assertEquals(2, list.single { it.participant.userId == 3 }.unreadCount)
    }

    @Test
    fun readingAThreadClearsItsBadge() = runTest {
        val c = workerContainer()
        val vm = MessagesViewModel(c.authRepository, c.messageRepository)
        keepCollecting(vm.uiState)

        c.messageRepository.markThreadRead(WORKER_ID, 3)

        assertEquals(1, vm.uiState.value.totalUnread)
    }

    @Test
    fun paneShowsTheLatestConversationUntilOneIsChosen() = runTest {
        val c = workerContainer()
        val vm = MessagesViewModel(c.authRepository, c.messageRepository)
        keepCollecting(vm.uiState)
        val first = vm.uiState.value.conversations!!.first().participant.userId
        assertEquals(first, vm.uiState.value.paneUserId)

        vm.onSelectConversation(2)

        assertEquals(2, vm.uiState.value.paneUserId)
        assertTrue(vm.uiState.value.conversations!!.any { it.participant.userId == 2 })
    }
}
