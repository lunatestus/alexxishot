package com.opneai.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ApiClient {
    private const val LAUNCH_ENDPOINT = "https://lunatestus003--vibe-backend-tunnel.modal.run"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.99 Mobile Safari/537.36"

    private var cachedBaseUrl: String? = null
    private val VIDEO_EXTENSIONS = setOf(
        "mp4",
        "mkv",
        "avi",
        "mov",
        "webm",
        "flv",
        "wmv",
        "m4v",
        "ts",
        "mpg",
        "mpeg",
        "3gp",
        "3g2",
        "m2ts",
        "mts"
    )

    data class BaseUrlResult(val url: String?, val error: String?)
    data class FetchResult(val items: List<FileItem>, val error: String?)

    fun resetBaseUrl() {
        cachedBaseUrl = null
    }

    suspend fun getBaseUrl(forceRefresh: Boolean = false): BaseUrlResult = withContext(Dispatchers.IO) {
        if (forceRefresh) cachedBaseUrl = null
        cachedBaseUrl?.let { return@withContext BaseUrlResult(it, null) }
        var lastError: String? = null
        try {
            repeat(8) {
                val conn = URL(LAUNCH_ENDPOINT).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Accept", "application/json")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

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
                            return@withContext BaseUrlResult(url, null)
                        }
                    }
                    lastError = when {
                        status == "starting" -> "Tunnel starting"
                        status.isNotEmpty() -> "Tunnel status: $status"
                        else -> "Launch HTTP $code"
                    }
                } else {
                    lastError = "Launch HTTP $code"
                }

                delay(1200)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext BaseUrlResult(null, "Socket: ${e.message}")
        }

        BaseUrlResult(null, lastError ?: "Tunnel timeout")
    }

    suspend fun fetchFolder(path: String): FetchResult = withContext(Dispatchers.IO) {
        val baseResult = getBaseUrl()
        val base = baseResult.url
        if (base == null) return@withContext FetchResult(emptyList(), baseResult.error ?: "Backend unavailable")

        try {
            val encodedPath = URLEncoder.encode(path, "UTF-8")
            val conn = URL("$base/list?path=$encodedPath").openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

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
                    val size = item.optLong("size", 0L)
                    if (!name.startsWith(".") && shouldIncludeItem(type, name)) {
                        result.add(FileItem(type = type, name = name, path = itemPath, size = size))
                    }
                }
                val err = if (result.isEmpty()) "Empty folder" else null
                return@withContext FetchResult(result, err)
            } else {
                conn.disconnect()
                return@withContext FetchResult(emptyList(), "API HTTP $code")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext FetchResult(emptyList(), "API error: ${e.message}")
        }
    }

    fun getStreamUrl(path: String, baseUrl: String? = cachedBaseUrl): String? {
        val base = baseUrl ?: return null
        val encodedPath = URLEncoder.encode(path, "UTF-8")
        return "$base/stream?path=$encodedPath"
    }

    private fun shouldIncludeItem(type: String, name: String): Boolean {
        if (type == "folder") return true
        if (type != "file") return false
        val dotIndex = name.lastIndexOf('.')
        if (dotIndex <= 0 || dotIndex == name.length - 1) return false
        val ext = name.substring(dotIndex + 1).lowercase()
        return ext in VIDEO_EXTENSIONS
    }
}
