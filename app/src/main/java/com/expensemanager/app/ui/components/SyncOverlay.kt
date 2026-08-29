package com.expensemanager.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensemanager.app.core.model.SyncProgressState
import com.expensemanager.app.core.model.SyncStage
import com.expensemanager.app.ui.theme.BorderLight
import com.expensemanager.app.ui.theme.LocalIsDarkTheme
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary

@Composable
fun SyncOverlay(
    syncState: SyncProgressState,
    modifier: Modifier = Modifier
) {
    val isVisible = syncState.isSyncing && syncState.showOverlay
    val isDark = LocalIsDarkTheme.current

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.72f else 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* block outside touches */ },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
                ) + fadeIn(tween(180)),
                exit = scaleOut(
                    targetScale = 0.94f,
                    animationSpec = tween(150)
                ) + fadeOut(tween(150))
            ) {
                SyncCardContent(
                    syncState = syncState,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
private fun SyncCardContent(
    syncState: SyncProgressState,
    isDark: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = syncState.progressFraction,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f),
        label = "syncProgress"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (isDark) 0.dp else 12.dp,
        modifier = Modifier
            .padding(horizontal = 28.dp)
            .fillMaxWidth()
            .border(1.dp, BorderLight, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Status Indicator: Clean LumaSpinLoader matching startup & brand aesthetic
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                when (syncState.stage) {
                    SyncStage.COMPLETED -> {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFF3F4F6))
                                .border(1.dp, BorderLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    SyncStage.FAILED -> {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFF3F4F6))
                                .border(1.dp, BorderLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Failed",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    else -> {
                        LumaSpinLoader(
                            size = 48.dp,
                            strokeWidth = 2.6.dp,
                            color = TextPrimary,
                            cycleDurationMillis = 2000
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stage Title
            val titleText = when (syncState.stage) {
                SyncStage.SCANNING_INBOX -> "Reading SMS Inbox"
                SyncStage.CLASSIFYING -> "Classifying Transactions"
                SyncStage.FINALIZING -> "Updating Balances"
                SyncStage.COMPLETED -> "Sync Complete"
                SyncStage.FAILED -> "Sync Failed"
                SyncStage.IDLE -> "Syncing..."
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Dynamic Subtitle
            Text(
                text = syncState.stageMessage.ifBlank { "Processing transactions..." },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Precision Monochromatic Progress Track
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isDark) Color(0xFF262626) else Color(0xFFE5E7EB))
                ) {
                    if (syncState.stage == SyncStage.COMPLETED) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(TextPrimary)
                        )
                    } else if (syncState.total > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .background(TextPrimary)
                        )
                    } else {
                        // Subtle indeterminate pulse
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.3f)
                                .align(Alignment.Center)
                                .background(TextSecondary.copy(alpha = 0.35f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Labels & Percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (syncState.total > 0) {
                            "${syncState.current} of ${syncState.total} messages"
                        } else {
                            "Scanning inbox..."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Normal
                    )

                    Text(
                        text = if (syncState.stage == SyncStage.COMPLETED) {
                            "100%"
                        } else if (syncState.total > 0) {
                            "${(animatedProgress * 100).toInt()}%"
                        } else {
                            "..."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Live Classification Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0xFF191919) else Color(0xFFF9FAFB))
                    .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF262626) else Color(0xFFE5E7EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val mainLabel = when {
                            syncState.latestMerchant.isNotBlank() -> syncState.latestMerchant
                            syncState.latestSender.isNotBlank() -> "SMS from ${syncState.latestSender}"
                            else -> "Smart Categorization"
                        }

                        val subLabel = when {
                            syncState.latestCategory != null && syncState.parsedTransactionsCount > 0 ->
                                "${syncState.latestCategory.displayName} • ${syncState.parsedTransactionsCount} found"
                            syncState.latestCategory != null ->
                                syncState.latestCategory.displayName
                            syncState.parsedTransactionsCount > 0 ->
                                "${syncState.parsedTransactionsCount} transaction(s) identified"
                            else -> "Analyzing financial alerts"
                        }

                        Text(
                            text = mainLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
