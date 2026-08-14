package com.mcc.construction.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcc.construction.data.MockRepository
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.components.Avatar
import com.mcc.construction.ui.components.LeadingIcon
import com.mcc.construction.ui.components.ListRow
import com.mcc.construction.ui.components.TrailingStack
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

@Composable
fun MessagesScreen(navController: NavHostController) {
    val conversations = MockRepository.conversations

    LazyColumn(
        contentPadding = PaddingValues(
            start = Spacing.screenH,
            end = Spacing.screenH,
            top = Spacing.lg,
            bottom = Spacing.xxxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap),
        modifier = Modifier.fillMaxSize()
    ) {
        items(conversations, key = { it.id }) { conversation ->
            ListRow(
                title = conversation.name,
                subtitle = conversation.lastMessage,
                onClick = { navController.navigate(Routes.chat(conversation.id)) },
                leading = {
                    if (conversation.isGroup) {
                        LeadingIcon(Icons.Filled.Groups, Tone.Info)
                    } else {
                        Avatar(conversation.name)
                    }
                },
                trailing = {
                    TrailingStack {
                        Text(
                            text = conversation.timestamp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (conversation.unread > 0) {
                            Spacer(Modifier.height(Spacing.sm))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = conversation.unread.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

