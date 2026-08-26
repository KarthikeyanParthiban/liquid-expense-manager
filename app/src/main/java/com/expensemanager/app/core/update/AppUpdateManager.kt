package com.expensemanager.app.core.update

import android.content.Context
import android.content.Intent
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
    val isNewer: Boolean
)

object AppUpdateManager {

    private const val GITHUB_API_URL = "https://api.github.com/repos/KarthikeyanParthiban/liquid-expense-manager/releases/latest"

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
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode != 200) {
                return@withContext Result.failure(Exception("GitHub API HTTP ${connection.responseCode}: ${connection.responseMessage}"))
            }

            val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseBody)

            val tagName = json.optString("tag_name", "")
            val title = json.optString("name", "Liquid Expense Manager $tagName")
            val body = json.optString("body", "Bug fixes and performance enhancements.")
            val assets = json.optJSONArray("assets")

            var downloadUrl: String? = null
            var apkSizeMb = 0.0

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url")
                        val bytes = asset.optLong("size", 0L)
                        apkSizeMb = bytes / (1024.0 * 1024.0)
                        break
                    }
                }
            }

            if (downloadUrl.isNullOrEmpty()) {
                return@withContext Result.success(null)
            }

            val cleanRemote = tagName.removePrefix("v").trim()
            val cleanCurrent = currentVersionName.removePrefix("v").trim()
            val isNewer = isVersionNewer(cleanRemote, cleanCurrent)

            val updateInfo = UpdateInfo(
                tagName = tagName,
                versionName = cleanRemote,
                title = title,
                releaseNotes = body,
                downloadUrl = downloadUrl,
                apkSizeMb = apkSizeMb,
                isNewer = isNewer
            )

            Result.success(updateInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Compares semver strings e.g. "1.1.2" vs "1.1.1".
     */
    fun isVersionNewer(remote: String, current: String): Boolean {
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

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
     * Downloads APK file with progress and launches PackageInstaller.
     */
    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        targetFileName: String = "liquid_expense_update.apk",
        onProgress: (progress: Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updateDir, targetFileName)

            if (apkFile.exists()) {
                apkFile.delete()
            }

            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 30000
            }

            val fileLength = connection.contentLengthLong

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (fileLength > 0) {
                            onProgress(totalRead.toFloat() / fileLength.toFloat())
                        }
                    }
                }
            }

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
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
