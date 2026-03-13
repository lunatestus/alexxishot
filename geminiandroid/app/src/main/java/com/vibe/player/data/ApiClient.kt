package com.vibe.player.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ApiClient {
    private const val TUNNEL_ENDPOINT = "https://lunatestus003--vibe-backend-tunnel.modal.run"

    private var cachedBaseUrl: String? = null

    suspend fun getBaseUrl(): String? = withContext(Dispatchers.IO) {
        cachedBaseUrl?.let { return@withContext it }
        try {
            val conn = URL(TUNNEL_ENDPOINT).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = JSONObject(body)
                if (json.optString("status") == "running") {
                    val url = json.optString("url")
                    if (url.isNotEmpty()) {
                        cachedBaseUrl = url
                        return@withContext url
                    }
                }
            } else {
                conn.disconnect()
            }
        } catch (_: Exception) {}
        null
    }

    fun clearCache() {
        cachedBaseUrl = null
    }

    suspend fun fetchFolder(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        val base = getBaseUrl() ?: return@withContext emptyList()
        try {
            val encodedPath = URLEncoder.encode(path, "UTF-8")
            val conn = URL("$base/list?path=$encodedPath").openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                val json = JSONObject(body)
                val items = json.getJSONArray("items")
                val result = mutableListOf<FileItem>()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val name = item.getString("name")
                    val itemPath = item.getString("path")
                    val type = item.getString("type")
                    if (!name.startsWith(".")) {
                        result.add(FileItem(type = type, name = name, path = itemPath))
                    }
                }
                return@withContext result
            } else {
                conn.disconnect()
            }
        } catch (_: Exception) {}
        emptyList()
    }

    fun getStreamUrl(path: String): String? {
        val base = cachedBaseUrl ?: return null
        val encodedPath = URLEncoder.encode(path, "UTF-8")
        return "$base/stream?path=$encodedPath"
    }
}
