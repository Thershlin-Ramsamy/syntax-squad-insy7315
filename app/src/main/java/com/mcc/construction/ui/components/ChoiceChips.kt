package com.mcc.construction.ui.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

/**
 * A large, glove-friendly choice: 48dp tall, icon and label, filled navy when
 * selected with a tick so the choice does not rely on colour alone. Use it
 * instead of a text field wherever the answer is one of a few options.
 */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(50)
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .heightIn(min = Spacing.touchTarget)
            .clip(shape)
            .background(if (selected) scheme.primary else scheme.surface)
            .border(1.dp, if (selected) scheme.primary else scheme.outline, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val tint = if (selected) scheme.onPrimary else scheme.onSurface
        when {
            selected -> Icon(Icons.Filled.Check, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            icon != null -> Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        if (selected || icon != null) Spacer(Modifier.width(Spacing.sm))
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}

/** A wrapping group of [ChoiceChip]s where exactly one (or none) is picked. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChipGroup(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    icon: ((T) -> ImageVector)? = null
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        options.forEach { option ->
            ChoiceChip(
                label = label(option),
                selected = option == selected,
                onClick = { onSelect(option) },
                icon = icon?.invoke(option)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ChoiceChipsPreview() {
    PreviewSurface {
        ChoiceChipGroup(
            options = listOf("Sunny", "Cloudy", "Rainy", "Windy"),
            selected = "Sunny",
            onSelect = {},
            label = { it },
            icon = { if (it == "Sunny") Icons.Filled.WbSunny else Icons.Filled.Cloud },
            modifier = Modifier.padding(Spacing.lg)
        )
    }
}
