package com.mcc.construction.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mcc.construction.R
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing

/**
 * A list with a detail pane beside it, for wide windows. The list keeps its
 * filters and scroll position while the pane shows whichever item is picked,
 * so a manager can work down a list without going back and forth.
 *
 * Only use this when the window is expanded; on a phone, list screens push the
 * detail screen instead. When nothing is picked, [detail] receives null and
 * the pane shows [emptyTitle] and [emptyMessage].
 */
@Composable
fun <T : Any> ListDetailLayout(
    selected: T?,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    listWeight: Float = LIST_WEIGHT,
    list: @Composable ColumnScope.() -> Unit,
    detail: @Composable (T) -> Unit
) {
    ScreenScaffold(modifier = modifier, scrollable = false, maxContentWidth = Dp.Unspecified) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(listWeight)
                    .padding(end = Spacing.lg),
                content = list
            )
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Box(Modifier.weight(1f - listWeight)) {
                if (selected != null) {
                    detail(selected)
                } else {
                    EmptyState(icon = emptyIcon, title = emptyTitle, message = emptyMessage)
                }
            }
        }
    }
}

/**
 * Outlines a row or card in the brand navy when it is the item open in a
 * side-by-side detail pane. Pass it as the row's modifier.
 */
@Composable
fun Modifier.selectedOutline(selected: Boolean): Modifier =
    if (selected) {
        border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
    } else {
        this
    }

/** A search box for filtering a list by name as you type. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    AppTextField(
        value = query,
        onValueChange = onQueryChange,
        label = label,
        modifier = modifier,
        leadingIcon = Icons.Filled.Search,
        imeAction = ImeAction.Search,
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear_search))
                }
            }
        }
    )
}

private const val LIST_WEIGHT = 0.42f

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun SearchFieldPreview() {
    PreviewSurface {
        SearchField(query = "Sipho", onQueryChange = {}, label = "Search by name", modifier = Modifier.padding(Spacing.lg))
    }
}
