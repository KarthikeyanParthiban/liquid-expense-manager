package com.expensemanager.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Clean Card and Surface Modifiers with titanium wireframe borders
fun Modifier.appleCard(
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 1.dp,
    containerColor: Color? = null,
    borderColor: Color? = null
): Modifier = composed {
    val finalContainer = containerColor ?: MaterialTheme.colorScheme.surface
    val finalBorder = borderColor ?: BorderLight

    val shadowMod = if (elevation > 0.dp) {
        Modifier.shadow(
            elevation = elevation,
            shape = shape,
            clip = false
        )
    } else Modifier

    this
        .then(shadowMod)
        .clip(shape)
        .background(finalContainer)
        .border(width = 1.dp, color = finalBorder, shape = shape)
}

fun Modifier.applePill(
    shape: Shape = RoundedCornerShape(14.dp),
    containerColor: Color? = null,
    borderColor: Color? = null
): Modifier = composed {
    val finalContainer = containerColor ?: MaterialTheme.colorScheme.surfaceVariant
    val finalBorder = borderColor ?: BorderLight

    this
        .clip(shape)
        .background(finalContainer)
        .border(width = 1.dp, color = finalBorder, shape = shape)
}

@Composable
fun CleanAppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}
