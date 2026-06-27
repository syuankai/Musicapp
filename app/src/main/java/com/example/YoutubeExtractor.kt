package com.example

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ExtractedStream(
    val videoId: String,
    val title: String,
    val artist: String,
    val streamUrl: String,
    val isVideo: Boolean,
    val thumbnailUrl: String
)

data class YoutubeSearchResult(
    val videoId: String,
    val title: String,
    val uploaderName: String,
    val durationText: String,
    val thumbnailUrl: String
)

object YoutubeExtractor {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    // Public list of resilient Piped API instances to cycle through in case of rate-limiting
    private val pipedInstances = listOf(
        "https://pipedapi.adminforge.de",
        "https://piped-api.garudalinux.org",
        "https://pipedapi.swg.rocks",
        "https://pipedapi.colby.cafe",
        "https://pipedapi.hostux.net",
        "https://pipedapi.suyu.sh"
    )

    fun extractVideoId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.length == 11 && trimmed.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
            return trimmed
        }

        try {
            val uri = Uri.parse(trimmed)
            // 1. Check v query parameter (standard youtube.com/watch?v=...)
            val vParam = uri.getQueryParameter("v")
            if (vParam != null && vParam.length == 11) return vParam

            // 2. Check path segments
            val segments = uri.pathSegments
            if (segments != null && segments.isNotEmpty()) {
                for (i in segments.indices) {
                    if ((segments[i] == "shorts" || segments[i] == "embed" || segments[i] == "v") && i + 1 < segments.size) {
                        val potentialId = segments[i + 1]
                        if (potentialId.length == 11) return potentialId
                    }
                }
                // 3. Check youtu.be/... or mobile formats
                if (trimmed.contains("youtu.be/", ignoreCase = true)) {
                    val lastSegment = segments.lastOrNull()
                    if (lastSegment != null && lastSegment.length == 11) return lastSegment
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Regex fallback
        val fallbackRegex = "(?:v=|\\/shorts\\/|\\/embed\\/|\\/v\\/|youtu\\.be\\/)([a-zA-Z0-9_-]{11})".toRegex()
        val match = fallbackRegex.find(trimmed)
        if (match != null && match.groupValues.size > 1) {
            return match.groupValues[1]
        }

        return null
    }

    suspend fun search(query: String): List<YoutubeSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<YoutubeSearchResult>()
        for (baseInstance in pipedInstances) {
            val encodedQuery = Uri.encode(query)
            val url = "$baseInstance/search?q=$encodedQuery&filter=all"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use

                    val responseBody = response.body?.string() ?: return@use
                    val json = JSONObject(responseBody)
                    val items = json.optJSONArray("items") ?: return@use

                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val type = item.optString("type", "")
                        if (type == "stream") {
                            val itemUrl = item.optString("url", "")
                            val videoId = if (itemUrl.contains("v=")) {
                                itemUrl.substringAfter("v=")
                            } else if (itemUrl.startsWith("/")) {
                                itemUrl.substringAfter("v=")
                            } else {
                                ""
                            }
                            
                            val finalVideoId = if (videoId.contains("&")) videoId.substringBefore("&") else videoId
                            
                            if (finalVideoId.length == 11) {
                                val title = item.optString("title", "Unknown Title")
                                val uploader = item.optString("uploaderName", "Unknown Uploader")
                                val thumbnail = item.optString("thumbnail", "https://img.youtube.com/vi/$finalVideoId/0.jpg")
                                val duration = item.optInt("duration", 0)
                                val min = duration / 60
                                val sec = duration % 60
                                val durationText = String.format("%d:%02d", min, sec)

                                results.add(
                                    YoutubeSearchResult(
                                        videoId = finalVideoId,
                                        title = title,
                                        uploaderName = uploader,
                                        durationText = durationText,
                                        thumbnailUrl = thumbnail
                                    )
                                )
                            }
                        }
                    }
                }
                if (results.isNotEmpty()) {
                    return@withContext results
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Silently fallback to next instance
            }
        }
        return@withContext results
    }

    suspend fun fetchStream(videoId: String, isVideoMode: Boolean): ExtractedStream? = withContext(Dispatchers.IO) {
        for (baseInstance in pipedInstances) {
            val url = "$baseInstance/streams/$videoId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use

                    val responseBody = response.body?.string() ?: return@use
                    val json = JSONObject(responseBody)

                    val title = json.optString("title", "YouTube Track")
                    val artist = json.optString("uploader", "Unknown Artist")
                    val thumbnailUrl = json.optString("thumbnailUrl", "https://img.youtube.com/vi/$videoId/0.jpg")

                    if (isVideoMode) {
                        // Extract video streams
                        val videoStreams = json.optJSONArray("videoStreams")
                        if (videoStreams != null && videoStreams.length() > 0) {
                            // Find the best mp4 stream or just first non-videoOnly stream
                            var selectedUrl: String? = null
                            for (i in 0 until videoStreams.length()) {
                                val obj = videoStreams.getJSONObject(i)
                                val mimeType = obj.optString("mimeType", "")
                                val isVideoOnly = obj.optBoolean("videoOnly", false)
                                if (!isVideoOnly && mimeType.contains("video/mp4")) {
                                    selectedUrl = obj.optString("url")
                                    break
                                }
                            }
                            // Fallback to first non-videoOnly stream if no mp4 found
                            if (selectedUrl == null) {
                                for (i in 0 until videoStreams.length()) {
                                    val obj = videoStreams.getJSONObject(i)
                                    val isVideoOnly = obj.optBoolean("videoOnly", false)
                                    if (!isVideoOnly) {
                                        selectedUrl = obj.optString("url")
                                        break
                                    }
                                }
                            }
                            if (selectedUrl != null) {
                                return@withContext ExtractedStream(
                                    videoId = videoId,
                                    title = title,
                                    artist = artist,
                                    streamUrl = selectedUrl,
                                    isVideo = true,
                                    thumbnailUrl = thumbnailUrl
                                )
                            }
                        }
                    } else {
                        // Extract audio-only streams
                        val audioStreams = json.optJSONArray("audioStreams")
                        if (audioStreams != null && audioStreams.length() > 0) {
                            // Prefer M4A/MP4 audio format
                            var selectedUrl: String? = null
                            var bestBitrate = -1
                            for (i in 0 until audioStreams.length()) {
                                val obj = audioStreams.getJSONObject(i)
                                val mimeType = obj.optString("mimeType", "")
                                val bitrate = obj.optInt("bitrate", -1)
                                if (mimeType.contains("audio/mp4") || mimeType.contains("m4a")) {
                                    if (bitrate > bestBitrate) {
                                        bestBitrate = bitrate
                                        selectedUrl = obj.optString("url")
                                    }
                                }
                            }
                            // Fallback to any audio stream if no mp4 audio found
                            if (selectedUrl == null) {
                                selectedUrl = audioStreams.getJSONObject(0).optString("url")
                            }
                            if (selectedUrl != null) {
                                return@withContext ExtractedStream(
                                    videoId = videoId,
                                    title = title,
                                    artist = artist,
                                    streamUrl = selectedUrl,
                                    isVideo = false,
                                    thumbnailUrl = thumbnailUrl
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fail silently and try the next instance in the resilient fallback loop
            }
        }
        return@withContext null
    }
}
