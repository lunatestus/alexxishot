package com.vibe.player.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object AppUpdater {

    suspend fun downloadAndInstall(
        context: Context,
        url: String,
        onProgress: (Long, Long) -> Unit,
        onStatus: (String) -> Unit
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val updateDir = File(context.externalCacheDir, "updates")
                if (!updateDir.exists()) updateDir.mkdirs()
                
                // Delete previous APKs
                updateDir.listFiles()?.forEach { it.delete() }
                
                val apkFile = File(updateDir, "vibe-update.apk")
                
                onStatus("Downloading update…")
                val connection = URL(url).openConnection()
                connection.connect()
                
                val fileLength = connection.contentLength
                val input = connection.getInputStream()
                val output = FileOutputStream(apkFile)
                
                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                var lastProgressUpdate = 0L
                
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    val now = System.currentTimeMillis()
                    if (now - lastProgressUpdate >= 250) {
                        lastProgressUpdate = now
                        onProgress(total, fileLength.toLong())
                    }
                    output.write(data, 0, count)
                }
                onProgress(total, fileLength.toLong())
                
                output.flush()
                output.close()
                input.close()
                
                onStatus("Installing update…")
                installApk(context, apkFile)
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
