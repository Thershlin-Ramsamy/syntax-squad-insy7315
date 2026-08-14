package com.mcc.construction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mcc.construction.data.TaskStatus
import com.mcc.construction.ui.theme.NavyBluePrimaryContainer
import com.mcc.construction.ui.theme.OnNavyBluePrimaryContainer
import com.mcc.construction.ui.theme.OnOrangeSecondaryContainer
import com.mcc.construction.ui.theme.OrangeSecondaryContainer
import com.mcc.construction.ui.theme.SuccessGreen

@Composable
fun StatusChip(status: TaskStatus, modifier: Modifier = Modifier) {
    val (container, content) = when (status) {
        TaskStatus.PENDING -> OrangeSecondaryContainer to OnOrangeSecondaryContainer
        TaskStatus.IN_PROGRESS -> NavyBluePrimaryContainer to OnNavyBluePrimaryContainer
        TaskStatus.COMPLETED -> SuccessGreen.copy(alpha = 0.16f) to SuccessGreen
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.label,
            color = content,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
