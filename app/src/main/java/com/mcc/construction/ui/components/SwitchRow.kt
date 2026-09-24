package com.mcc.construction.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing
import com.mcc.construction.ui.theme.Tone

/**
 * An on/off setting. The whole row toggles, not just the switch — a 52dp-wide
 * switch is a small target for a gloved thumb, a full-width row is not.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tone: Tone = Tone.Neutral,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(MaterialTheme.shapes.small)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            LeadingIcon(icon, tone)
            Spacer(Modifier.width(Spacing.lg))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        // The row handles the toggle; the switch is only its visual state.
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SwitchRowPreview() {
    PreviewSurface {
        Column(Modifier.padding(Spacing.lg)) {
            SwitchRow(
                title = "Push notifications",
                subtitle = "Task assignments and safety notices",
                icon = Icons.Filled.Notifications,
                checked = true,
                onCheckedChange = {}
            )
            SwitchRow(title = "Vibrate", checked = false, onCheckedChange = {})
        }
    }
}
