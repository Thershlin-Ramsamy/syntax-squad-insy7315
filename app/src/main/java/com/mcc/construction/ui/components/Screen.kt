package com.mcc.construction.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mcc.construction.ui.theme.Spacing

/**
 * Wrapper for a full-screen destination pushed onto the back stack (details,
 * forms, chat). Provides the back affordance and consistent gutters so no screen
 * has to re-derive its own padding.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
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
