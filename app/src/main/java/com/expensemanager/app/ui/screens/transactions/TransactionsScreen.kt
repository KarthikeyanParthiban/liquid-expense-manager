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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
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
    val selectedSortOption by viewModel.selectedSortOption.collectAsState()
    val selectedTxnForEdit by viewModel.selectedTransactionForEdit.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    val activeFilterCount = (if (selectedType != null) 1 else 0) +
            (if (selectedCategory != null) 1 else 0) +
            (if (selectedSortOption != TransactionSortOption.NEWEST_FIRST) 1 else 0) +
            (if (searchQuery.isNotBlank()) 1 else 0)

    val hasActiveFilters = activeFilterCount > 0
    val totalFilteredAmount = transactions.sumOf { it.amount }

    // If sorted by amount, group by amount ranges or render directly; if sorted by time, group by date
    val isTimeSorted = selectedSortOption == TransactionSortOption.NEWEST_FIRST || selectedSortOption == TransactionSortOption.OLDEST_FIRST

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            // Unified Search Bar with Integrated Filter/Tune Action Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .appleCard(shape = RoundedCornerShape(16.dp), elevation = 1.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search transactions...", color = TextTertiary, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
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

                Spacer(modifier = Modifier.width(8.dp))

                // Filter & Sort Trigger Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (activeFilterCount > 0) TextPrimary else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (activeFilterCount > 0) Color.Transparent else BorderLight, RoundedCornerShape(16.dp))
                        .clickable { showFilterSheet = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter & Sort",
                            tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.background else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        if (activeFilterCount > 0) {
                            Text(
                                text = "$activeFilterCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                }
            }

            // Single, Clean Horizontal Filter Strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Filter Pills
                item {
                    QuickTypeChip(
                        title = "All",
                        isSelected = selectedType == null,
                        onClick = { viewModel.selectType(null) }
                    )
                }
                item {
                    QuickTypeChip(
                        title = "Debits",
                        isSelected = selectedType == TransactionType.DEBIT,
                        onClick = { viewModel.selectType(TransactionType.DEBIT) }
                    )
                }
                item {
                    QuickTypeChip(
                        title = "Credits",
                        isSelected = selectedType == TransactionType.CREDIT,
                        onClick = { viewModel.selectType(TransactionType.CREDIT) }
                    )
                }
                item {
                    QuickTypeChip(
                        title = "Refunds",
                        isSelected = selectedType == TransactionType.REFUND,
                        onClick = { viewModel.selectType(TransactionType.REFUND) }
                    )
                }

                // Active Category Tag Pill (if set in bottom sheet)
                if (selectedCategory != null) {
                    item {
                        ActiveFilterTagPill(
                            label = selectedCategory!!.displayName,
                            onClear = { viewModel.selectCategory(null) }
                        )
                    }
                }

                // Active Sort Tag Pill (if non-default)
                if (selectedSortOption != TransactionSortOption.NEWEST_FIRST) {
                    item {
                        ActiveFilterTagPill(
                            label = selectedSortOption.displayName,
                            onClear = { viewModel.setSortOption(TransactionSortOption.NEWEST_FIRST) }
                        )
                    }
                }

                if (hasActiveFilters) {
                    item {
                        TextButton(
                            onClick = { viewModel.clearAllFilters() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Clear All",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleBlue
                            )
                        }
                    }
                }
            }

            // Results count + Total Amount Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasActiveFilters) "${transactions.size} RESULTS" else "${transactions.size} TRANSACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )

                if (hasActiveFilters && transactions.isNotEmpty()) {
                    Text(
                        text = "Total: ${CurrencyFormatter.format(totalFilteredAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Transaction List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No transactions found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasActiveFilters) "Try adjusting your search or filters" else "Sync SMS to load transactions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    if (isTimeSorted) {
                        val grouped = transactions.groupBy { DateTimeUtils.formatDate(it.timestamp) }
                        grouped.forEach { (dateStr, txns) ->
                            item(key = "header_$dateStr") {
                                Text(
                                    text = dateStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary,
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
                    } else {
                        items(transactions, key = { it.id }) { txn ->
                            TransactionItem(
                                transaction = txn,
                                onClick = { viewModel.openTransactionDetail(txn) }
                            )
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {
            TransactionFilterBottomSheet(
                selectedType = selectedType,
                selectedCategory = selectedCategory,
                selectedSortOption = selectedSortOption,
                onSelectType = { viewModel.selectType(it) },
                onSelectCategory = { viewModel.selectCategory(it) },
                onSelectSort = { viewModel.setSortOption(it) },
                onResetAll = { viewModel.clearAllFilters() },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
private fun QuickTypeChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = com.expensemanager.app.ui.theme.LocalIsDarkTheme.current
    val bg = if (isSelected) TextPrimary else MaterialTheme.colorScheme.surface
    val textColor = if (isSelected) MaterialTheme.colorScheme.background else TextSecondary
    val border = if (isSelected) Color.Transparent else BorderLight

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
private fun ActiveFilterTagPill(
    label: String,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, BorderLight, RoundedCornerShape(20.dp))
            .padding(start = 10.dp, end = 6.dp, top = 3.dp, bottom = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
