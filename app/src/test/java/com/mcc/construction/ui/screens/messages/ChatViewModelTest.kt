package com.mcc.construction.ui.screens.messages

import com.mcc.construction.R
import com.mcc.construction.data.fake.FakeAppContainer
import com.mcc.construction.ui.MainDispatcherRule
import com.mcc.construction.ui.screens.WORKER_ID
import com.mcc.construction.ui.screens.keepCollecting
import com.mcc.construction.ui.screens.workerContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val johan = 3

    private fun viewModel(c: FakeAppContainer) = ChatViewModel(
        johan, c.authRepository, c.messageRepository, c.userRepository, c.syncRepository, c.clock
    )

    @Test
    fun opensTheThreadInOrderAndMarksItRead() = runTest {
        val c = workerContainer()
        val vm = viewModel(c)
        keepCollecting(vm.uiState)

        val state = vm.uiState.value
        assertEquals("Johan Botha", state.participant?.fullName)
        assertEquals(state.messages.sortedBy { it.timestamp }, state.messages)
        assertEquals(0, c.messageRepository.observeConversations(WORKER_ID).first().single { it.participant.userId == johan }.unreadCount)
    }

    @Test
    fun sentMessagesPersistAndTheDraftClears() = runTest {
        val c = workerContainer()
        val vm = viewModel(c)
        keepCollecting(vm.uiState)
        val before = vm.uiState.value.messages.size

        vm.onDraftChange("  Rebar spacing checked at 150 mm  ")
        vm.onSend()

        assertEquals(before + 1, vm.uiState.value.messages.size)
        assertEquals("Rebar spacing checked at 150 mm", vm.uiState.value.messages.last().content)
        assertEquals("", vm.uiState.value.draft)
        // A new ViewModel on the same repository still sees it — the session keeps it.
        val reopened = viewModel(c)
        keepCollecting(reopened.uiState)
        assertEquals(before + 1, reopened.uiState.value.messages.size)
    }

    @Test
    fun blankDraftIsNotSent() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)
        val before = vm.uiState.value.messages.size

        vm.onDraftChange("   ")
        vm.onSend()

        assertFalse(vm.uiState.value.canSend)
        assertEquals(before, vm.uiState.value.messages.size)
    }

    @Test
    fun quickReplySendsInOneTapAndKeepsTheDraft() = runTest {
        val vm = viewModel(workerContainer())
        keepCollecting(vm.uiState)
        vm.onDraftChange("half typed")

        vm.onQuickReply("On my way")

        assertEquals("On my way", vm.uiState.value.messages.last().content)
        assertEquals("half typed", vm.uiState.value.draft)
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun offlineSendSaysItIsQueued() = runTest {
        val c = workerContainer()
        c.syncRepository.setOfflineMode(true)
        val vm = viewModel(c)
        keepCollecting(vm.uiState)

        vm.onQuickReply("Done")

        assertEquals(R.string.chat_sent_offline, vm.uiState.value.message?.textRes)
    }
}
