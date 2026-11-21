package com.termux.ilynxos.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.termux.ilynxos.ui.components.WarmBackgroundScaffold
import com.termux.ilynxos.ui.theme.OrangeAccentBg
import com.termux.ilynxos.ui.theme.OrangeAccentBorder
import com.termux.ilynxos.ui.theme.OrangePrimary
import com.termux.ilynxos.ui.theme.TextDarkPrimary
import com.termux.ilynxos.ui.theme.TextDarkSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    isInstalled: Boolean,
    onNavigateNext: (Boolean) -> Unit
) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
        delay(600)
        onNavigateNext(isInstalled)
    }

    WarmBackgroundScaffold(showWave = true) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
            ) {
                // White & Orange Hero Logo
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(OrangePrimary, RoundedCornerShape(26.dp))
                        .border(2.5.dp, Color.White, RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DesktopWindows,
                        contentDescription = "i-Lynx OS Logo",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "i-Lynx OS",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextDarkPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Version 1.03 (Noble Modern Edition)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OrangePrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Developer: Smruti Ranjan Sahoo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDarkSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
