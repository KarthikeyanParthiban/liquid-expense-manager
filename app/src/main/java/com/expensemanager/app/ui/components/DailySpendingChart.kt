package com.expensemanager.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.TextTertiary
import com.expensemanager.app.ui.theme.appleCard

data class DailySpend(
    val dayOfMonth: Int,
    val dateLabel: String,
    val amount: Double,
    val isPeakDay: Boolean = false
)

@Composable
fun DailySpendingChart(
    dailySpends: List<DailySpend>,
    modifier: Modifier = Modifier
) {
    if (dailySpends.isEmpty()) return

    var selectedDay by remember { mutableStateOf<DailySpend?>(null) }
    val maxAmount = (dailySpends.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(100.0)
    val totalMonthSpend = dailySpends.sumOf { it.amount }
    val avgDailySpend = if (dailySpends.isNotEmpty()) totalMonthSpend / dailySpends.size else 0.0
    val isDark = com.expensemanager.app.ui.theme.LocalIsDarkTheme.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .appleCard(shape = RoundedCornerShape(24.dp), elevation = 1.dp)
            .padding(20.dp)
    ) {
        Column {
            // Header with Selected / Peak info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (selectedDay != null) "SELECTED DAY" else "DAILY SPENDING TREND",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.1.sp
                    )
                    Text(
                        text = if (selectedDay != null) "${selectedDay!!.dateLabel}: ${CurrencyFormatter.format(selectedDay!!.amount)}"
                        else "Avg ${CurrencyFormatter.format(avgDailySpend)} / day",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (selectedDay != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Tap to reset",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Bar Chart in Monotone
            val barCount = dailySpends.size
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .pointerInput(dailySpends) {
                        detectTapGestures { offset ->
                            val barWidth = size.width / barCount
                            val index = (offset.x / barWidth).toInt().coerceIn(0, barCount - 1)
                            selectedDay = if (selectedDay == dailySpends[index]) null else dailySpends[index]
                        }
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val slotWidth = canvasWidth / barCount
                val barWidth = (slotWidth * 0.65f).coerceAtLeast(3f)

                // Draw average line
                val avgY = canvasHeight - ((avgDailySpend / maxAmount).toFloat() * canvasHeight)
                drawLine(
                    color = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0),
                    start = Offset(0f, avgY),
                    end = Offset(canvasWidth, avgY),
                    strokeWidth = 1.5f
                )

                dailySpends.forEachIndexed { index, spend ->
                    val isSelected = selectedDay == spend
                    val isPeak = spend.amount == maxAmount && spend.amount > 0
                    val barHeight = ((spend.amount / maxAmount).toFloat() * canvasHeight).coerceAtLeast(4f)
                    val left = (index * slotWidth) + ((slotWidth - barWidth) / 2f)
                    val top = canvasHeight - barHeight

                    val barColor = when {
                        isSelected -> if (isDark) Color.White else Color(0xFF171717)
                        isPeak -> if (isDark) Color(0xFFFFFFFF) else Color(0xFF212121)
                        spend.amount > avgDailySpend -> if (isDark) Color(0xFFB0B0B0) else Color(0xFF616161)
                        spend.amount > 0 -> if (isDark) Color(0xFF666666) else Color(0xFF9E9E9E)
                        else -> if (isDark) Color(0xFF1F1F1F) else Color(0xFFE5E7EB)
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom X-axis labels (1st, 10th, 20th, End of month)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "1st", fontSize = 11.sp, color = TextTertiary)
                if (barCount >= 10) Text(text = "10th", fontSize = 11.sp, color = TextTertiary)
                if (barCount >= 20) Text(text = "20th", fontSize = 11.sp, color = TextTertiary)
                Text(text = "${barCount}th", fontSize = 11.sp, color = TextTertiary)
            }
        }
    }
}
