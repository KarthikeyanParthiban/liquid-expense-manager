package com.expensemanager.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.data.local.dao.TransactionDao
import com.expensemanager.app.ui.theme.LightElevatedSurface
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.TextTertiary

@Composable
fun DonutChart(
    spendingList: List<TransactionDao.CategorySpending>,
    totalExpense: Double,
    modifier: Modifier = Modifier,
    chartSize: Dp = 200.dp,
    strokeWidth: Dp = 20.dp,
    onCategorySelected: ((Category) -> Unit)? = null
) {
    var highlightedCategory by remember { mutableStateOf<TransactionDao.CategorySpending?>(null) }
    val isDark = com.expensemanager.app.ui.theme.LocalIsDarkTheme.current

    val monoShades = if (isDark) listOf(
        Color(0xFFFFFFFF), Color(0xFFE0E0E0), Color(0xFFBDBDBD), Color(0xFF9E9E9E), Color(0xFF757575), Color(0xFF616161)
    ) else listOf(
        Color(0xFF171717), Color(0xFF424242), Color(0xFF616161), Color(0xFF757575), Color(0xFF9E9E9E), Color(0xFFBDBDBD)
    )

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(spendingList, totalExpense) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    if (spendingList.isEmpty() || totalExpense <= 0.0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No spending recorded for this period",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Proportional Segmented Monochrome Ribbon Bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPENDING DISTRIBUTION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${spendingList.size} categories",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multi-segment horizontal progress ribbon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    spendingList.forEachIndexed { idx, item ->
                        val cat = runCatching { Category.valueOf(item.category) }.getOrDefault(Category.OTHERS)
                        val sliceColor = monoShades[idx % monoShades.size]
                        val weight = (item.totalAmount / totalExpense).toFloat().coerceAtLeast(0.01f)
                        val isHighlighted = highlightedCategory == item

                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxHeight()
                                .padding(horizontal = 0.75.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (highlightedCategory == null || isHighlighted) sliceColor else sliceColor.copy(alpha = 0.35f)
                                )
                                .clickable {
                                    highlightedCategory = if (highlightedCategory == item) null else item
                                    onCategorySelected?.invoke(cat)
                                }
                        )
                    }
                }
            }
        }

        // 2. Ranked Category Breakdown Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            spendingList.forEachIndexed { index, item ->
                val cat = runCatching { Category.valueOf(item.category) }.getOrDefault(Category.OTHERS)
                val sliceColor = monoShades[index % monoShades.size]
                val fraction = if (totalExpense > 0) (item.totalAmount / totalExpense).toFloat() else 0f
                val percentage = (fraction * 100).toInt()
                val isHighlighted = highlightedCategory == item

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val itemScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.98f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "categoryRowScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = itemScale
                            scaleY = itemScale
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isHighlighted) MaterialTheme.colorScheme.surfaceVariant else LightElevatedSurface)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            highlightedCategory = if (highlightedCategory == item) null else item
                            onCategorySelected?.invoke(cat)
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIcon(category = cat, size = 36.dp, iconSize = 18.dp)

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = cat.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${item.count} transaction${if (item.count > 1) "s" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextTertiary
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.format(item.totalAmount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "$percentage% of spend",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Drill Down",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Individual Category Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction * animationProgress.value)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(sliceColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
