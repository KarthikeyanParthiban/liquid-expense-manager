package com.expensemanager.app.ui.screens.accounts

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.core.model.Account
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.ui.components.AccountLedgerSheet
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.AppleBlueLight
import com.expensemanager.app.ui.theme.AppleGreen
import com.expensemanager.app.ui.theme.AppleOrange
import com.expensemanager.app.ui.theme.AppleRed
import com.expensemanager.app.ui.theme.BorderSubtle
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.TextTertiary
import com.expensemanager.app.ui.theme.appleCard

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onTransactionClick: (Transaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val accounts by viewModel.accounts.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val currentMonthSummary by viewModel.currentMonthSummary.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

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
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(monthlyBudget.toInt().toString()) }
    var selectedAccountForLedger by remember { mutableStateOf<Account?>(null) }

    val budgetUsedPercentage = if (monthlyBudget > 0) {
        (currentMonthSummary.totalExpense / monthlyBudget).toFloat()
    } else 0f

    val progressColor = when {
        budgetUsedPercentage >= 1.0f -> AppleRed
        budgetUsedPercentage >= 0.8f -> AppleOrange
        else -> AppleGreen
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Accounts & Budgets",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )

                    IconButton(
                        onClick = { isBalanceHidden = !isBalanceHidden },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isBalanceHidden) "Show Balances" else "Hide Balances",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Monthly Budget Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .appleCard(shape = RoundedCornerShape(24.dp), elevation = 1.dp)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MONTHLY BUDGET",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.1.sp
                            )
                            IconButton(onClick = { showBudgetDialog = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Budget", tint = AppleBlue, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Spent this month",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = CurrencyFormatter.format(currentMonthSummary.totalExpense),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = progressColor
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Budget Limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = CurrencyFormatter.format(monthlyBudget),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { budgetUsedPercentage.coerceAtMost(1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = progressColor,
                            trackColor = BorderSubtle
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (budgetUsedPercentage >= 1.0f) {
                                "Budget exceeded by ${CurrencyFormatter.format(currentMonthSummary.totalExpense - monthlyBudget)}"
                            } else {
                                "${CurrencyFormatter.format(monthlyBudget - currentMonthSummary.totalExpense)} remaining of budget"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (budgetUsedPercentage >= 1.0f) AppleRed else TextSecondary
                        )
                    }
                }
            }

            if (accounts.isEmpty()) {
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
                            text = "No accounts discovered from SMS yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                // Section 1: Bank Accounts
                if (bankAccounts.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bank Accounts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AppleBlueLight)
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${bankAccounts.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleBlue
                                )
                            }
                        }
                    }

                    items(bankAccounts, key = { it.id }) { account ->
                        AccountListItem(
                            account = account,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { selectedAccountForLedger = account }
                        )
                    }
                }

                // Section 2: Credit Cards
                if (creditCards.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Credit Cards",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEEF2FF))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${creditCards.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1)
                                )
                            }
                        }
                    }

                    items(creditCards, key = { it.id }) { account ->
                        AccountListItem(
                            account = account,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { selectedAccountForLedger = account }
                        )
                    }
                }

                // Section 3: Wallets & Others
                if (wallets.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Wallets & Digital Payments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFECFDF5))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${wallets.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    items(wallets, key = { it.id }) { account ->
                        AccountListItem(
                            account = account,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { selectedAccountForLedger = account }
                        )
                    }
                }
            }
        }

        // Budget Edit Dialog
        if (showBudgetDialog) {
            AlertDialog(
                onDismissRequest = { showBudgetDialog = false },
                title = { Text("Set Monthly Budget", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Budget in INR") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newBudget = budgetInput.toDoubleOrNull()
                            if (newBudget != null && newBudget > 0) {
                                viewModel.setMonthlyBudget(newBudget)
                            }
                            showBudgetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppleBlue)
                    ) {
                        Text("Save", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBudgetDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Account Ledger Bottom Sheet
        selectedAccountForLedger?.let { acc ->
            AccountLedgerSheet(
                account = acc,
                transactions = allTransactions,
                onDismiss = { selectedAccountForLedger = null },
                onTransactionClick = { txn ->
                    selectedAccountForLedger = null
                    onTransactionClick(txn)
                },
                onDeleteAccount = { accountId ->
                    viewModel.deleteAccount(accountId)
                }
            )
        }
    }
}

@Composable
private fun AccountListItem(
    account: Account,
    isBalanceHidden: Boolean,
    onClick: () -> Unit
) {
    val isCreditCard = account.accountType == AccountType.CREDIT_CARD
    val isWallet = account.accountType == AccountType.WALLET

    val themeColor = when {
        isCreditCard -> Color(0xFF6366F1)
        isWallet -> Color(0xFF10B981)
        else -> AppleBlue
    }

    val themeBgLight = when {
        isCreditCard -> Color(0xFFEEF2FF)
        isWallet -> Color(0xFFECFDF5)
        else -> AppleBlueLight
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .appleCard(shape = RoundedCornerShape(20.dp), elevation = 1.dp)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(themeBgLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isCreditCard -> Icons.Default.CreditCard
                            isWallet -> Icons.Default.Wallet
                            else -> Icons.Default.AccountBalance
                        },
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(
                        text = account.bankName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isCreditCard) "Credit Card • ${account.maskNumber}" else "${account.accountType.name.replace("_", " ")} • ${account.maskNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                account.lastKnownBalance?.let { bal ->
                    Text(
                        text = if (isBalanceHidden) "₹ ••••••" else CurrencyFormatter.format(bal),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        letterSpacing = if (isBalanceHidden) 2.sp else 0.sp
                    )
                    Text(
                        text = if (isCreditCard) "Available Limit" else if (isWallet) "Wallet Balance" else "Available Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                } ?: run {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppleGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
