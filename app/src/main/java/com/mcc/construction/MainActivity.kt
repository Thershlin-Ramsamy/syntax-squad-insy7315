package com.mcc.construction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mcc.construction.di.SupabaseAppContainer
import com.mcc.construction.navigation.AppRoot
import com.mcc.construction.ui.adaptive.WindowWidthClass
import com.mcc.construction.ui.common.BackendFeedbackHost
import com.mcc.construction.ui.common.BackendReadStatus
import com.mcc.construction.ui.theme.MCCConstructionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val forcedWidthClass = when (
            intent?.getStringExtra(EXTRA_FORCE_WIDTH_CLASS)?.lowercase()
        ) {
            "compact" -> WindowWidthClass.Compact
            "medium" -> WindowWidthClass.Medium
            "expanded" -> WindowWidthClass.Expanded
            else -> null
        }

        setContent {
            MCCConstructionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column {
                        val mccApplication = application as MccApplication

                        if (!mccApplication.isBackendConfigured) {
                            Text(
                                text = stringResource(
                                    R.string.backend_setup_required
                                ),
                                modifier = Modifier.padding(24.dp)
                            )
                        } else {
                            val container = mccApplication.container

                            if (container is SupabaseAppContainer) {
                                BackendReadStatus(container.backendReadFailed)
                            }

                            Box(Modifier.weight(1f)) {
                                AppRoot(
                                    forcedWidthClass = forcedWidthClass
                                )
                            }
                        }
                    }

                    BackendFeedbackHost()
                }
            }
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()

        val mccApplication = application as MccApplication

        if (mccApplication.isBackendConfigured) {
            mccApplication.container.recordUserInteraction()
        }
    }

    private companion object {
        const val EXTRA_FORCE_WIDTH_CLASS = "forceWidthClass"
    }
}