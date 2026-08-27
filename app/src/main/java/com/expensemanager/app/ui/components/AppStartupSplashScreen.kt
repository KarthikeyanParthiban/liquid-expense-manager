package com.expensemanager.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.ui.theme.DarkBg
import com.expensemanager.app.ui.theme.LightBgConstant
import com.expensemanager.app.ui.theme.LocalIsDarkTheme
import com.expensemanager.app.ui.theme.TextPrimaryConstant
import com.expensemanager.app.ui.theme.TextSecondaryConstant
import kotlinx.coroutines.delay

private val AppleSpringEase = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

/**
 * App Startup Screen featuring the Luma Spin loader with a liquid smooth swipe-up reveal transition.
 * Automatically adapts background, loader, and typography colors to Dark, Light, or System settings.
 */
@Composable
fun AppStartupSplashScreen(
    durationMillis: Long = 1600L,
    content: @Composable () -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }
    val isDark = LocalIsDarkTheme.current

    val splashBg = if (isDark) DarkBg else LightBgConstant
    val loaderColor = if (isDark) Color.White else TextPrimaryConstant
    val titleColor = if (isDark) Color.White else TextPrimaryConstant
    val subtitleColor = if (isDark) Color(0xFFA3A3A3) else TextSecondaryConstant

    val contentScale by animateFloatAsState(
        targetValue = if (showSplash) 0.94f else 1f,
        animationSpec = tween(durationMillis = 750, easing = AppleSpringEase),
        label = "contentScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (showSplash) 0.75f else 1f,
        animationSpec = tween(durationMillis = 700, easing = AppleSpringEase),
        label = "contentAlpha"
    )

    LaunchedEffect(Unit) {
        delay(durationMillis)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main App Content (Home / Dashboard Navigation) with subtle scaling entrance
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(contentScale)
                .alpha(contentAlpha)
        ) {
            content()
        }

        // Startup Screen Overlay with Liquid Smooth Swipe-Up Reveal
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 750, easing = AppleSpringEase)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 24.dp)
                    .background(splashBg),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LumaSpinLoader(
                        size = 72.dp,
                        strokeWidth = 3.5.dp,
                        color = loaderColor
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Component 1: LQD Branding (L & D primary, Q Royal Electric Blue)
                    LqdBrandWordmark(
                        textColor = titleColor,
                        accentColor = Color(0xFF0055FF),
                        fontSize = 38.sp
                    )
                }
            }
        }
    }
}

/**
 * Component 1: Official LQD Brand Wordmark
 * Renders L and D in primary adaptive color and Q in Royal Electric Blue (#0055FF).
 */
@Composable
fun LqdBrandWordmark(
    modifier: Modifier = Modifier,
    textColor: Color,
    accentColor: Color = Color(0xFF0055FF),
    fontSize: androidx.compose.ui.unit.TextUnit = 38.sp
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "L",
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            letterSpacing = 0.sp
        )
        Text(
            text = "Q",
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor,
            letterSpacing = 0.sp
        )
        Text(
            text = "D",
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            letterSpacing = 0.sp
        )
    }
}


