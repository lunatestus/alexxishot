package com.vibe.player.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast

object UpdateManager {
    const val APK_MIME = "application/vnd.android.package-archive"

    fun startUpdateDownload(context: Context, url: String): Long {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Vibe Player Update")
            .setDescription("Downloading update...")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "vibe-update.apk"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    fun handleDownloadComplete(context: Context, downloadId: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = dm.query(query)
        cursor?.use {
            if (!it.moveToFirst()) {
                Toast.makeText(context, "Update download not found", Toast.LENGTH_SHORT).show()
                return
            }
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                val uri = dm.getUriForDownloadedFile(downloadId)
                if (uri != null) {
                    promptInstall(context, uri)
                } else {
                    Toast.makeText(context, "Downloaded file unavailable", Toast.LENGTH_SHORT).show()
                }
            } else if (status == DownloadManager.STATUS_FAILED) {
                Toast.makeText(context, "Update download failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun promptInstall(context: Context, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
        }

        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
