package com.mcc.construction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import com.mcc.construction.navigation.AppRoot
import com.mcc.construction.ui.theme.MCCConstructionTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate(). Swaps the launch theme out for the
        // app theme once the first frame is ready, and back-ports the Android 12
        // splash to the API 24 minimum so the brand moment is the same
        // everywhere rather than only on new devices.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Optional launch-time override of the width size class, used to capture
        // the expanded layout on an emulator that will not genuinely resize:
        //
        //   adb shell am start -n com.mcc.construction/.MainActivity \
        //       --es forceWidthClass expanded
        //
        // Absent the extra the app uses the real measured window, so normal runs
        // are unaffected and the adaptive behaviour is still driven by the window.
        val forced = when (intent?.getStringExtra(EXTRA_FORCE_WIDTH_CLASS)?.lowercase()) {
            "compact" -> WindowWidthSizeClass.Compact
            "medium" -> WindowWidthSizeClass.Medium
            "expanded" -> WindowWidthSizeClass.Expanded
            else -> null
        }

        setContent {
            val measured = calculateWindowSizeClass(this).widthSizeClass
            MCCConstructionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(widthSizeClass = forced ?: measured)
                }
            }
        }
    }

    private companion object {
        const val EXTRA_FORCE_WIDTH_CLASS = "forceWidthClass"
    }
}
