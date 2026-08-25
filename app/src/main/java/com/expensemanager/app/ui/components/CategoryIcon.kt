package com.expensemanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.expensemanager.app.core.model.Category
import com.expensemanager.app.ui.theme.TextPrimary

@Composable
fun CategoryIcon(
    category: Category,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp
) {
    val iconVector: ImageVector = when (category) {
        Category.FOOD -> Icons.Default.Restaurant
        Category.GROCERIES -> Icons.Default.ShoppingCart
        Category.SHOPPING -> Icons.Default.ShoppingBag
        Category.TRANSPORT -> Icons.Default.DirectionsCar
        Category.BILLS_UTILITIES -> Icons.AutoMirrored.Filled.ReceiptLong
        Category.ENTERTAINMENT -> Icons.Default.Movie
        Category.HEALTHCARE -> Icons.Default.LocalHospital
        Category.INVESTMENT -> Icons.AutoMirrored.Filled.TrendingUp
        Category.SALARY_INCOME -> Icons.Default.AccountBalanceWallet
        Category.TRANSFERS -> Icons.Default.SwapHoriz
        Category.FEES_CHARGES -> Icons.Default.MoneyOff
        Category.EDUCATION -> Icons.Default.School
        Category.PERSONAL -> Icons.Default.Spa
        Category.OTHERS -> Icons.Default.Category
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = category.displayName,
            tint = TextPrimary,
            modifier = Modifier.size(iconSize)
        )
    }
}
