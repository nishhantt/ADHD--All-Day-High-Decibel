package com.example.musicplayer.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class YouTubeExtractor @Inject constructor(
    private val client: OkHttpClient
) {
    private val TAG = "YouTubeExtractor"

    suspend fun extractStreamUrl(videoId: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return@withContext ""

            // Find ytInitialPlayerResponse in the HTML
            val regex = Regex("var ytInitialPlayerResponse = (\\{.*?\\});")
            val match = regex.find(html)
            val jsonStr = match?.groupValues?.get(1) ?: return@withContext ""

            val playerResponse = JSONObject(jsonStr)
            val streamingData = playerResponse.getJSONObject("streamingData")
            val formats = streamingData.getJSONArray("adaptiveFormats")

            // Look for best audio-only format (usually opus or mp4a)
            var bestUrl = ""
            var bestBitrate = 0
            
            for (i in 0 until formats.length()) {
                val format = formats.getJSONObject(i)
                val mimeType = format.getString("mimeType")
                if (mimeType.contains("audio")) {
                    val bitrate = format.getInt("bitrate")
                    if (bitrate > bestBitrate) {
                        bestBitrate = bitrate
                        bestUrl = format.optString("url")
                    }
                }
            }

            if (bestUrl.isBlank()) {
                bestUrl = "https://youtube-to-mp3-proxy.terasp.net/api/stream?id=$videoId"
            }

            Log.d(TAG, "Extracted Stream URL for $videoId: $bestUrl")
            bestUrl
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed", e)
            "https://youtube-to-mp3-proxy.terasp.net/api/stream?id=$videoId"
        }
    }
}
