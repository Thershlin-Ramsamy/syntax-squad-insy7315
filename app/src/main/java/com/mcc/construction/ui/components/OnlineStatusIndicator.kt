package com.mcc.construction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mcc.construction.data.MockRepository
import com.mcc.construction.ui.theme.DangerRed
import com.mcc.construction.ui.theme.SuccessGreen

/**
 * Offline-first apps must always surface connectivity/sync state (CLAUDE.md design
 * notes). Tappable here purely so the offline path can be demoed without a real
 * network layer.
 */
@Composable
fun OnlineStatusIndicator(modifier: Modifier = Modifier) {
    val isOnline by MockRepository.isOnline

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { MockRepository.toggleOnline() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (isOnline) SuccessGreen else DangerRed, CircleShape)
        )
        Text(
            text = if (isOnline) "Online" else "Offline",
            style = MaterialTheme.typography.labelMedium
        )
    }
}
