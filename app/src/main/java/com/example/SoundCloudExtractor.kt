package com.example

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SoundCloudTrack(
    val id: String,
    val title: String,
    val artist: String,
    val streamUrl: String,
    val thumbnailUrl: String
)

object SoundCloudExtractor {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private const val CLIENT_ID = "a3e059563d7fd3372b49b37f00a00bcf"

    fun isSoundCloudUrl(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.contains("soundcloud.com", ignoreCase = true)
    }

    suspend fun resolve(url: String): List<SoundCloudTrack> = withContext(Dispatchers.IO) {
        val tracksList = mutableListOf<SoundCloudTrack>()
        val encodedUrl = Uri.encode(url.trim())
        val resolveUrl = "https://api-v2.soundcloud.com/resolve?url=$encodedUrl&client_id=$CLIENT_ID"
        
        val request = Request.Builder()
            .url(resolveUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val kind = json.optString("kind", "")
                
                if (kind == "track") {
                    val track = parseTrackJson(json)
                    if (track != null) {
                        tracksList.add(track)
                    }
                } else if (kind == "playlist") {
                    val tracksArray = json.optJSONArray("tracks")
                    if (tracksArray != null) {
                        for (i in 0 until tracksArray.length()) {
                            val trackJson = tracksArray.getJSONObject(i)
                            if (!trackJson.has("media")) {
                                val trackId = trackJson.optLong("id", -1)
                                if (trackId != -1L) {
                                    val fullTrack = fetchFullTrack(trackId)
                                    if (fullTrack != null) {
                                        tracksList.add(fullTrack)
                                    }
                                }
                            } else {
                                val track = parseTrackJson(trackJson)
                                if (track != null) {
                                    tracksList.add(track)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext tracksList
    }

    private suspend fun fetchFullTrack(trackId: Long): SoundCloudTrack? = withContext(Dispatchers.IO) {
        val url = "https://api-v2.soundcloud.com/tracks/$trackId?client_id=$CLIENT_ID"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    return@withContext parseTrackJson(JSONObject(body))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private suspend fun parseTrackJson(json: JSONObject): SoundCloudTrack? {
        val id = json.optLong("id", -1)
        if (id == -1L) return null
        val title = json.optString("title", "SoundCloud Track")
        
        val userObj = json.optJSONObject("user")
        val artist = userObj?.optString("username", "Unknown Artist") ?: "Unknown Artist"
        
        var thumbnailUrl = json.optString("artwork_url", "")
        if (thumbnailUrl.isEmpty() || thumbnailUrl == "null") {
            thumbnailUrl = userObj?.optString("avatar_url", "") ?: ""
        }

        // Extract media streams
        val mediaObj = json.optJSONObject("media") ?: return null
        val transcodings = mediaObj.optJSONArray("transcodings") ?: return null
        
        var selectedTranscodingUrl: String? = null
        // We prefer progressive mp3 format if available, otherwise fallback to HLS
        for (i in 0 until transcodings.length()) {
            val tc = transcodings.getJSONObject(i)
            val format = tc.optJSONObject("format")
            val mimeType = format?.optString("mime_type", "") ?: ""
            val protocol = tc.optString("protocol", "")
            
            if (mimeType.contains("audio/mpeg") && protocol == "progressive") {
                selectedTranscodingUrl = tc.optString("url")
                break
            }
        }
        
        if (selectedTranscodingUrl == null) {
            // Fallback to any available transcoding URL
            for (i in 0 until transcodings.length()) {
                val tc = transcodings.getJSONObject(i)
                selectedTranscodingUrl = tc.optString("url")
                break
            }
        }

        if (selectedTranscodingUrl != null) {
            return SoundCloudTrack(
                id = "soundcloud_$id",
                title = title,
                artist = artist,
                streamUrl = selectedTranscodingUrl,
                thumbnailUrl = thumbnailUrl
            )
        }
        return null
    }

    suspend fun fetchPlayableUrl(transcodingUrl: String): String? = withContext(Dispatchers.IO) {
        val urlWithClient = if (transcodingUrl.contains("?")) {
            "$transcodingUrl&client_id=$CLIENT_ID"
        } else {
            "$transcodingUrl?client_id=$CLIENT_ID"
        }
        val request = Request.Builder()
            .url(urlWithClient)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    val json = JSONObject(body)
                    return@withContext json.optString("url", null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
