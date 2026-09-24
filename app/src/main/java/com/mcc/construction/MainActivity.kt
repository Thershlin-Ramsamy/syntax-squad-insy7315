package com.mcc.construction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mcc.construction.navigation.AppRoot
import com.mcc.construction.ui.adaptive.WindowWidthClass
import com.mcc.construction.ui.theme.MCCConstructionTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Optional launch-time override of the width class, used to capture the
        // expanded layout on an emulator that will not genuinely resize:
        //
        //   adb shell am start -n com.mcc.construction/.MainActivity \
        //       --es forceWidthClass expanded
        //
        // Without the extra the app uses the real measured window, so normal
        // runs are unaffected and the layout still follows the window live.
        val forcedWidthClass = when (intent?.getStringExtra(EXTRA_FORCE_WIDTH_CLASS)?.lowercase()) {
            "compact" -> WindowWidthClass.Compact
            "medium" -> WindowWidthClass.Medium
            "expanded" -> WindowWidthClass.Expanded
            else -> null
        }

        setContent {
            MCCConstructionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(forcedWidthClass = forcedWidthClass)
                }
            }
        }
    }

    private companion object {
        const val EXTRA_FORCE_WIDTH_CLASS = "forceWidthClass"
    }
}
