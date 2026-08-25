package com.expensemanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.AppleBlueLight
import com.expensemanager.app.ui.theme.AppleGreen
import com.expensemanager.app.ui.theme.AppleGreenLight
import com.expensemanager.app.ui.theme.AppleRed
import com.expensemanager.app.ui.theme.AppleRedLight
import com.expensemanager.app.ui.theme.BorderLight
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.appleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountLedgerSheet(
    account: Account,
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sortByAmount by remember { mutableStateOf(false) }

    val accountTxns = transactions.filter {
        it.accountId == account.id ||
                (it.bankName.equals(account.bankName, ignoreCase = true) &&
                        (it.accountMask == account.maskNumber || it.accountMask == null || account.maskNumber == "PRIMARY"))
    }

    val totalDebits = accountTxns.filter { it.type == TransactionType.DEBIT && !it.isExcludedFromBudget }.sumOf { it.amount }
    val totalCredits = accountTxns.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }

    val isCreditCard = account.accountType == AccountType.CREDIT_CARD
    val balanceLabel = if (isCreditCard) "AVAILABLE LIMIT" else "AVAILABLE BALANCE"

    val sortedTxns = remember(accountTxns, sortByAmount) {
        if (sortByAmount) accountTxns.sortedByDescending { it.amount }
        else accountTxns.sortedByDescending { it.timestamp }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCreditCard) Color(0xFF5856D6).copy(alpha = 0.12f) else AppleBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCreditCard) Icons.Default.CreditCard else Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = if (isCreditCard) Color(0xFF5856D6) else AppleBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = account.bankName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${account.accountType.name.replace("_", " ")} • ${account.maskNumber}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Balance Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .appleCard(shape = RoundedCornerShape(20.dp), elevation = 1.dp)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = balanceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = account.lastKnownBalance?.let { CurrencyFormatter.format(it) } ?: "Not available",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total debits pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppleRedLight)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(text = "Spent / Debits", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    text = CurrencyFormatter.format(totalDebits),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleRed
                                )
                            }
                        }

                        // Total credits pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppleGreenLight)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(text = "Income / Credits", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    text = CurrencyFormatter.format(totalCredits),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transactions Header with Sort Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACCOUNT LEDGER (${accountTxns.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.1.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = !sortByAmount,
                        onClick = { sortByAmount = false },
                        label = { Text("Newest", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppleBlueLight,
                            selectedLabelColor = AppleBlue
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (!sortByAmount) AppleBlue else BorderLight,
                            selectedBorderColor = AppleBlue,
                            enabled = true,
                            selected = !sortByAmount
                        ),
                        modifier = Modifier.height(28.dp)
                    )

                    FilterChip(
                        selected = sortByAmount,
                        onClick = { sortByAmount = true },
                        label = { Text("Amount", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppleBlueLight,
                            selectedLabelColor = AppleBlue
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (sortByAmount) AppleBlue else BorderLight,
                            selectedBorderColor = AppleBlue,
                            enabled = true,
                            selected = sortByAmount
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable List of Account Transactions
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sortedTxns, key = { it.id }) { txn ->
                    TransactionItem(
                        transaction = txn,
                        onClick = { onTransactionClick(txn) }
                    )
                }
            }
        }
    }
}
