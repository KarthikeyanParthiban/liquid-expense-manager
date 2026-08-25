package com.expensemanager.app.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.core.util.DateTimeUtils
import com.expensemanager.app.ui.components.CategoryDetailSheet
import com.expensemanager.app.ui.components.CategoryIcon
import com.expensemanager.app.ui.components.DailySpendingChart
import com.expensemanager.app.ui.components.DonutChart
import com.expensemanager.app.ui.components.MerchantDetailSheet
import com.expensemanager.app.ui.components.PaymentModeSplitCard
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.BorderSubtle
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.TextTertiary
import com.expensemanager.app.ui.theme.appleCard

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onTransactionClick: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val monthTimestamp by viewModel.selectedMonthTimestamp.collectAsState()
    val summary by viewModel.monthlySummary.collectAsState()
    val categorySpending by viewModel.categorySpending.collectAsState()
    val topMerchants by viewModel.topMerchants.collectAsState()
    val dailyTrends by viewModel.dailySpendingTrends.collectAsState()
    val paymentSplit by viewModel.paymentModeSplit.collectAsState()
    val monthTxns by viewModel.monthTransactions.collectAsState()

    val selectedCategoryForDrilldown by viewModel.selectedCategoryForDrilldown.collectAsState()
    val selectedMerchantForDrilldown by viewModel.selectedMerchantForDrilldown.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp)
        ) {
            // Month Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Spending Insights",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.previousMonth() }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = TextSecondary)
                            }
                            Text(
                                text = DateTimeUtils.formatMonthYear(monthTimestamp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppleBlue
                            )
                            IconButton(onClick = { viewModel.nextMonth() }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Daily Spending Trend Chart
            item {
                Spacer(modifier = Modifier.height(6.dp))
                DailySpendingChart(dailySpends = dailyTrends)
            }

            // Payment Mode Split Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                PaymentModeSplitCard(split = paymentSplit)
            }

            // Category Donut Chart
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .appleCard(shape = RoundedCornerShape(24.dp), elevation = 1.dp)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "CATEGORY BREAKDOWN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        DonutChart(
                            spendingList = categorySpending,
                            totalExpense = summary.totalExpense,
                            onCategorySelected = { cat -> viewModel.selectCategoryForDrilldown(cat) }
                        )
                    }
                }
            }

            // Interactive Category List Header
            if (categorySpending.isNotEmpty()) {
                item {
                    Text(
                        text = "CATEGORIES (TAP TO DRILL DOWN)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.1.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                items(categorySpending, key = { it.category }) { item ->
                    val catEnum = runCatching { Category.valueOf(item.category) }.getOrDefault(Category.OTHERS)
                    val percentage = if (summary.totalExpense > 0) {
                        (item.totalAmount / summary.totalExpense * 100).toInt()
                    } else 0

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 3.dp)
                            .appleCard(shape = RoundedCornerShape(16.dp), elevation = 0.5.dp)
                            .clickable { viewModel.selectCategoryForDrilldown(catEnum) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIcon(category = catEnum, size = 38.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = catEnum.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${item.count} transactions • $percentage%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Text(
                                text = CurrencyFormatter.format(item.totalAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Interactive Top Merchants Leaderboard
            if (topMerchants.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "TOP MERCHANTS (TAP TO INSPECT)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.1.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                items(topMerchants, key = { it.merchantName }) { merchant ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 3.dp)
                            .appleCard(shape = RoundedCornerShape(16.dp), elevation = 0.5.dp)
                            .clickable { viewModel.selectMerchantForDrilldown(merchant.merchantName) }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF3F4F6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = merchant.merchantName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${merchant.count} orders • ${merchant.category.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Text(
                                text = CurrencyFormatter.format(merchant.totalAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppleBlue
                            )
                        }
                    }
                }
            }
        }

        // Category Detail Drill-Down Bottom Sheet
        selectedCategoryForDrilldown?.let { cat ->
            CategoryDetailSheet(
                category = cat,
                transactions = monthTxns,
                totalMonthExpense = summary.totalExpense,
                onDismiss = { viewModel.selectCategoryForDrilldown(null) },
                onTransactionClick = { txn ->
                    viewModel.selectCategoryForDrilldown(null)
                    onTransactionClick(txn)
                }
            )
        }

        // Merchant Detail Drill-Down Bottom Sheet
        selectedMerchantForDrilldown?.let { merchant ->
            MerchantDetailSheet(
                merchantName = merchant,
                transactions = monthTxns,
                onDismiss = { viewModel.selectMerchantForDrilldown(null) },
                onTransactionClick = { txn ->
                    viewModel.selectMerchantForDrilldown(null)
                    onTransactionClick(txn)
                }
            )
        }
    }
}
