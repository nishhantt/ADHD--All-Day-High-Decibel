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
        val pipedInstances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.ducks.party",
            "https://pipedapi-libre.kavin.rocks",
            "https://api.piped.vicr123.com"
        )

        for (instance in pipedInstances) {
            try {
                val url = "$instance/streams/$videoId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    val audioStreams = json.getJSONArray("audioStreams")
                    
                    // Prefer M4A/WebM audio with highest bitrate
                    var bestUrl = ""
                    var bestBitrate = 0
                    
                    for (i in 0 until audioStreams.length()) {
                        val stream = audioStreams.getJSONObject(i)
                        val bitrate = stream.getInt("bitrate")
                        if (bitrate > bestBitrate) {
                            bestBitrate = bitrate
                            bestUrl = stream.getString("url")
                        }
                    }
                    
                    if (bestUrl.isNotEmpty()) {
                        Log.d(TAG, "Extracted Stream URL from $instance for $videoId: $bestUrl")
                        return@withContext bestUrl
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Extraction failed for instance $instance: ${e.message}")
            }
        }

        // Final fallback if all Piped instances fail
        "https://youtube-to-mp3-proxy.terasp.net/api/stream?id=$videoId"
    }
}
