package com.expensemanager.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.expensemanager.app.ui.navigation.Screen
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.AppleBlueLight
import com.expensemanager.app.ui.theme.BorderLight
import com.expensemanager.app.ui.theme.LightCardSurfaceTranslucent
import com.expensemanager.app.ui.theme.TextTertiary

@Composable
fun GlassBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val screens = listOf(
        Screen.Dashboard,
        Screen.Transactions,
        Screen.Analytics,
        Screen.Accounts,
        Screen.Settings
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(CircleShape)
            .background(LightCardSurfaceTranslucent)
            .border(width = 1.dp, color = BorderLight, shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            screens.forEach { screen ->
                val isSelected = currentRoute == screen.route
                val activeTint by animateColorAsState(
                    targetValue = if (isSelected) AppleBlue else TextTertiary,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "bottomIconTint"
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(screen.route) }
                        .then(
                            if (isSelected) {
                                Modifier.background(AppleBlueLight)
                            } else Modifier
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        tint = activeTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
