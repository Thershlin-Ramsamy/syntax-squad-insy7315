package com.mcc.construction.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Message
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.components.ActionFeedbackHost
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.ChoiceChip
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.format.formatDate
import com.mcc.construction.ui.format.formatTime
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Chat wired to its ViewModel.
 *
 * @param embedded true when shown beside the chat list on a wide window: no
 *   app bar of its own, just the person's name above the conversation.
 */
@Composable
fun ChatRoute(
    otherUserId: Int,
    onBack: () -> Unit,
    embedded: Boolean = false,
    viewModel: ChatViewModel = viewModel(key = "chat-$otherUserId", factory = ChatViewModel.factory(otherUserId))
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        embedded = embedded,
        onBack = onBack,
        onDraftChange = viewModel::onDraftChange,
        onSend = viewModel::onSend,
        onQuickReply = viewModel::onQuickReply,
        onMessageShown = viewModel::onMessageShown
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    embedded: Boolean,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickReply: (String) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = state.participant?.fullName ?: stringResource(R.string.title_messages)
    ActionFeedbackHost(state.message, onMessageShown, modifier, bottomInset = COMPOSER_HEIGHT) {
        ScreenScaffold(
            title = if (embedded) null else name,
            onBack = if (embedded) null else onBack,
            scrollable = false,
            maxContentWidth = if (embedded) Dp.Unspecified else Spacing.contentMaxWidth
        ) {
            if (embedded) {
                PaneHeader(state.participant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Box(Modifier.weight(1f)) {
                when {
                    state.isLoading -> LoadingState()
                    state.messages.isEmpty() -> EmptyState(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = stringResource(R.string.chat_empty_title),
                        message = stringResource(R.string.chat_empty_message, name)
                    )
                    else -> MessageList(state)
                }
            }
            Composer(state, onDraftChange, onSend, onQuickReply)
        }
    }
}

@Composable
private fun PaneHeader(participant: User?) {
    if (participant == null) return
    Row(
        modifier = Modifier.padding(bottom = Spacing.md, start = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(participant.fullName, size = 44.dp)
        Spacer(Modifier.width(Spacing.md))
        Column {
            Text(participant.fullName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(participant.jobTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MessageList(state: ChatUiState) {
    val listState = rememberLazyListState()
    // Open at the newest message, and follow new ones as they arrive.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Spacing.md, horizontal = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        itemsIndexed(state.messages, key = { _, m -> m.messageId }) { index, message ->
            val day = message.timestamp.toLocalDate()
            if (index == 0 || state.messages[index - 1].timestamp.toLocalDate() != day) {
                DaySeparator(day, state.today)
            }
            Bubble(message, fromMe = message.senderId == state.myUserId)
        }
    }
}

@Composable
private fun DaySeparator(day: LocalDate, today: LocalDate) {
    Box(Modifier.fillMaxWidth().padding(vertical = Spacing.sm), contentAlignment = Alignment.Center) {
        Text(
            text = when (day) {
                today -> stringResource(R.string.attendance_today)
                today.minusDays(1) -> stringResource(R.string.attendance_yesterday)
                else -> day.formatDate()
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = Spacing.md, vertical = Spacing.xs)
        )
    }
}

/** A message bubble: yours on the right in navy, theirs on the left in grey. */
@Composable
private fun Bubble(message: Message, fromMe: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val shape = if (fromMe) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = if (fromMe) Alignment.CenterEnd else Alignment.CenterStart) {
        Column(
            modifier = Modifier
                .widthIn(max = BUBBLE_MAX_WIDTH)
                .clip(shape)
                // The raised container, not the card grey: in dark mode the card
                // grey sits too close to the background to read as a bubble.
                .background(if (fromMe) scheme.primary else scheme.surfaceContainerHigh)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalAlignment = if (fromMe) Alignment.End else Alignment.Start
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = if (fromMe) scheme.onPrimary else scheme.onSurface
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = message.timestamp.formatTime(),
                style = MaterialTheme.typography.labelSmall,
                color = if (fromMe) scheme.onPrimary.copy(alpha = 0.8f) else scheme.onSurfaceVariant
            )
        }
    }
}

/** Quick replies for one-tap answers, then the text box and a 56dp send button. */
@Composable
private fun Composer(
    state: ChatUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickReply: (String) -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
        Column(
            Modifier
                // The scaffold already pads for the navigation bar; lift the
                // composer only by however much more the keyboard needs.
                .windowInsetsPadding(WindowInsets.ime.exclude(WindowInsets.navigationBars))
                .padding(vertical = Spacing.sm)
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                QUICK_REPLIES.forEach { res ->
                    val text = stringResource(res)
                    ChoiceChip(label = text, selected = false, onClick = { onQuickReply(text) }, icon = Icons.AutoMirrored.Filled.Reply)
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.draft,
                    onValueChange = onDraftChange,
                    placeholder = { Text(stringResource(R.string.chat_placeholder)) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(28.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(Spacing.sm))
                FilledIconButton(
                    onClick = onSend,
                    enabled = state.canSend,
                    modifier = Modifier.size(Spacing.minTouch),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send))
                    }
                }
            }
        }
    }
}

private val QUICK_REPLIES = listOf(
    R.string.chat_quick_on_my_way,
    R.string.chat_quick_done,
    R.string.chat_quick_need_help,
    R.string.chat_quick_running_late
)

private val BUBBLE_MAX_WIDTH: Dp = 320.dp

/** Room kept below the snackbar for the composer: quick replies, text box and padding. */
private val COMPOSER_HEIGHT: Dp = 140.dp

private fun previewState(): ChatUiState {
    val now = LocalDateTime.now()
    return ChatUiState(
        isLoading = false,
        participant = User(3, UserRole.MANAGER, "Johan", "Botha", "", "", "Site Foreman", ""),
        myUserId = 5,
        messages = listOf(
            Message(1, 3, 5, "Morning Sipho — are you on site yet?", now.minusMinutes(40)),
            Message(2, 5, 3, "Morning. Yes, on my way to Block C now.", now.minusMinutes(37)),
            Message(3, 3, 5, "Confirm the rebar spacing before you pour.", now.minusMinutes(20))
        ),
        draft = "Spacing is",
        today = now.toLocalDate()
    )
}

@DevicePreviews
@Composable
private fun ChatPreview() {
    PreviewSurface { ChatScreen(previewState(), false, {}, {}, {}, {}, {}) }
}
