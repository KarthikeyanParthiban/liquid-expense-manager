package com.expensemanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.core.model.Account
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.TextTertiary
import com.expensemanager.app.ui.theme.appleCard
import androidx.compose.ui.graphics.Color
import com.expensemanager.app.core.util.DateTimeUtils

@Composable
fun GlassAccountCard(
    account: Account,
    hideBalance: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCreditCard = account.accountType == AccountType.CREDIT_CARD
    val isWallet = account.accountType == AccountType.WALLET
    val isInactive = account.isInactive

    val clickableModifier = modifier
        .width(215.dp)
        .appleCard(shape = RoundedCornerShape(22.dp), elevation = 1.dp)
        .then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
        .padding(16.dp)

    Box(modifier = clickableModifier) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isInactive) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isCreditCard -> Icons.Default.CreditCard
                            isWallet -> Icons.Default.Wallet
                            else -> Icons.Default.AccountBalance
                        },
                        contentDescription = null,
                        tint = if (isInactive) Color(0xFFD97706) else TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isInactive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF3C7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "INACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isCreditCard) "CARD • ${account.maskNumber}" else account.maskNumber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = account.bankName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isInactive) TextSecondary else TextPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (isInactive) "Last active > 1 yr ago" else if (isCreditCard) "Available Limit" else if (isWallet) "Wallet Balance" else "Available Balance",
                style = MaterialTheme.typography.labelSmall,
                color = if (isInactive) Color(0xFFD97706) else TextTertiary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (hideBalance) "••••••" else account.lastKnownBalance?.let { CurrencyFormatter.format(it) } ?: if (isInactive) "Dormant" else "Active",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isInactive) TextSecondary else if (account.lastKnownBalance != null) TextPrimary else TextSecondary,
                letterSpacing = if (hideBalance) 2.sp else (-0.3).sp
            )

            if (account.lastUpdated > 0L) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "as of ${DateTimeUtils.formatDate(account.lastUpdated)}",
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            }
        }
    }
}
