package com.mcc.construction.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mcc.construction.R
import com.mcc.construction.ui.preview.DevicePreviews
import com.mcc.construction.ui.preview.PreviewSurface
import com.mcc.construction.ui.theme.Spacing

/**
 * The standard frame for a screen. Use it for every new screen so gutters,
 * scrolling, the back button, the sync indicator and the width cap on wide
 * windows are the same everywhere.
 *
 * Two forms:
 *  - **Inside the navigation chrome** (a bottom-bar or rail destination): leave
 *    [title] null. The app bar already shows the title, so this adds only the
 *    content frame.
 *  - **Pushed on top** (a detail page, a form): pass [title] and [onBack]. This
 *    adds an app bar with a back button and the sync indicator.
 *
 * Content is capped at [maxContentWidth] and centred, so a settings list does
 * not stretch across a 1280dp desktop window. Pass [Dp.Unspecified] for
 * screens that lay out their own wide content, such as two-column dashboards.
 *
 * Set [scrollable] to false when the content is a `LazyColumn`, which scrolls
 * itself.
 */
@Composable
fun ScreenScaffold(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollable: Boolean = true,
    maxContentWidth: Dp = Spacing.contentMaxWidth,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    if (title == null) {
        ScreenBody(modifier, scrollable, maxContentWidth, content)
        return
    }
    Scaffold(
        modifier = modifier,
        topBar = { DetailTopBar(title = title, onBack = onBack, actions = actions) },
        bottomBar = bottomBar
    ) { padding ->
        ScreenBody(Modifier.padding(padding), scrollable, maxContentWidth, content)
    }
}

@Composable
private fun ScreenBody(
    modifier: Modifier,
    scrollable: Boolean,
    maxContentWidth: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val scroll = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(scroll),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = maxContentWidth)
                .fillMaxWidth()
                .then(if (scrollable) Modifier else Modifier.fillMaxSize())
                .padding(
                    start = Spacing.screenH,
                    end = Spacing.screenH,
                    top = Spacing.lg,
                    bottom = if (scrollable) Spacing.xxxl else 0.dp
                ),
            content = content
        )
    }
}

/**
 * App bar for a pushed screen: back button, title, then any [actions] and the
 * sync indicator, which is on every screen so connection state is never out of
 * sight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
        },
        actions = {
            actions()
            SyncStatusIndicator(Modifier.padding(end = Spacing.sm))
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Frame for a pushed screen whose content sets its own gutters with
 * [ScreenPadding]. Existing screens use this; new screens should prefer
 * [ScreenScaffold], which handles gutters and wide windows for you.
 */
@Composable
fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = { DetailTopBar(title = title, onBack = onBack, actions = actions) },
        bottomBar = bottomBar
    ) { padding ->
        val base = Modifier
            .fillMaxSize()
            .padding(padding)
        Column(
            modifier = if (scrollable) base.verticalScroll(rememberScrollState()) else base,
            content = content
        )
    }
}

/**
 * Standard page gutters. Screens compose their content inside this so the
 * left edge lines up across every destination in the app.
 */
@Composable
fun ScreenPadding(
    modifier: Modifier = Modifier,
    top: Dp = Spacing.lg,
    bottom: Dp = Spacing.xxxl,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.padding(
            start = Spacing.screenH,
            end = Spacing.screenH,
            top = top,
            bottom = bottom
        ),
        content = content
    )
}

@DevicePreviews
@Composable
private fun ScreenScaffoldPreview() {
    PreviewSurface {
        ScreenScaffold(title = "Material details", onBack = {}) {
            SectionHeader(title = "Stock")
            AppCard(modifier = Modifier.fillMaxWidth()) {
                KeyValueRow("On hand", "240 bags")
                KeyValueRow("Minimum", "100 bags")
            }
        }
    }
}
