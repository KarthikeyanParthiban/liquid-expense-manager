package com.expensemanager.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.TextPrimary

/**
 * High-performance native Jetpack Compose implementation of the Luma Spin loader.
 * Features 2 orbital stretching rounded pills with a 180° phase offset.
 */
@Composable
fun LumaSpinLoader(
    modifier: Modifier = Modifier,
    size: Dp = 65.dp,
    strokeWidth: Dp = 3.dp,
    color: Color = TextPrimary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lumaSpinTransition")
    val rawProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing)
        ),
        label = "lumaProgress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size.minDimension
            val strokePx = strokeWidth.toPx()
            val cornerRadiusPx = 50.dp.toPx()
            val shift = canvasSize * (35f / 65f)

            fun calculateBounds(progress: Float): Pair<Offset, Size> {
                val p = progress % 1f
                val top: Float
                val right: Float
                val bottom: Float
                val left: Float

                // Exact 8-phase interpolation of keyframes
                when {
                    p < 0.125f -> {
                        val t = p / 0.125f
                        top = 0f
                        right = shift
                        bottom = shift * (1f - t)
                        left = 0f
                    }
                    p < 0.250f -> {
                        val t = (p - 0.125f) / 0.125f
                        top = shift * t
                        right = shift
                        bottom = 0f
                        left = 0f
                    }
                    p < 0.375f -> {
                        val t = (p - 0.250f) / 0.125f
                        top = shift
                        right = shift * (1f - t)
                        bottom = 0f
                        left = 0f
                    }
                    p < 0.500f -> {
                        val t = (p - 0.375f) / 0.125f
                        top = shift
                        right = 0f
                        bottom = 0f
                        left = shift * t
                    }
                    p < 0.625f -> {
                        val t = (p - 0.500f) / 0.125f
                        top = shift * (1f - t)
                        right = 0f
                        bottom = 0f
                        left = shift
                    }
                    p < 0.750f -> {
                        val t = (p - 0.625f) / 0.125f
                        top = 0f
                        right = 0f
                        bottom = shift * t
                        left = shift
                    }
                    p < 0.875f -> {
                        val t = (p - 0.750f) / 0.125f
                        top = 0f
                        right = 0f
                        bottom = shift
                        left = shift * (1f - t)
                    }
                    else -> {
                        val t = (p - 0.875f) / 0.125f
                        top = 0f
                        right = shift * t
                        bottom = shift
                        left = 0f
                    }
                }

                val offset = Offset(left + strokePx / 2f, top + strokePx / 2f)
                val width = (canvasSize - left - right - strokePx).coerceAtLeast(strokePx)
                val height = (canvasSize - top - bottom - strokePx).coerceAtLeast(strokePx)
                return Pair(offset, Size(width, height))
            }

            // Pill 1
            val (offset1, size1) = calculateBounds(rawProgress)
            drawRoundRect(
                color = color,
                topLeft = offset1,
                size = size1,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Stroke(width = strokePx)
            )

            // Pill 2 (Phase offset by 50% / -1.25s)
            val (offset2, size2) = calculateBounds(rawProgress + 0.5f)
            drawRoundRect(
                color = color,
                topLeft = offset2,
                size = size2,
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Stroke(width = strokePx)
            )
        }
    }
}
