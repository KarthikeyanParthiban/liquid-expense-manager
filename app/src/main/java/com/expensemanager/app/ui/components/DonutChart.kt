package com.expensemanager.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.data.local.dao.TransactionDao
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.AppleBlueLight
import com.expensemanager.app.ui.theme.BorderSubtle
import com.expensemanager.app.ui.theme.LightElevatedSurface
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.TextTertiary
import kotlin.math.atan2

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

    val activeItem = highlightedCategory
    val activeCategory = activeItem?.let {
        runCatching { Category.valueOf(it.category) }.getOrDefault(Category.OTHERS)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered Donut Ring with interactive center info
        Box(
            modifier = Modifier.size(chartSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(chartSize)
                    .pointerInput(spendingList, totalExpense) {
                        detectTapGestures { offset ->
                            if (spendingList.isEmpty() || totalExpense <= 0) return@detectTapGestures
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchVec = offset - center
                            var angle = Math.toDegrees(atan2(touchVec.y.toDouble(), touchVec.x.toDouble())).toFloat()
                            angle = (angle + 90f + 360f) % 360f // Normalize starting from top (-90 deg)

                            var currentAngle = 0f
                            for (item in spendingList) {
                                val sweep = ((item.totalAmount / totalExpense) * 360f).toFloat()
                                if (angle in currentAngle..(currentAngle + sweep)) {
                                    highlightedCategory = if (highlightedCategory == item) null else item
                                    val cat = runCatching { Category.valueOf(item.category) }.getOrDefault(Category.OTHERS)
                                    onCategorySelected?.invoke(cat)
                                    break
                                }
                                currentAngle += sweep
                            }
                        }
                    }
            ) {
                val strokeWidthPx = strokeWidth.toPx()
                val radius = (size.minDimension - strokeWidthPx) / 2f
                val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
                val arcSize = Size(radius * 2, radius * 2)

                if (spendingList.isEmpty() || totalExpense <= 0.0) {
                    drawArc(
                        color = Color(0xFFE5E7EB),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                } else {
                    var currentAngle = -90f
                    for (item in spendingList) {
                        val isHighlighted = highlightedCategory == item
                        val sweepAngle = ((item.totalAmount / totalExpense) * 360f).toFloat()
                        val cat = runCatching { Category.valueOf(item.category) }.getOrDefault(Category.OTHERS)
                        val color = Color(cat.colorHex)

                        drawArc(
                            color = if (highlightedCategory == null || isHighlighted) color else color.copy(alpha = 0.35f),
                            startAngle = currentAngle,
                            sweepAngle = (sweepAngle - 2f).coerceAtLeast(3f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = if (isHighlighted) strokeWidthPx * 1.2f else strokeWidthPx,
                                cap = StrokeCap.Round
                            )
                        )
                        currentAngle += sweepAngle
                    }
                }
            }

            // Center Content
            Box(
                modifier = Modifier
                    .size(chartSize - (strokeWidth * 2) - 14.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = activeCategory?.displayName ?: "TOTAL SPENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (activeCategory != null) Color(activeCategory.colorHex) else TextSecondary,
                        letterSpacing = 0.8.sp,
                        maxLines = 1
                    )
                    Text(
                        text = CurrencyFormatter.format(activeItem?.totalAmount ?: totalExpense),
                        fontSize = if (activeItem != null) 18.sp else 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    if (activeItem != null) {
                        val pct = if (totalExpense > 0) (activeItem.totalAmount / totalExpense * 100).toInt() else 0
                        Text(
                            text = "$pct% of total",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppleBlue
                        )
                    } else {
                        Text(
                            text = "${spendingList.sumOf { it.count }} txns",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Visual Segmented Proportional Bar
        if (spendingList.isNotEmpty() && totalExpense > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFE5E7EB))
            ) {
                for (item in spendingList) {
                    val weight = (item.totalAmount / totalExpense).toFloat().coerceAtLeast(0.01f)
                    val cat = runCatching { Category.valueOf(item.category) }.getOrDefault(Category.OTHERS)
                    val isHighlighted = highlightedCategory == item

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(10.dp)
                            .background(
                                if (highlightedCategory == null || isHighlighted) Color(cat.colorHex)
                                else Color(cat.colorHex).copy(alpha = 0.35f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explanatory Grid of Categories with percentage pills
            val topCategories = spendingList.take(6)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                topCategories.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { item ->
                            val cat = runCatching { Category.valueOf(item.category) }.getOrDefault(Category.OTHERS)
                            val pct = ((item.totalAmount / totalExpense) * 100).toInt()
                            val isSelected = highlightedCategory == item

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AppleBlueLight else Color(0xFFF9FAFB))
                                    .clickable {
                                        highlightedCategory = if (highlightedCategory == item) null else item
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(cat.colorHex))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = "$pct%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
