package com.mcc.construction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mcc.construction.data.remote.AppForeground
import com.mcc.construction.navigation.AppRoot
import com.mcc.construction.ui.adaptive.WindowWidthClass
import com.mcc.construction.ui.common.BackendFeedbackHost
import com.mcc.construction.ui.theme.MCCConstructionTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val forcedWidthClass = when (
            intent?.getStringExtra("forceWidthClass")?.lowercase()
        ) {
            "compact" -> WindowWidthClass.Compact
            "medium" -> WindowWidthClass.Medium
            "expanded" -> WindowWidthClass.Expanded
            else -> null
        }

        setContent {
            MCCConstructionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(forcedWidthClass = forcedWidthClass)
                    BackendFeedbackHost()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppForeground.setActive(true)
    }

    override fun onStop() {
        AppForeground.setActive(false)
        super.onStop()
    }
}