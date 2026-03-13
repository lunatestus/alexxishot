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
    var lastError: String? = null

    suspend fun getBaseUrl(): String? = withContext(Dispatchers.IO) {
        cachedBaseUrl?.let { return@withContext it }
        try {
            val conn = URL(TUNNEL_ENDPOINT).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "VibePlayer/1.0")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            
            val code = conn.responseCode
            if (code == 200) {
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
                lastError = "Tunnel reported status: ${json.optString("status")}"
            } else {
                lastError = "Tunnel error code: $code"
                conn.disconnect()
            }
        } catch (e: Exception) {
            lastError = "Tunnel connection failed: ${e.message}"
            e.printStackTrace()
        }
        null
    }

    suspend fun fetchFolder(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        lastError = null
        val base = getBaseUrl() 
        if (base == null) {
            // lastError is already set by getBaseUrl
            return@withContext emptyList()
        }
        
        try {
            val encodedPath = URLEncoder.encode(path, "UTF-8")
            val conn = URL("$base/list?path=$encodedPath").openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "VibePlayer/1.0")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            
            val code = conn.responseCode
            if (code == 200) {
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
                if (result.isEmpty()) { lastError = "Folder is empty" }
                return@withContext result
            } else {
                lastError = "API error code: $code"
                conn.disconnect()
            }
        } catch (e: Exception) {
            lastError = "API connection failed: ${e.message}"
            e.printStackTrace()
        }
        emptyList()
    }

    fun getStreamUrl(path: String): String? {
        val base = cachedBaseUrl ?: return null
        val encodedPath = URLEncoder.encode(path, "UTF-8")
        return "$base/stream?path=$encodedPath"
    }
}
