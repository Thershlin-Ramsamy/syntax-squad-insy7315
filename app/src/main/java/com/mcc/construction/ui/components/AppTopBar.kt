package com.mcc.construction.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mcc.construction.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(currentRoute: String?) {
    val title = when (currentRoute) {
        Routes.HOME -> "Dashboard"
        Routes.TASKS -> "My Tasks"
        Routes.PROJECTS -> "Projects"
        Routes.MESSAGES -> "Messages"
        Routes.PROFILE -> "Profile"
        else -> "MCC Construction"
    }
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        actions = { OnlineStatusIndicator(modifier = Modifier.padding(end = 12.dp)) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
