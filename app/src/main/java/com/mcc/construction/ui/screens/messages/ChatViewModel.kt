package com.mcc.construction.ui.screens.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mcc.construction.R
import com.mcc.construction.data.model.Message
import com.mcc.construction.data.model.User
import com.mcc.construction.data.repository.AuthRepository
import com.mcc.construction.data.repository.MessageRepository
import com.mcc.construction.data.repository.SyncRepository
import com.mcc.construction.data.repository.UserRepository
import com.mcc.construction.ui.common.UiMessage
import com.mcc.construction.ui.common.appContainer
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val isLoading: Boolean = true,
    val participant: User? = null,
    val myUserId: Int? = null,
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val isSending: Boolean = false,
    val today: LocalDate = LocalDate.MIN,
    val message: UiMessage? = null
) {
    val canSend: Boolean
        get() = draft.isNotBlank() && !isSending
}

class ChatViewModel(
    private val otherUserId: Int,
    private val authRepository: AuthRepository,
    private val messageRepository: MessageRepository,
    userRepository: UserRepository,
    private val syncRepository: SyncRepository,
    private val clock: Clock
) : ViewModel() {

    private val transient = MutableStateFlow(Transient())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val thread = authRepository.session
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                messageRepository.observeThread(
                    session.user.userId,
                    otherUserId
                )
            }
        }

    val uiState: StateFlow<ChatUiState> = combine(
        thread,
        userRepository.observeUser(otherUserId),
        authRepository.session,
        transient
    ) { messages, participant, session, extra ->
        ChatUiState(
            isLoading = messages == null,
            participant = participant,
            myUserId = session?.user?.userId,
            messages = messages.orEmpty(),
            draft = extra.draft,
            isSending = extra.isSending,
            today = LocalDate.now(clock),
            message = extra.message
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        ChatUiState()
    )

    init {
        thread.onEach { messages ->
            val me = authRepository.session.value
                ?.user?.userId ?: return@onEach

            if (
                messages.orEmpty().any {
                    it.receiverId == me && !it.isRead
                }
            ) {
                try {
                    messageRepository.markThreadRead(
                        me,
                        otherUserId
                    )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    transient.update {
                        it.copy(
                            message = UiMessage(
                                R.string.backend_operation_failed,
                                isError = true
                            )
                        )
                    }
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onDraftChange(text: String) {
        transient.update {
            it.copy(draft = text)
        }
    }

    fun onSend() {
        send(transient.value.draft, clearDraft = true)
    }

    fun onQuickReply(text: String) {
        send(text, clearDraft = false)
    }

    fun onMessageShown() {
        transient.update {
            it.copy(message = null)
        }
    }

    private fun send(
        text: String,
        clearDraft: Boolean
    ) {
        val me = authRepository.session.value
            ?.user?.userId ?: return

        val content = text.trim()

        if (
            content.isEmpty() ||
            transient.value.isSending
        ) {
            return
        }

        transient.update {
            it.copy(isSending = true)
        }

        viewModelScope.launch {
            try {
                val saved = messageRepository.sendMessage(
                    me,
                    otherUserId,
                    content
                )

                val feedback = when {
                    saved.messageId < 0 ->
                        UiMessage(R.string.backend_message_queued)

                    !syncRepository.status.value.isOnline ->
                        UiMessage(R.string.chat_sent_offline)

                    else -> null
                }

                transient.update {
                    it.copy(
                        draft = if (
                            clearDraft && it.draft == text
                        ) {
                            ""
                        } else {
                            it.draft
                        },
                        message = feedback
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                transient.update {
                    it.copy(
                        message = UiMessage(
                            R.string.backend_operation_failed,
                            isError = true
                        )
                    )
                }
            } finally {
                transient.update {
                    it.copy(isSending = false)
                }
            }
        }
    }

    private data class Transient(
        val draft: String = "",
        val isSending: Boolean = false,
        val message: UiMessage? = null
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        fun factory(otherUserId: Int) = viewModelFactory {
            initializer {
                val c = appContainer()

                ChatViewModel(
                    otherUserId,
                    c.authRepository,
                    c.messageRepository,
                    c.userRepository,
                    c.syncRepository,
                    c.clock
                )
            }
        }
    }
}