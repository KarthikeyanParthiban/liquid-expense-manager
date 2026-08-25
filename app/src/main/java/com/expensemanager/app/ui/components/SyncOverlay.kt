package com.expensemanager.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.expensemanager.app.core.model.SyncProgressState
import com.expensemanager.app.core.model.SyncStage
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.AppleGreen
import com.expensemanager.app.ui.theme.AppleTeal
import com.expensemanager.app.ui.theme.DarkBorderHighlight
import com.expensemanager.app.ui.theme.DarkCardSurface
import com.expensemanager.app.ui.theme.LocalIsDarkTheme

@Composable
fun SyncOverlay(
    syncState: SyncProgressState,
    modifier: Modifier = Modifier
) {
    val isVisible = syncState.isSyncing && syncState.showOverlay
    val isDark = LocalIsDarkTheme.current

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* block outside touches */ },
            contentAlignment = Alignment.Center
        ) {
            // Animated scaling card
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f)
                ) + fadeIn(tween(180)),
                exit = scaleOut(
                    targetScale = 0.92f,
                    animationSpec = tween(160)
                ) + fadeOut(tween(160))
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
    val infiniteTransition = rememberInfiniteTransition(label = "sync_radar")
    
    // Rotating radar / pulse animations
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = syncState.progressFraction,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "progress"
    )

    val cardBg = if (isDark) Color(0xFF141416) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) DarkBorderHighlight else Color(0xFFE2E8F0)
    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = cardBg,
        shadowElevation = 24.dp,
        modifier = Modifier
            .padding(horizontal = 28.dp)
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Glowing Radar Badge
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                if (syncState.stage == SyncStage.COMPLETED) {
                    // Success Checkmark Badge
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(AppleGreen.copy(alpha = 0.16f))
                            .border(1.5.dp, AppleGreen.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Done",
                            tint = AppleGreen,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                } else {
                    // Ambient Pulsing Wave Ring
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        AppleBlue.copy(alpha = pulseAlpha),
                                        AppleTeal.copy(alpha = pulseAlpha * 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Rotating Neon Gradient Ring
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .rotate(rotationAngle)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        AppleBlue,
                                        AppleTeal,
                                        AppleGreen,
                                        Color.Transparent,
                                        AppleBlue
                                    )
                                ),
                                CircleShape
                            )
                    )

                    // Central Glowing Core Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        AppleBlue.copy(alpha = 0.25f),
                                        AppleTeal.copy(alpha = 0.15f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (syncState.stage == SyncStage.CLASSIFYING) Icons.Default.AutoAwesome else Icons.Default.Sync,
                            contentDescription = null,
                            tint = if (isDark) Color.White else AppleBlue,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(if (syncState.stage == SyncStage.SCANNING_INBOX) rotationAngle else 0f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Title
            Text(
                text = when (syncState.stage) {
                    SyncStage.SCANNING_INBOX -> "Reading SMS Inbox"
                    SyncStage.CLASSIFYING -> "Classifying Transactions"
                    SyncStage.FINALIZING -> "Syncing Balances & Widgets"
                    SyncStage.COMPLETED -> "Import Complete!"
                    SyncStage.FAILED -> "Sync Failed"
                    SyncStage.IDLE -> "Syncing..."
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Dynamic Subtitle
            Text(
                text = syncState.stageMessage.ifBlank { "Processing financial messages..." },
                style = MaterialTheme.typography.bodyMedium,
                color = subtextColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Precision Gradient Progress Track
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0xFF222228) else Color(0xFFE2E8F0))
                ) {
                    if (syncState.stage == SyncStage.COMPLETED) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AppleGreen)
                        )
                    } else if (syncState.total > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(AppleBlue, AppleTeal, AppleGreen)
                                    )
                                )
                        )
                    } else {
                        // Indeterminate pulse
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.35f)
                                .align(Alignment.Center)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, AppleBlue, Color.Transparent)
                                    )
                                )
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
                        color = subtextColor,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = if (syncState.stage == SyncStage.COMPLETED) {
                            "100%"
                        } else if (syncState.total > 0) {
                            "${(animatedProgress * 100).toInt()}%"
                        } else {
                            "..."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (syncState.stage == SyncStage.COMPLETED) AppleGreen else AppleBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Real-Time Live Classification Chip / Ticker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFF1F5F9))
                    .border(
                        1.dp,
                        if (isDark) Color(0xFF2C2C35) else Color(0xFFE2E8F0),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AppleBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = AppleBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            val liveText = when {
                                syncState.latestMerchant.isNotBlank() -> "Found: ${syncState.latestMerchant}"
                                syncState.latestCategory != null -> "Detected: ${syncState.latestCategory.displayName}"
                                syncState.latestSender.isNotBlank() -> "Scanning: ${syncState.latestSender}"
                                else -> "AI Classification Engine Active"
                            }
                            Text(
                                text = liveText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = titleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${syncState.parsedTransactionsCount} transactions identified",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppleGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
