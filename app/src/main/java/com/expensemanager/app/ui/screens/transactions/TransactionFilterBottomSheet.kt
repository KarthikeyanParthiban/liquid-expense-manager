package com.expensemanager.app.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.BorderLight
import com.expensemanager.app.ui.theme.DarkBorderLight
import com.expensemanager.app.ui.theme.LocalIsDarkTheme
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFilterBottomSheet(
    selectedType: TransactionType?,
    selectedCategory: Category?,
    selectedSortOption: TransactionSortOption,
    onSelectType: (TransactionType?) -> Unit,
    onSelectCategory: (Category?) -> Unit,
    onSelectSort: (TransactionSortOption) -> Unit,
    onResetAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = LocalIsDarkTheme.current
    val sheetBg = if (isDark) Color(0xFF14141A) else Color(0xFFFFFFFF)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextTertiary.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Row: Title + Reset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Filter & Sort",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                TextButton(onClick = onResetAll) {
                    Text(
                        text = "Reset All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. SORT ORDER SECTION
            Text(
                text = "SORT BY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = TextSecondary,
                letterSpacing = 1.1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionSortOption.values().take(2).forEach { option ->
                    val isSelected = selectedSortOption == option
                    SortOptionPill(
                        title = option.displayName,
                        isSelected = isSelected,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectSort(option) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionSortOption.values().drop(2).forEach { option ->
                    val isSelected = selectedSortOption == option
                    SortOptionPill(
                        title = option.displayName,
                        isSelected = isSelected,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectSort(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. TRANSACTION TYPE SECTION
            Text(
                text = "TRANSACTION TYPE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = TextSecondary,
                letterSpacing = 1.1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TypeFilterPill(
                    title = "All Types",
                    isSelected = selectedType == null,
                    onClick = { onSelectType(null) }
                )
                TypeFilterPill(
                    title = "Debits / Spends",
                    isSelected = selectedType == TransactionType.DEBIT,
                    onClick = { onSelectType(TransactionType.DEBIT) }
                )
                TypeFilterPill(
                    title = "Credits / Income",
                    isSelected = selectedType == TransactionType.CREDIT,
                    onClick = { onSelectType(TransactionType.CREDIT) }
                )
                TypeFilterPill(
                    title = "Refunds",
                    isSelected = selectedType == TransactionType.REFUND,
                    onClick = { onSelectType(TransactionType.REFUND) }
                )
                TypeFilterPill(
                    title = "Transfers",
                    isSelected = selectedType == TransactionType.TRANSFER,
                    onClick = { onSelectType(TransactionType.TRANSFER) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. CATEGORY FILTER SECTION
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = TextSecondary,
                letterSpacing = 1.1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CategoryFilterPill(
                    title = "All Categories",
                    isSelected = selectedCategory == null,
                    onClick = { onSelectCategory(null) }
                )
                Category.values().forEach { category ->
                    CategoryFilterPill(
                        title = category.displayName,
                        isSelected = selectedCategory == category,
                        onClick = { onSelectCategory(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Done Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text(
                    text = "Apply & View Results",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SortOptionPill(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val bg = if (isSelected) TextPrimary else if (isDark) Color(0xFF1E1E26) else Color(0xFFF1F5F9)
    val textColor = if (isSelected) MaterialTheme.colorScheme.background else TextSecondary
    val border = if (isSelected) Color.Transparent else if (isDark) DarkBorderLight else BorderLight

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
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
private fun TypeFilterPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val bg = if (isSelected) TextPrimary else if (isDark) Color(0xFF1E1E26) else Color(0xFFF1F5F9)
    val textColor = if (isSelected) MaterialTheme.colorScheme.background else TextSecondary
    val border = if (isSelected) Color.Transparent else if (isDark) DarkBorderLight else BorderLight

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
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
private fun CategoryFilterPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val bg = if (isSelected) TextPrimary else if (isDark) Color(0xFF1E1E26) else Color(0xFFF1F5F9)
    val textColor = if (isSelected) MaterialTheme.colorScheme.background else TextSecondary
    val border = if (isSelected) Color.Transparent else if (isDark) DarkBorderLight else BorderLight

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
