package com.expensemanager.app.ui.screens.transactions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.core.model.Transaction
import com.expensemanager.app.core.model.TransactionStatus
import com.expensemanager.app.core.model.TransactionType
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.core.util.DateTimeUtils
import com.expensemanager.app.ui.components.CategoryIcon
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.AppleBlueLight
import com.expensemanager.app.ui.theme.AppleGreenDark
import com.expensemanager.app.ui.theme.AppleGreenLight
import com.expensemanager.app.ui.theme.AppleOrange
import com.expensemanager.app.ui.theme.AppleRed
import com.expensemanager.app.ui.theme.AppleRedDark
import com.expensemanager.app.ui.theme.AppleRedLight
import com.expensemanager.app.ui.theme.BorderLight
import com.expensemanager.app.ui.theme.BorderSubtle
import com.expensemanager.app.ui.theme.LightCardSurface
import com.expensemanager.app.ui.theme.LightElevatedSurface
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.TextTertiary
import com.expensemanager.app.ui.theme.appleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSave: (Transaction, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var merchantName by remember { mutableStateOf(transaction.merchantName ?: "") }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var note by remember { mutableStateOf(transaction.note ?: "") }
    var isExcluded by remember { mutableStateOf(transaction.isExcludedFromBudget) }
    var applyRuleToAll by remember { mutableStateOf(false) }
    var showAdvancedDetails by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val isCredit = transaction.type == TransactionType.CREDIT ||
            transaction.type == TransactionType.REFUND ||
            transaction.type == TransactionType.REVERSAL ||
            transaction.type == TransactionType.CARD_SETTLEMENT

    val amountColor = if (isCredit) AppleGreenDark else AppleRedDark

    val confidencePct = (transaction.confidence * 100).toInt()
    val confidenceBadgeColor = when {
        transaction.confidence >= 0.90f -> AppleGreenDark
        transaction.confidence >= 0.75f -> AppleBlue
        else -> AppleOrange
    }
    val confidenceBadgeBg = when {
        transaction.confidence >= 0.90f -> AppleGreenLight
        transaction.confidence >= 0.75f -> AppleBlueLight
        else -> Color(0xFFFFF8E1)
    }

    val isCard = transaction.accountType == AccountType.CREDIT_CARD

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(category = selectedCategory, size = 42.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Transaction Details",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${DateTimeUtils.formatFullDate(transaction.timestamp)} • ${DateTimeUtils.formatTime(transaction.timestamp)}",
                            fontSize = 12.sp,
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

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Hero Amount & Status Badges
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleCard(shape = RoundedCornerShape(20.dp), elevation = 1.dp)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${if (isCredit) "+" else "-"}${CurrencyFormatter.format(transaction.amount, transaction.currency)}",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = amountColor,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (transaction.status) {
                                            TransactionStatus.COMPLETED -> Color(0xFFE8F5E9)
                                            TransactionStatus.REFUNDED, TransactionStatus.REVERSED -> Color(0xFFFFF3E0)
                                            TransactionStatus.PENDING -> Color(0xFFFFF8E1)
                                            TransactionStatus.FAILED -> Color(0xFFFFEBEE)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = transaction.status.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (transaction.status) {
                                        TransactionStatus.COMPLETED -> AppleGreenDark
                                        TransactionStatus.REFUNDED, TransactionStatus.REVERSED -> AppleOrange
                                        TransactionStatus.PENDING -> Color(0xFFF57F17)
                                        TransactionStatus.FAILED -> AppleRedDark
                                    }
                                )
                            }

                            // Confidence Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(confidenceBadgeBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$confidencePct% Parsed",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = confidenceBadgeColor
                                )
                            }

                            // Excluded indicator
                            if (isExcluded) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF3F4F6))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Excluded from Budget",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Prominent Original SMS Message Card
                if (!transaction.rawBody.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleCard(shape = RoundedCornerShape(18.dp), elevation = 1.dp)
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
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(AppleBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Message,
                                        contentDescription = null,
                                        tint = AppleBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "BANK SMS ALERT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Sender: ${transaction.sender ?: "Bank Alert"}",
                                        fontSize = 11.sp,
                                        color = TextTertiary
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("SMS Text", transaction.rawBody)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "SMS text copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF3F4F6))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy SMS",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF9FAFB))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = transaction.rawBody,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 19.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 3. Merchant / Payee Text Input
                Text(
                    text = "MERCHANT & CATEGORIZATION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    label = { Text("Merchant / Payee Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AppleBlue,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips Selector
                Text(
                    text = "Select Category:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(Category.values()) { cat ->
                        val isSelected = selectedCategory == cat
                        val catColor = Color(cat.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName, fontSize = 12.sp) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = catColor)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.15f),
                                selectedLabelColor = catColor,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) catColor else BorderLight,
                                selectedBorderColor = catColor,
                                enabled = true,
                                selected = isSelected
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Smart Rule Checkbox
                if (merchantName.isNotBlank() && selectedCategory != transaction.category) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = applyRuleToAll,
                            onCheckedChange = { applyRuleToAll = it },
                            colors = CheckboxDefaults.colors(checkedColor = AppleBlue)
                        )
                        Text(
                            text = "Always categorize '$merchantName' as ${selectedCategory.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Financial Account & Ledger Details
                Text(
                    text = "ACCOUNT & METADATA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleCard(shape = RoundedCornerShape(18.dp), elevation = 1.dp)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isCard) Icons.Default.CreditCard else Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = if (isCard) Color(0xFF5856D6) else AppleBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${transaction.bankName} ${transaction.accountMask ?: ""}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = transaction.accountType.name.replace("_", " "),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (!transaction.referenceId.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Reference / UTR", fontSize = 12.sp, color = TextSecondary)
                                Text(transaction.referenceId, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppleBlue)
                            }
                        }

                        if (transaction.balanceAfter != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Balance After Txn", fontSize = 12.sp, color = TextSecondary)
                                Text(CurrencyFormatter.format(transaction.balanceAfter), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Personal Note Field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Personal Note (Optional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AppleBlue,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 6. Exclude from Budget Switch Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleCard(shape = RoundedCornerShape(16.dp), elevation = 1.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exclude from Budget",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Don't count this in monthly expense analytics",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary
                            )
                        }
                        Switch(
                            checked = isExcluded,
                            onCheckedChange = { isExcluded = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppleBlue)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Expandable Diagnostics & Audit Accordion
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showAdvancedDetails = !showAdvancedDetails }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Financial Audit & Linking Diagnostics",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = if (showAdvancedDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = showAdvancedDetails) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3F4F6))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Transaction Type: ${transaction.type.name}", fontSize = 11.sp, color = TextSecondary)
                            Text("Classification Reason: ${transaction.classificationReason ?: "Pattern matching"}", fontSize = 11.sp, color = TextSecondary)
                            if (!transaction.originalTransactionId.isNullOrBlank()) {
                                Text("Linked Original Debit ID: ${transaction.originalTransactionId}", fontSize = 11.sp, color = AppleBlue)
                            }
                            if (!transaction.relatedTransactionId.isNullOrBlank()) {
                                Text("Cross-Channel Related ID: ${transaction.relatedTransactionId}", fontSize = 11.sp, color = AppleBlue)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Fixed Bottom Action Buttons Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AppleRed.copy(alpha = 0.5f)))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        val updated = transaction.copy(
                            merchantName = merchantName.ifBlank { null },
                            category = selectedCategory,
                            note = note.ifBlank { null },
                            isExcludedFromBudget = isExcluded
                        )
                        onSave(updated, applyRuleToAll)
                    },
                    modifier = Modifier
                        .weight(1.6f)
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleBlue)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction record? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete(transaction.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
