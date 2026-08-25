package com.expensemanager.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.ui.theme.LocalIsDarkTheme
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.TextTertiary
import com.expensemanager.app.ui.theme.appleCard

data class CashFlowPeriod(
    val label: String,
    val dateRange: String,
    val income: Double,
    val spend: Double
) {
    val net: Double get() = income - spend
}

@Composable
fun IncomeSpendBarChartCard(
    cashFlowPeriods: List<CashFlowPeriod>,
    modifier: Modifier = Modifier,
    onNavigateToInsights: (() -> Unit)? = null
) {
    if (cashFlowPeriods.isEmpty()) return

    val isDark = LocalIsDarkTheme.current
    var selectedIndex by remember { mutableStateOf<Int?>(cashFlowPeriods.lastIndex) }
    var animationTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(cashFlowPeriods) {
        animationTriggered = true
        selectedIndex = cashFlowPeriods.lastIndex
    }

    val total7DayIncome = remember(cashFlowPeriods) { cashFlowPeriods.sumOf { it.income } }
    val total7DaySpend = remember(cashFlowPeriods) { cashFlowPeriods.sumOf { it.spend } }
    val avgDailySpend = if (cashFlowPeriods.isNotEmpty()) total7DaySpend / cashFlowPeriods.size else 0.0

    val maxVal = remember(cashFlowPeriods) {
        val highest = cashFlowPeriods.maxOfOrNull { maxOf(it.income, it.spend) } ?: 100.0
        if (highest <= 0.0) 100.0 else highest
    }

    // Monotone Apple / Insights Theme Palette (Dropping all red/green)
    val incomeColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF171717)
    val spendColor = if (isDark) Color(0xFF545458) else Color(0xFF8E8E93)
    val activeSpendColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF545458)
    val avgLineColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E7EB)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .appleCard(shape = RoundedCornerShape(24.dp), elevation = 1.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onNavigateToInsights?.invoke()
            }
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Title & Headline + Insights Navigation Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LAST 7 DAYS TREND",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Avg ${CurrencyFormatter.format(avgDailySpend)} / day",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                // Insights Link Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Insights",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Go to Insights",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Monotone Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(incomeColor)
                    )
                    Text(
                        text = "Income (${CurrencyFormatter.format(total7DayIncome)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(spendColor)
                    )
                    Text(
                        text = "Spend (${CurrencyFormatter.format(total7DaySpend)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart Grid (Height 100.dp, perfectly proportioned for 7 columns)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                // Average Baseline Line
                Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    val canvasHeight = size.height
                    val avgY = (canvasHeight - ((avgDailySpend / maxVal).toFloat() * canvasHeight)).coerceIn(8f, canvasHeight - 8f)
                    drawLine(
                        color = avgLineColor,
                        start = Offset(0f, avgY),
                        end = Offset(size.width, avgY),
                        strokeWidth = 1f
                    )
                }

                // 7 Days Columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    cashFlowPeriods.forEachIndexed { index, period ->
                        val isSelected = selectedIndex == index

                        val incomeFraction = (period.income / maxVal).toFloat().coerceIn(0.03f, 1f)
                        val spendFraction = (period.spend / maxVal).toFloat().coerceIn(0.03f, 1f)

                        val animatedIncome by animateFloatAsState(
                            targetValue = if (animationTriggered) incomeFraction else 0f,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                            label = "inc_bar_$index"
                        )

                        val animatedSpend by animateFloatAsState(
                            targetValue = if (animationTriggered) spendFraction else 0f,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                            label = "spd_bar_$index"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedIndex = if (selectedIndex == index) null else index
                                }
                        ) {
                            // Bar Pair Track
                            Row(
                                modifier = Modifier
                                    .height(78.dp)
                                    .padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Income Bar (White)
                                Box(
                                    modifier = Modifier
                                        .width(9.dp)
                                        .fillMaxHeight(animatedIncome)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 1.dp, bottomEnd = 1.dp))
                                        .background(incomeColor)
                                )

                                Spacer(modifier = Modifier.width(3.dp))

                                // Spend Bar (Titanium / Slate)
                                Box(
                                    modifier = Modifier
                                        .width(9.dp)
                                        .fillMaxHeight(animatedSpend)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 1.dp, bottomEnd = 1.dp))
                                        .background(if (isSelected) activeSpendColor else spendColor)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Day Label
                            Text(
                                text = period.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TextPrimary else TextTertiary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Day Drilldown Strip
            val selectedPeriod = selectedIndex?.let { idx -> cashFlowPeriods.getOrNull(idx) }
            if (selectedPeriod != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedPeriod.label} (${selectedPeriod.dateRange})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "In: ${CurrencyFormatter.format(selectedPeriod.income)}  •  Out: ${CurrencyFormatter.format(selectedPeriod.spend)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
