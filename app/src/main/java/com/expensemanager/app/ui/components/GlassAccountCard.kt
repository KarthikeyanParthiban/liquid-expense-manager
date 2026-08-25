package com.expensemanager.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.core.model.Account
import com.expensemanager.app.core.model.AccountType
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.AppleBlueLight
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.appleCard

@Composable
fun GlassAccountCard(
    account: Account,
    hideBalance: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCreditCard = account.accountType == AccountType.CREDIT_CARD

    val clickableModifier = if (onClick != null) {
        modifier
            .width(200.dp)
            .appleCard(shape = RoundedCornerShape(20.dp), elevation = 1.dp)
            .clickable { onClick() }
            .padding(16.dp)
    } else {
        modifier
            .width(200.dp)
            .appleCard(shape = RoundedCornerShape(20.dp), elevation = 1.dp)
            .padding(16.dp)
    }

    Box(modifier = clickableModifier) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCreditCard) Color(0xFF5856D6).copy(alpha = 0.12f)
                            else AppleBlueLight
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCreditCard) Icons.Default.CreditCard else Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = if (isCreditCard) Color(0xFF5856D6) else AppleBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = account.maskNumber,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = account.bankName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (isCreditCard) "Available Limit" else "Available Balance",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (hideBalance) "₹ ••••••"
                else account.lastKnownBalance?.let { CurrencyFormatter.format(it) } ?: "Active",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = if (hideBalance) 2.sp else 0.sp
            )
        }
    }
}
