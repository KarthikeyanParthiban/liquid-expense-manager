package com.expensemanager.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.expensemanager.app.core.update.UpdateInfo
import com.expensemanager.app.ui.theme.AppleBlue
import com.expensemanager.app.ui.theme.AppleBlueLight
import com.expensemanager.app.ui.theme.AppleGreen
import com.expensemanager.app.ui.theme.AppleGreenLight
import com.expensemanager.app.ui.theme.AppleRed
import com.expensemanager.app.ui.theme.AppleRedLight
import com.expensemanager.app.ui.theme.BorderLight
import com.expensemanager.app.ui.theme.LightElevatedSurface
import com.expensemanager.app.ui.theme.TextPrimary
import com.expensemanager.app.ui.theme.TextSecondary
import com.expensemanager.app.ui.theme.TextTertiary
import com.expensemanager.app.ui.theme.appleCard
import java.util.Locale

@Composable
fun UpdateAvailableDialog(
    updateInfo: UpdateInfo,
    currentVersion: String,
    isDownloading: Boolean,
    downloadProgress: Float,
    bytesDownloaded: Long,
    totalBytes: Long,
    downloadError: String?,
    onInstallClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .appleCard(
                    shape = RoundedCornerShape(24.dp),
                    elevation = 6.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    borderColor = BorderLight
                )
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Header Row with Vector Icon & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppleBlueLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = AppleBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Software Update",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                letterSpacing = (-0.3).sp
                            )
                            Text(
                                text = "A new build is available",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    if (!isDownloading) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Version Transition and File Size Badges Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(LightElevatedSurface)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "v$currentVersion",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppleGreenLight)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "v${updateInfo.versionName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleGreen
                            )
                        }
                    }

                    if (updateInfo.apkSizeMb > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppleBlueLight)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f MB", updateInfo.apkSizeMb),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Release Notes Section
                Text(
                    text = "WHAT'S NEW",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                        .verticalScroll(scrollState)
                ) {
                    val cleanNotes = cleanReleaseNotes(updateInfo.releaseNotes)

                    Text(
                        text = if (cleanNotes.isNotBlank()) cleanNotes else "Performance enhancements and bug fixes.",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }

                // Downloading Progress Section
                if (isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(LightElevatedSurface)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading package...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${(downloadProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AppleBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        if (totalBytes > 0 || bytesDownloaded > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val downloadedMb = bytesDownloaded / (1024.0 * 1024.0)
                            val totalMb = if (totalBytes > 0) totalBytes / (1024.0 * 1024.0) else updateInfo.apkSizeMb
                            Text(
                                text = String.format(Locale.US, "%.1f MB / %.1f MB", downloadedMb, totalMb),
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }

                // Error Section
                if (downloadError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppleRedLight)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = AppleRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = downloadError,
                            fontSize = 11.sp,
                            color = AppleRed,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isDownloading) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(BorderLight)
                            )
                        ) {
                            Text(
                                text = "Later",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = onInstallClick,
                        enabled = !isDownloading,
                        modifier = Modifier
                            .weight(if (isDownloading) 1f else 1.4f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppleBlue,
                            disabledContainerColor = AppleBlue.copy(alpha = 0.6f)
                        )
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Downloading...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = if (downloadError != null) "Retry Update" else "Install Update",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Strips emojis, raw markdown headers, and links to keep the UI clean and aligned with app aesthetic.
 */
private fun cleanReleaseNotes(raw: String): String {
    val emojiRegex = Regex("[\\p{So}\\p{Cn}\\p{Sk}\\p{Extended_Pictographic}\uD83C-\uDBFF\uDC00-\uDFFF]+")
    val formattedLines = mutableListOf<String>()

    for (line in raw.lines()) {
        var cleaned = line.replace(emojiRegex, "").trim()
        // Strip markdown headers e.g. "### Highlights" -> "Highlights"
        cleaned = cleaned.replace(Regex("^#+\\s*"), "")
        // Strip bold / italics
        cleaned = cleaned.replace("**", "").replace("__", "")

        // Skip direct links, download instructions, or release title repetitions
        if (cleaned.contains("Download", ignoreCase = true) ||
            cleaned.contains(".apk", ignoreCase = true) ||
            cleaned.startsWith("[") ||
            cleaned.startsWith("http") ||
            cleaned.startsWith("LQD v", ignoreCase = true) ||
            cleaned.startsWith("LQD ", ignoreCase = true) ||
            cleaned.startsWith("Kaching", ignoreCase = true) ||
            cleaned.startsWith("Liquid", ignoreCase = true)
        ) {
            continue
        }

        if (cleaned.startsWith("- ") || cleaned.startsWith("* ")) {
            cleaned = "• " + cleaned.substring(2).trim()
        }

        if (cleaned.isNotBlank()) {
            formattedLines.add(cleaned)
        }
    }

    return if (formattedLines.isNotEmpty()) {
        formattedLines.joinToString("\n\n")
    } else {
        "Bug fixes, performance enhancements, and system stability improvements."
    }
}

