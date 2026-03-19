package com.example.musicplayer.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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
            "https://api.piped.vicr123.com",
            "https://pipedapi.moomoo.me",
            "https://piped-api.lunar.icu"
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
                    
                    // Priority 1: audioStreams
                    val audioStreams = json.optJSONArray("audioStreams")
                    if (audioStreams != null && audioStreams.length() > 0) {
                        var bestUrl = ""
                        var bestBitrate = -1
                        
                        for (i in 0 until audioStreams.length()) {
                            val stream = audioStreams.getJSONObject(i)
                            val bitrate = stream.optInt("bitrate", 128000)
                            if (bitrate > bestBitrate) {
                                bestBitrate = bitrate
                                bestUrl = stream.getString("url")
                            }
                        }
                        if (bestUrl.isNotEmpty()) return@withContext bestUrl
                    }

                    // Priority 2: videoStreams (extract audio from video if audioOnly is missing)
                    val videoStreams = json.optJSONArray("videoStreams")
                    if (videoStreams != null && videoStreams.length() > 0) {
                        for (i in 0 until videoStreams.length()) {
                            val stream = videoStreams.getJSONObject(i)
                            if (stream.optBoolean("videoOnly") == false) {
                                return@withContext stream.getString("url")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Extraction failed for instance $instance: ${e.message}")
            }
        }

        // Final fallback: Reliable fast proxy
        "https://youtube-to-mp3-proxy.terasp.net/api/stream?id=$videoId"
    }
}
