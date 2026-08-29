package com.expensemanager.app.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSizeMb: Double,
    val apkSizeBytes: Long,
    val isNewer: Boolean
)

object AppUpdateManager {

    private const val GITHUB_API_URL = "https://api.github.com/repos/KarthikeyanParthiban/liquid-expense-manager/releases/latest"

    /**
     * Safely retrieves the installed app's versionName at runtime.
     */
    fun getAppVersionName(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * Checks GitHub Releases API for the latest version.
     */
    suspend fun checkForUpdates(currentVersionName: String): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "LiquidExpenseManager-App")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode != 200) {
                return@withContext Result.failure(Exception("GitHub API HTTP ${connection.responseCode}: ${connection.responseMessage}"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            val tagName = json.optString("tag_name", "")
            val title = json.optString("name", "LQD $tagName")
            val body = json.optString("body", "Bug fixes and performance enhancements.")
            val assets = json.optJSONArray("assets")

            var downloadUrl: String? = null
            var apkSizeMb = 0.0
            var apkSizeBytes = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url")
                        apkSizeBytes = asset.optLong("size", 0L)
                        apkSizeMb = apkSizeBytes / (1024.0 * 1024.0)
                        break
                    }
                }
            }

            if (downloadUrl.isNullOrEmpty()) {
                return@withContext Result.success(null)
            }

            val cleanRemote = tagName.removePrefix("v").removePrefix("V").trim()
            val cleanCurrent = currentVersionName.removePrefix("v").removePrefix("V").trim()
            val isNewer = isVersionNewer(cleanRemote, cleanCurrent)

            val updateInfo = UpdateInfo(
                tagName = tagName,
                versionName = cleanRemote,
                title = title,
                releaseNotes = body,
                downloadUrl = downloadUrl,
                apkSizeMb = apkSizeMb,
                apkSizeBytes = apkSizeBytes,
                isNewer = isNewer
            )

            Result.success(updateInfo)
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Unable to connect to GitHub (No internet connection or DNS error). Please check your Wi-Fi or mobile data."))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Connection timed out while checking for updates. Please check your network and try again."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extracts numeric semver components from a version string.
     */
    fun parseVersionNumbers(versionStr: String): List<Int> {
        val sanitized = versionStr.trim().removePrefix("v").removePrefix("V")
        val mainPart = sanitized.split("+")[0].split("-")[0]
        return mainPart.split(".")
            .mapNotNull { segment ->
                val digits = segment.takeWhile { it.isDigit() }
                digits.toIntOrNull()
            }
    }

    /**
     * Compares semver strings e.g. "1.2.0" vs "1.1.1".
     * Returns true ONLY if remote is strictly greater than current.
     */
    fun isVersionNewer(remote: String, current: String): Boolean {
        val remoteParts = parseVersionNumbers(remote)
        val currentParts = parseVersionNumbers(current)

        if (remoteParts.isEmpty() || currentParts.isEmpty()) {
            return false
        }

        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    /**
     * Downloads APK file with redirect following, progress reporting, and launches PackageInstaller.
     */
    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        targetFileName: String = "liquid_expense_update.apk",
        expectedSizeBytes: Long = 0L,
        onProgress: (bytesRead: Long, totalBytes: Long, fraction: Float) -> Unit = { _, _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updateDir, targetFileName)

            if (apkFile.exists()) {
                apkFile.delete()
            }

            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirectCount = 0
            val maxRedirects = 10

            while (true) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 20000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "LiquidExpenseManager-App")
                    setRequestProperty("Accept", "*/*")
                }

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newUrl.isNullOrEmpty() || ++redirectCount > maxRedirects) {
                        return@withContext Result.failure(Exception("Too many HTTP redirects ($redirectCount) or invalid location header."))
                    }
                    currentUrl = newUrl
                } else if (responseCode in 200..299) {
                    break
                } else {
                    val errBody = try { connection.errorStream?.bufferedReader()?.readText() } catch (e: Exception) { null }
                    connection.disconnect()
                    return@withContext Result.failure(Exception("HTTP $responseCode: ${connection.responseMessage}${if (!errBody.isNullOrBlank()) " - $errBody" else ""}"))
                }
            }

            val headerLength = connection.contentLengthLong
            val totalExpected = if (headerLength > 0) headerLength else expectedSizeBytes

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(32768) // 32 KB buffer
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastProgressEmit = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastProgressEmit > 50 || (totalExpected > 0 && totalRead == totalExpected)) {
                            lastProgressEmit = now
                            val fraction = if (totalExpected > 0) {
                                (totalRead.toFloat() / totalExpected.toFloat()).coerceIn(0f, 1f)
                            } else 0f
                            onProgress(totalRead, totalExpected, fraction)
                        }
                    }
                }
            }
            connection.disconnect()

            // Verify downloaded APK file integrity
            if (!apkFile.exists() || apkFile.length() < 100_000) {
                return@withContext Result.failure(Exception("Downloaded APK file is incomplete (${apkFile.length()} bytes)."))
            }

            // Emit final 100% progress
            onProgress(apkFile.length(), if (totalExpected > 0) totalExpected else apkFile.length(), 1f)

            // Launch package installer intent via FileProvider
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Download failed: No internet connection or DNS error. Please check your Wi-Fi or mobile data."))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Download timed out while fetching update. Please check your connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
