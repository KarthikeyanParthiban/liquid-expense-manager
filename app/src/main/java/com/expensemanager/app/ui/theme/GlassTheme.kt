package com.expensemanager.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Clean Apple iOS Card and Surface Modifiers
fun Modifier.appleCard(
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 2.dp,
    containerColor: Color = LightCardSurface,
    borderColor: Color = BorderLight
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.04f),
        spotColor = Color.Black.copy(alpha = 0.06f)
    )
    .clip(shape)
    .background(containerColor)
    .border(width = 1.dp, color = borderColor, shape = shape)

fun Modifier.applePill(
    shape: Shape = RoundedCornerShape(14.dp),
    containerColor: Color = LightElevatedSurface,
    borderColor: Color = BorderSubtle
): Modifier = this
    .clip(shape)
    .background(containerColor)
    .border(width = 1.dp, color = borderColor, shape = shape)

@Composable
fun CleanAppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LightBg)
    ) {
        content()
    }
}
