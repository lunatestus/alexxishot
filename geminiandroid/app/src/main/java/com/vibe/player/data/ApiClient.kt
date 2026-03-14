package com.vibe.player.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ApiClient {
    private const val LAUNCH_ENDPOINT = "https://lunatestus003--vibe-backend-launch.modal.run"
    // Using a standard Chrome/Android User-Agent to avoid Cloudflare/Modal blocking
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.99 Mobile Safari/537.36"

    private var cachedBaseUrl: String? = null
    var lastError: String? = null

    fun resetBaseUrl() {
        cachedBaseUrl = null
    }

    suspend fun getBaseUrl(): String? = withContext(Dispatchers.IO) {
        cachedBaseUrl?.let { return@withContext it }
        try {
            repeat(20) {
                val conn = URL(LAUNCH_ENDPOINT).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 20000
                conn.readTimeout = 20000

                val code = conn.responseCode
                val body = try {
                    if (code in 200..299) conn.inputStream.bufferedReader().readText()
                    else conn.errorStream?.bufferedReader()?.readText().orEmpty()
                } catch (_: Exception) {
                    ""
                } finally {
                    conn.disconnect()
                }

                if (body.isNotEmpty()) {
                    val json = JSONObject(body)
                    val status = json.optString("status")
                    if (status == "running") {
                        val url = json.optString("url")
                        if (url.isNotEmpty()) {
                            cachedBaseUrl = url
                            return@withContext url
                        }
                    }
                    if (status == "starting") {
                        lastError = "Tunnel starting"
                    } else if (status.isNotEmpty()) {
                        lastError = "Tunnel status: $status"
                    }
                } else {
                    lastError = "Launch HTTP $code"
                }

                kotlinx.coroutines.delay(1500)
            }
        } catch (e: Exception) {
            lastError = "Socket: ${e.message}"
            e.printStackTrace()
        }

        null
    }

    suspend fun fetchFolder(path: String): List<FileItem> = withContext(Dispatchers.IO) {
        lastError = null
        val base = getBaseUrl() 
        if (base == null) return@withContext emptyList()
        
        try {
            val encodedPath = URLEncoder.encode(path, "UTF-8")
            val conn = URL("$base/list?path=$encodedPath").openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 20000
            conn.readTimeout = 20000
            
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
                if (result.isEmpty()) { lastError = "Empty folder" }
                return@withContext result
            } else {
                lastError = "API HTTP $code"
                conn.disconnect()
            }
        } catch (e: Exception) {
            lastError = "API error: ${e.message}"
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
