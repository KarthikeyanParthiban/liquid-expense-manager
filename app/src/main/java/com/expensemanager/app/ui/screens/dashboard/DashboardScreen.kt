package com.expensemanager.app.ui.screens.dashboard

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.core.model.Account
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.util.DateTimeUtils
import com.expensemanager.app.ui.components.AccountLedgerSheet
import com.expensemanager.app.ui.components.CategoryDetailSheet
import com.expensemanager.app.ui.components.CategoryIcon
import com.expensemanager.app.ui.components.DonutChart
import com.expensemanager.app.ui.components.GlassAccountCard
import com.expensemanager.app.ui.components.GlassStatCard
import com.expensemanager.app.ui.components.TransactionItem
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.AppleBlueLight
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.appleCard

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTransactions: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthTimestamp by viewModel.selectedMonthTimestamp.collectAsState()
    val summary by viewModel.monthlySummary.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val totalBankBalance by viewModel.totalBankBalance.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val categorySpending by viewModel.categorySpending.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    var isBalanceHidden by remember { mutableStateOf(false) }
    var selectedAccountForLedger by remember { mutableStateOf<Account?>(null) }
    var selectedCategoryForDetail by remember { mutableStateOf<Category?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp)
        ) {
            // Header with Month Selector & Sync Button
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
                            text = "Expense Manager",
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

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AppleBlueLight)
                    ) {
                        IconButton(
                            onClick = { viewModel.syncSms() },
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AppleBlue)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = "Sync SMS", tint = AppleBlue)
                            }
                        }
                    }
                }
            }

            // Stat Card (Total Available Balance, Spent, Income)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                GlassStatCard(
                    totalBankBalance = totalBankBalance,
                    totalExpense = summary.totalExpense,
                    totalIncome = summary.totalIncome,
                    hideBalance = isBalanceHidden,
                    onToggleHideBalance = { isBalanceHidden = !isBalanceHidden }
                )
            }

            // Bank Accounts Carousel (Clickable for Ledger & Eye Icon for Privacy)
            if (accounts.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACCOUNTS & CARDS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.1.sp
                            )

                            IconButton(
                                onClick = { isBalanceHidden = !isBalanceHidden },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isBalanceHidden) "Show Balances" else "Hide Balances",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(accounts, key = { it.id }) { account ->
                                GlassAccountCard(
                                    account = account,
                                    hideBalance = isBalanceHidden,
                                    onClick = { selectedAccountForLedger = account }
                                )
                            }
                        }
                    }
                }
            }

            // Category Spending Breakdown (Clickable for Detail)
            if (categorySpending.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = "SPENDING BREAKDOWN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.1.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .appleCard(shape = RoundedCornerShape(24.dp), elevation = 1.dp)
                                .padding(18.dp)
                        ) {
                            DonutChart(
                                spendingList = categorySpending,
                                totalExpense = summary.totalExpense,
                                onCategorySelected = { cat -> selectedCategoryForDetail = cat }
                            )
                        }
                    }
                }

                items(categorySpending.take(4), key = { it.category }) { item ->
                    val catEnum = runCatching { Category.valueOf(item.category) }.getOrDefault(Category.OTHERS)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 3.dp)
                            .appleCard(shape = RoundedCornerShape(16.dp), elevation = 0.5.dp)
                            .clickable { selectedCategoryForDetail = catEnum }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIcon(category = catEnum, size = 34.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = catEnum.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = com.expensemanager.app.core.util.CurrencyFormatter.format(item.totalAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT TRANSACTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.1.sp
                    )

                    TextButton(onClick = onNavigateToTransactions) {
                        Text(
                            text = "See All",
                            color = AppleBlue,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            // Recent Transactions List
            if (recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions found.\nTap Sync to read bank SMS.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(recentTransactions.take(8), key = { it.id }) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        )

        // Account Ledger Bottom Sheet
        selectedAccountForLedger?.let { acc ->
            AccountLedgerSheet(
                account = acc,
                transactions = recentTransactions,
                onDismiss = { selectedAccountForLedger = null },
                onTransactionClick = { txn ->
                    selectedAccountForLedger = null
                    onTransactionClick(txn)
                }
            )
        }

        // Category Detail Bottom Sheet
        selectedCategoryForDetail?.let { cat ->
            CategoryDetailSheet(
                category = cat,
                transactions = recentTransactions,
                totalMonthExpense = summary.totalExpense,
                onDismiss = { selectedCategoryForDetail = null },
                onTransactionClick = { txn ->
                    selectedCategoryForDetail = null
                    onTransactionClick(txn)
                }
            )
        }
    }
}
