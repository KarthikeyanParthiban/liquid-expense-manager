package com.expensemanager.app.ui.screens.transactions

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.core.util.DateTimeUtils
import com.expensemanager.app.ui.components.TransactionItem
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.AppleBlueLight
import com.expensemanager.app.ui.theme.AppleGreen
import com.expensemanager.app.ui.theme.AppleGreenLight
import com.expensemanager.app.ui.theme.AppleRed
import com.expensemanager.app.ui.theme.AppleRedLight
import com.expensemanager.app.ui.theme.BorderLight
import com.expensemanager.app.ui.theme.LightCardSurface
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.TextTertiary
import com.expensemanager.app.ui.theme.appleCard

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val selectedTxnForEdit by viewModel.selectedTransactionForEdit.collectAsState()

    val hasActiveFilters = searchQuery.isNotBlank() || selectedCategory != null || selectedType != null
    val totalFilteredAmount = transactions.sumOf { it.amount }

    val groupedTransactions = transactions.groupBy { DateTimeUtils.formatDate(it.timestamp) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            // Clean Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .appleCard(shape = RoundedCornerShape(16.dp), elevation = 1.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search merchant, bank, amount...", color = TextTertiary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Type Filter Chips (All, Expenses, Income, Transfers, Refunds)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { viewModel.selectType(null) },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppleBlue,
                            selectedLabelColor = Color.White,
                            containerColor = LightCardSurface,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedType == null,
                            borderColor = BorderLight,
                            selectedBorderColor = AppleBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedType == TransactionType.DEBIT,
                        onClick = { viewModel.selectType(TransactionType.DEBIT) },
                        label = { Text("Expenses") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppleRed,
                            selectedLabelColor = Color.White,
                            containerColor = LightCardSurface,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedType == TransactionType.DEBIT,
                            borderColor = BorderLight,
                            selectedBorderColor = AppleRed
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedType == TransactionType.CREDIT,
                        onClick = { viewModel.selectType(TransactionType.CREDIT) },
                        label = { Text("Income") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppleGreen,
                            selectedLabelColor = Color.White,
                            containerColor = LightCardSurface,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedType == TransactionType.CREDIT,
                            borderColor = BorderLight,
                            selectedBorderColor = AppleGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedType == TransactionType.TRANSFER,
                        onClick = { viewModel.selectType(TransactionType.TRANSFER) },
                        label = { Text("Transfers & Bills") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF5856D6),
                            selectedLabelColor = Color.White,
                            containerColor = LightCardSurface,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedType == TransactionType.TRANSFER,
                            borderColor = BorderLight,
                            selectedBorderColor = Color(0xFF5856D6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedType == TransactionType.REFUND,
                        onClick = { viewModel.selectType(TransactionType.REFUND) },
                        label = { Text("Refunds") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF007AFF),
                            selectedLabelColor = Color.White,
                            containerColor = LightCardSurface,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedType == TransactionType.REFUND,
                            borderColor = BorderLight,
                            selectedBorderColor = Color(0xFF007AFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Category.entries) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(category) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(category.colorHex))
                            )
                        },
                        label = { Text(category.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(category.colorHex),
                            selectedLabelColor = Color.White,
                            containerColor = LightCardSurface,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = BorderLight,
                            selectedBorderColor = Color(category.colorHex)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Filter Summary & Quick Reset Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${transactions.size} transactions • ${CurrencyFormatter.format(totalFilteredAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                if (hasActiveFilters) {
                    TextButton(
                        onClick = { viewModel.clearAllFilters() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(text = "Reset Filters", color = AppleBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Transactions List grouped by date
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FilterAltOff, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching transactions found",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        if (hasActiveFilters) {
                            TextButton(onClick = { viewModel.clearAllFilters() }) {
                                Text("Clear all filters", color = AppleBlue)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 110.dp)
                ) {
                    groupedTransactions.forEach { (dateHeader, txns) ->
                        item {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        items(txns, key = { it.id }) { txn ->
                            TransactionItem(
                                transaction = txn,
                                onClick = { viewModel.openTransactionDetail(txn) }
                            )
                        }
                    }
                }
            }
        }

        // Transaction Detail / Edit Dialog
        selectedTxnForEdit?.let { txn ->
            TransactionDetailDialog(
                transaction = txn,
                onDismiss = { viewModel.closeTransactionDetail() },
                onSave = { updated, applyRule -> viewModel.updateTransaction(updated, applyRule) },
                onDelete = { id -> viewModel.deleteTransaction(id) }
            )
        }
    }
}
