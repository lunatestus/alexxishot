package com.vibe.player.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object UpdateManager {
    const val APK_MIME = "application/vnd.android.package-archive"
    private const val APK_FILENAME = "vibe-update.apk"

    sealed class StartResult {
        data class Started(val id: Long) : StartResult()
        data class Error(val message: String) : StartResult()
    }

    sealed class CompleteResult {
        object StartedInstall : CompleteResult()
        data class Error(val message: String) : CompleteResult()
    }

    fun startUpdateDownload(context: Context, url: String): StartResult {
        // Delete previous APK so DownloadManager doesn't fail with a conflict
        val oldFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILENAME)
        if (oldFile.exists()) oldFile.delete()

        return try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Vibe Player Update")
                .setDescription("Downloading update...")
                .setMimeType(APK_MIME)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    APK_FILENAME
                )
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = dm.enqueue(request)
            if (id <= 0L) StartResult.Error("Unable to start update download")
            else StartResult.Started(id)
        } catch (e: Exception) {
            StartResult.Error(e.message ?: "Unable to start update download")
        }
    }

    fun handleDownloadComplete(context: Context, downloadId: Long): CompleteResult {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = dm.query(query)
        cursor?.use {
            if (!it.moveToFirst()) {
                return CompleteResult.Error("Update download not found")
            }
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILENAME)
                if (file.exists()) {
                    promptInstall(context, file)
                    return CompleteResult.StartedInstall
                } else {
                    return CompleteResult.Error("Downloaded file unavailable")
                }
            } else if (status == DownloadManager.STATUS_FAILED) {
                return CompleteResult.Error("Update download failed")
            }
        }
        return CompleteResult.Error("Update download failed")
    }

    private fun promptInstall(context: Context, file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)

        // Delete the APK after handing it off to the installer so it doesn't waste storage
        file.deleteOnExit()
    }
}
