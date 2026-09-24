package com.mcc.construction.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mcc.construction.R
import com.mcc.construction.data.model.Conversation
import com.mcc.construction.data.model.Message
import com.mcc.construction.data.model.User
import com.mcc.construction.data.model.UserRole
import com.mcc.construction.ui.adaptive.LocalWindowLayout
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.EmptyState
import com.mcc.construction.ui.components.LoadingState
import com.mcc.construction.ui.components.ScreenScaffold
import com.mcc.construction.ui.format.relativeTime
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import java.time.LocalDateTime

/**
 * Chat list wired to its ViewModel. On a phone a tap opens the chat screen; on
 * a wide window the chat opens in [chatPane] beside the list.
 */
@Composable
fun MessagesRoute(
    onOpenChat: (userId: Int) -> Unit,
    chatPane: (@Composable (userId: Int) -> Unit)? = null,
    viewModel: MessagesViewModel = viewModel(factory = MessagesViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val twoPane = LocalWindowLayout.current.isExpanded && chatPane != null
    MessagesScreen(
        state = state,
        onConversationClick = { id -> if (twoPane) viewModel.onSelectConversation(id) else onOpenChat(id) },
        chatPane = if (twoPane) chatPane else null
    )
}

@Composable
fun MessagesScreen(
    state: MessagesUiState,
    onConversationClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    chatPane: (@Composable (userId: Int) -> Unit)? = null
) {
    val conversations = state.conversations
    if (conversations == null) {
        LoadingState(modifier)
        return
    }
    if (chatPane == null) {
        ScreenScaffold(modifier = modifier, scrollable = false) {
            ConversationList(state, conversations, onConversationClick, highlight = null)
        }
        return
    }
    ScreenScaffold(modifier = modifier, scrollable = false, maxContentWidth = Dp.Unspecified) {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(LIST_WEIGHT)) {
                ConversationList(state, conversations, onConversationClick, highlight = state.paneUserId)
            }
            VerticalDivider(Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.weight(1f - LIST_WEIGHT)) {
                state.paneUserId?.let { chatPane(it) }
            }
        }
    }
}

@Composable
private fun ConversationList(
    state: MessagesUiState,
    conversations: List<Conversation>,
    onConversationClick: (Int) -> Unit,
    highlight: Int?
) {
    if (conversations.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = stringResource(R.string.messages_empty_title),
            message = stringResource(R.string.messages_empty_message)
        )
        return
    }
    if (state.totalUnread > 0) {
        Text(
            text = pluralStringResource(R.plurals.messages_unread_total, state.totalUnread, state.totalUnread),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(Spacing.blockGap))
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.xxxl, end = if (highlight != null) Spacing.lg else 0.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        items(conversations, key = { it.participant.userId }) { conversation ->
            ConversationRow(
                conversation = conversation,
                sentByMe = conversation.lastMessage.senderId == state.myUserId,
                selected = conversation.participant.userId == highlight,
                onClick = { onConversationClick(conversation.participant.userId) }
            )
        }
    }
}

/** A conversation: who, their last message, when, and an unread badge. Unread rows are bold. */
@Composable
private fun ConversationRow(
    conversation: Conversation,
    sentByMe: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val unread = conversation.unreadCount > 0
    val shape = MaterialTheme.shapes.medium
    val unreadDescription = pluralStringResource(R.plurals.messages_unread_count, conversation.unreadCount, conversation.unreadCount)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.rowHeight)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { if (unread) stateDescription = unreadDescription }
            .padding(horizontal = Spacing.cardPad, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(conversation.participant.fullName, size = 48.dp)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.participant.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = relativeTime(conversation.lastMessage.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(Spacing.xxs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val preview = if (sentByMe) {
                    stringResource(R.string.messages_you_prefix, conversation.lastMessage.content)
                } else {
                    conversation.lastMessage.content
                }
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (unread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (unread) {
                    Spacer(Modifier.width(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

private const val LIST_WEIGHT = 0.4f

private fun previewState(): MessagesUiState {
    val now = LocalDateTime.now()
    fun user(id: Int, first: String, last: String) = User(id, UserRole.MANAGER, first, last, "", "", "Site Foreman", "")
    return MessagesUiState(
        conversations = listOf(
            Conversation(user(3, "Johan", "Botha"), Message(4, 3, 5, "Confirm the rebar spacing before you pour.", now.minusMinutes(18)), 2),
            Conversation(user(6, "Thabo", "Mokoena"), Message(10, 6, 5, "Cement for Block B is staged at the lay-down area.", now.minusMinutes(75)), 1),
            Conversation(user(2, "Naledi", "Khumalo"), Message(6, 5, 2, "Thank you.", now.minusDays(1)), 0)
        ),
        myUserId = 5
    )
}

@DevicePreviews
@Composable
private fun MessagesPreview() {
    PreviewSurface { MessagesScreen(previewState(), {}) }
}
