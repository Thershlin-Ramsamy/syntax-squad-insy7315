package com.mcc.construction.ui.screens.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.data.model.Conversation
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.MessageRepository
import com.mcc.construction.ui.common.appContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class MessagesUiState(
    /** Null until the first load, so the screen can tell loading from empty. */
    val conversations: List<Conversation>? = null,
    val myUserId: Int? = null,
    /** The conversation open beside the list on wide windows. */
    val selectedUserId: Int? = null
) {
    val totalUnread: Int get() = conversations.orEmpty().sumOf { it.unreadCount }

    /** What the chat pane shows: the chosen person, or the most recent conversation. */
    val paneUserId: Int?
        get() = selectedUserId?.takeIf { id -> conversations.orEmpty().any { it.participant.userId == id } }
            ?: conversations?.firstOrNull()?.participant?.userId
}

/** Everyone the signed-in user has messaged, most recent first, with unread counts. */
class MessagesViewModel(
    authRepository: AuthRepository,
    messageRepository: MessageRepository
) : ViewModel() {

    private val selected = MutableStateFlow<Int?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val conversations = authRepository.session.flatMapLatest { session ->
        if (session == null) flowOf(null) else messageRepository.observeConversations(session.user.userId)
    }

    val uiState: StateFlow<MessagesUiState> = combine(
        conversations,
        authRepository.session,
        selected
    ) { list, session, selectedId ->
        MessagesUiState(conversations = list, myUserId = session?.user?.userId, selectedUserId = selectedId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MessagesUiState())

    fun onSelectConversation(userId: Int) = selected.update { userId }

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                val c = appContainer()
                MessagesViewModel(c.authRepository, c.messageRepository)
            }
        }
    }
}
