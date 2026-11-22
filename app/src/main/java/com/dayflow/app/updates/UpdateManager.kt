package com.dayflow.app.updates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String
)

object UpdateManager {
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/private1111-33/DAYFLOW-APP/releases/latest"
    private const val APK_ASSET_NAME = "DAYFLOW.apk"

    suspend fun checkForUpdate(context: Context): AppUpdate? = withContext(Dispatchers.IO) {
        val connection = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "DAYFLOW-Android-App")
        }

        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (json.optBoolean("draft", true) || json.optBoolean("prerelease", true)) {
                return@withContext null
            }

            val latestVersion = json.optString("tag_name").removePrefix("v")
            val installedVersion = context.packageManager
                .getPackageInfo(context.packageName, 0).versionName.orEmpty()
            if (!isNewerVersion(latestVersion, installedVersion)) return@withContext null

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var apkUrl: String? = null
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                if (asset.optString("name") == APK_ASSET_NAME) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
            val url = apkUrl?.takeIf { it.startsWith("https://") } ?: return@withContext null
            AppUpdate(
                versionName = latestVersion,
                downloadUrl = url,
                releaseNotes = json.optString("body").take(500)
            )
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadAndInstall(context: Context, update: AppUpdate) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            withContext(Dispatchers.Main) {
                val settingsIntent = Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settingsIntent)
            }
            return@withContext
        }

        val connection = (URL(update.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "DAYFLOW-Android-App")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("تعذر تحميل التحديث")
            }
            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updateDir, "dayflow-update.apk")
            connection.inputStream.use { input ->
                apkFile.outputStream().use { output -> input.copyTo(output) }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            withContext(Dispatchers.Main) {
                context.startActivity(installIntent)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun isNewerVersion(latest: String, installed: String): Boolean {
        val latestParts = latest.split('.').map { it.toIntOrNull() ?: 0 }
        val installedParts = installed.split('.').map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(latestParts.size, installedParts.size)) {
            val a = latestParts.getOrElse(index) { 0 }
            val b = installedParts.getOrElse(index) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
