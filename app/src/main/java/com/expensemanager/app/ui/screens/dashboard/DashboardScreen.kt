package com.expensemanager.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
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
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.util.DateTimeUtils
import com.expensemanager.app.ui.components.AccountLedgerSheet
import com.expensemanager.app.ui.components.CategoryDetailSheet
import com.expensemanager.app.ui.components.DonutChart
import com.expensemanager.app.ui.components.GamificationCard
import com.expensemanager.app.ui.components.GlassAccountCard
import com.expensemanager.app.ui.components.GlassStatCard
import com.expensemanager.app.ui.components.TransactionItem
import com.expensemanager.app.ui.screens.gamification.WealthQuestsSheet
import com.expensemanager.app.ui.theme.BorderLight
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.TextTertiary
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

    // Gamification & Wealth Flows
    val liquidScore by viewModel.liquidScore.collectAsState()
    val streakInfo by viewModel.streakInfo.collectAsState()
    val activeQuests by viewModel.activeQuests.collectAsState()
    val roundUpSummary by viewModel.roundUpSummary.collectAsState()

    val bankAccounts = remember(accounts) {
        accounts.filter { it.accountType != AccountType.CREDIT_CARD && it.accountType != AccountType.WALLET }
    }
    val creditCards = remember(accounts) {
        accounts.filter { it.accountType == AccountType.CREDIT_CARD }
    }
    val wallets = remember(accounts) {
        accounts.filter { it.accountType == AccountType.WALLET }
    }

    var isBalanceHidden by remember { mutableStateOf(true) }
    var showWealthQuestsSheet by remember { mutableStateOf(false) }
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
            // Month Selector Header & Quick Actions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sleek Monotone Month Capsule
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.previousMonth() },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = DateTimeUtils.formatMonthYear(monthTimestamp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = { viewModel.nextMonth() },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Month",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Theme Toggle Button
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val isDark = com.expensemanager.app.ui.theme.LocalIsDarkTheme.current
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, BorderLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { com.expensemanager.app.ui.theme.ThemeManager.toggleTheme(context, isDark) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Sleek Sync Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, BorderLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { viewModel.syncSms() },
                                enabled = !isSyncing,
                                modifier = Modifier.size(36.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = TextPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync SMS",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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

            // Gamification & Wealth Discipline Card (Liquid Score, Streak & Compounder)
            item {
                GamificationCard(
                    liquidScore = liquidScore,
                    streakInfo = streakInfo,
                    roundUpSummary = roundUpSummary,
                    onClick = { showWealthQuestsSheet = true }
                )
            }

            // Group 1: Bank Accounts Carousel
            if (bankAccounts.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BANK ACCOUNTS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${bankAccounts.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(bankAccounts, key = { it.id }) { account ->
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

            // Group 2: Credit Cards Carousel
            if (creditCards.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CREDIT CARDS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${creditCards.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(creditCards, key = { it.id }) { account ->
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

            // Group 3: Wallets & Passbooks Carousel
            if (wallets.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WALLETS & PASSBOOKS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${wallets.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(wallets, key = { it.id }) { account ->
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

            // Category Spending Section
            if (categorySpending.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "MONTHLY SPENDING BREAKDOWN",
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
                                onCategorySelected = { category ->
                                    selectedCategoryForDetail = category
                                }
                            )
                        }
                    }
                }
            }

            // Recent Transactions Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
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
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .appleCard(shape = RoundedCornerShape(20.dp), elevation = 1.dp)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions recorded for this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                items(recentTransactions.take(15), key = { it.id }) { txn ->
                    TransactionItem(
                        transaction = txn,
                        onClick = { onTransactionClick(txn) }
                    )
                }
            }
        }

        // Gamification & Wealth Quests Sheet
        if (showWealthQuestsSheet) {
            WealthQuestsSheet(
                liquidScore = liquidScore,
                streakInfo = streakInfo,
                quests = activeQuests,
                roundUpSummary = roundUpSummary,
                onDismiss = { showWealthQuestsSheet = false }
            )
        }

        // Account Ledger Bottom Sheet
        selectedAccountForLedger?.let { account ->
            AccountLedgerSheet(
                account = account,
                transactions = recentTransactions,
                onDismiss = { selectedAccountForLedger = null },
                onTransactionClick = { txn ->
                    selectedAccountForLedger = null
                    onTransactionClick(txn)
                }
            )
        }

        // Category Detail Bottom Sheet
        selectedCategoryForDetail?.let { category ->
            CategoryDetailSheet(
                category = category,
                transactions = recentTransactions,
                totalMonthExpense = summary.totalExpense,
                onDismiss = { selectedCategoryForDetail = null },
                onTransactionClick = { txn ->
                    selectedCategoryForDetail = null
                    onTransactionClick(txn)
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        )
    }
}
