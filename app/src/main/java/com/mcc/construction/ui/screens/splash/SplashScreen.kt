package com.mcc.construction.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mcc.construction.R
import com.mcc.construction.navigation.Routes
import com.mcc.construction.ui.theme.BrandWhite
import com.mcc.construction.ui.theme.ConstructionGold
import com.mcc.construction.ui.theme.Ink500
import com.mcc.construction.ui.theme.NavyBlue
import com.mcc.construction.ui.theme.Spacing
import kotlinx.coroutines.delay

/**
 * Full-bleed brand moment. A determinate progress bar rather than a spinner —
 * a bar that visibly fills reads as progress toward something, where an
 * indefinite spinner reads as waiting.
 *
 * Colours here are pinned to the logo rather than taken from the colour scheme.
 * The official lockup is navy-and-gold artwork on transparency, so it needs a
 * light ground to read at all; letting dark mode flip this screen would put a
 * navy wordmark on a near-black field. A splash is the one screen where the
 * brand outranks the system theme.
 */
@Composable
fun SplashScreen(navController: NavHostController) {
    var started by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 1100),
        label = "splashProgress"
    )

    LaunchedEffect(Unit) {
        started = true
        delay(1400)
        navController.navigate(Routes.LOGIN) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.screenH),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.mcc_logo_full_lockup),
                // The lockup already says "MCC Construction", so the app name is
                // announced here and not repeated as a heading below.
                contentDescription = "MCC Construction",
                contentScale = ContentScale.Fit,
                // widthIn must come before fillMaxWidth: fillMaxWidth fixes the
                // width to whatever it is handed, so a cap placed after it never
                // gets a chance to apply and the lockup runs edge to edge.
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.xxl))
            Text(
                text = "Site Management System",
                style = MaterialTheme.typography.bodyLarge,
                color = Ink500,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.xxxl))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(140.dp)
                    .height(4.dp),
                color = ConstructionGold,
                trackColor = NavyBlue.copy(alpha = 0.14f)
            )
        }
    }
}
