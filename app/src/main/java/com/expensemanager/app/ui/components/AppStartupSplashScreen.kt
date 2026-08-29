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

import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.expensemanager.app.R

private val AppleSpringEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * App Startup Screen featuring the Luma Spin loader with a liquid smooth gradient swipe-up reveal transition.
 * Automatically adapts background, loader, and typography colors to Dark, Light, or System settings.
 */
@Composable
fun AppStartupSplashScreen(
    loopDurationMillis: Int = 2200,
    content: @Composable () -> Unit
) {
    var showSplash by remember { mutableStateOf(true) }
    val isDark = LocalIsDarkTheme.current

    val splashGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                DarkBg,
                Color(0xFF0A0A0A),
                Color(0xFF121212),
                Color(0xFF080808)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                LightBgConstant,
                Color(0xFFF7F8FA),
                Color(0xFFFFFFFF),
                Color(0xFFF2F4F7)
            )
        )
    }

    val loaderColor = if (isDark) Color.White else TextPrimaryConstant

    val contentScale by animateFloatAsState(
        targetValue = if (showSplash) 0.92f else 1f,
        animationSpec = tween(durationMillis = 850, easing = AppleSpringEase),
        label = "contentScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (showSplash) 0.5f else 1f,
        animationSpec = tween(durationMillis = 800, easing = AppleSpringEase),
        label = "contentAlpha"
    )

    val progressAnim = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(Unit) {
        // Complete the exact full 360 degree orbital loop before transitioning to homescreen
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = loopDurationMillis,
                easing = LinearEasing
            )
        )
        // Smoothly dismiss splash only after the full loop has reached 100% completion
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

        // Startup Screen Overlay with Liquid Smooth Gradient Swipe-Up Reveal
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(animationSpec = tween(350)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 850, easing = AppleSpringEase)
            ) + fadeOut(
                animationSpec = tween(durationMillis = 800, easing = LinearEasing),
                targetAlpha = 0.05f
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 32.dp)
                    .background(splashGradient)
            ) {
                // Centered Loader & Official Logo
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    LumaSpinLoader(
                        size = 72.dp,
                        strokeWidth = 3.5.dp,
                        color = loaderColor,
                        progress = progressAnim.value,
                        cycleDurationMillis = loopDurationMillis
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Official LQD Geometric Brand Logo
                    LqdLogo(
                        isDark = isDark,
                        modifier = Modifier.height(46.dp)
                    )
                }

                // Bottom trailing gradient feather for the swipe-up reveal edge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    if (isDark) Color(0x33333333) else Color(0x15000000)
                                )
                            )
                        )
                )
            }
        }
    }
}

/**
 * Official LQD Geometric Vector Brand Logo.
 * Renders L and D in adaptive brand color (white on dark / #18181A on light) and Q in brand grey (#8C9198).
 * Preserves the exact 1187:536 aspect ratio.
 */
@Composable
fun LqdLogo(
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalIsDarkTheme.current
) {
    val drawableRes = if (isDark) R.drawable.ic_lqd_logo_reversed else R.drawable.ic_lqd_logo
    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = "LQD Logo",
        modifier = modifier
            .width((46 * (1187f / 536f)).dp),
        contentScale = ContentScale.Fit
    )
}

/**
 * Compatibility alias for LqdLogo
 */
@Composable
fun LqdBrandWordmark(
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    accentColor: Color = Color(0xFF8C9198),
    fontSize: androidx.compose.ui.unit.TextUnit = 38.sp
) {
    LqdLogo(modifier = modifier)
}


