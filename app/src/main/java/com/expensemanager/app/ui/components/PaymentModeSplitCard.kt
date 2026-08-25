package com.expensemanager.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.AccountBalance
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
import com.expensemanager.app.core.util.CurrencyFormatter
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.AppleBlueLight
import com.expensemanager.app.ui.theme.AppleGreen
import com.expensemanager.app.ui.theme.AppleGreenLight
import com.expensemanager.app.ui.theme.AppleOrange
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.appleCard

data class PaymentModeSplit(
    val creditCardAmount: Double,
    val bankUpiAmount: Double,
    val totalAmount: Double
) {
    val creditCardPercentage: Float
        get() = if (totalAmount > 0) (creditCardAmount / totalAmount).toFloat() else 0f

    val bankUpiPercentage: Float
        get() = if (totalAmount > 0) (bankUpiAmount / totalAmount).toFloat() else 0f
}

@Composable
fun PaymentModeSplitCard(
    split: PaymentModeSplit,
    modifier: Modifier = Modifier
) {
    if (split.totalAmount <= 0) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .appleCard(shape = RoundedCornerShape(24.dp), elevation = 1.dp)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "PAYMENT CHANNELS SPLIT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-segment progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE5E7EB))
            ) {
                if (split.creditCardPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(split.creditCardPercentage.coerceAtLeast(0.01f))
                            .height(12.dp)
                            .background(Color(0xFF5856D6)) // Apple Purple for Credit Card
                    )
                }
                if (split.bankUpiPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .weight(split.bankUpiPercentage.coerceAtLeast(0.01f))
                            .height(12.dp)
                            .background(AppleBlue)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend Items
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Credit Card item
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5856D6).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color(0xFF5856D6),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Credit Cards (${(split.creditCardPercentage * 100).toInt()}%)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = CurrencyFormatter.format(split.creditCardAmount),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                // Bank & UPI item
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AppleBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = AppleBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Bank / UPI (${(split.bankUpiPercentage * 100).toInt()}%)",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = CurrencyFormatter.format(split.bankUpiAmount),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
