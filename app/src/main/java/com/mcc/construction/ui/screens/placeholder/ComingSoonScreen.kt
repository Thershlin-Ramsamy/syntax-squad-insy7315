package com.mcc.construction.ui.screens.placeholder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.mcc.construction.ui.components.EmptyState

/** Stand-in for a destination scheduled for a later batch. */
@Composable
fun ComingSoonScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Filled.Construction,
            title = title,
            message = "This screen is scheduled for a later build."
        )
    }
}
