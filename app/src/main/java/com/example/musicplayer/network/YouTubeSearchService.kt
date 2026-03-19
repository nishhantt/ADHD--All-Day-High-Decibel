package com.example.musicplayer.network

import com.example.musicplayer.domain.models.Song
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeSearchService @Inject constructor(
    private val client: OkHttpClient,
    private val extractor: YouTubeExtractor
) {
    private val API_KEY = "AIzaSyDK1Q5Xl0IroijWh0TxBrXyVMGP17bmi_c"
    private val SEARCH_URL = "https://www.googleapis.com/youtube/v3/search?part=snippet&maxResults=10&q=%s&type=video&videoCategoryId=10&key=$API_KEY"

    suspend fun searchSongs(query: String): List<Song> {
        return try {
            val url = String.format(SEARCH_URL, query)
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()
            
            val json = JSONObject(body)
            val items = json.getJSONArray("items")
            val songs = mutableListOf<Song>()
            
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val id = item.getJSONObject("id").getString("videoId")
                val snippet = item.getJSONObject("snippet")
                val title = snippet.getString("title")
                val artist = snippet.getString("channelTitle")
                val thumbnails = snippet.getJSONObject("thumbnails")
                val image = thumbnails.getJSONObject("high").getString("url")
                
                // Prefix YouTube ID for routing
                val songId = "yt_$id"
                
                // We resolve the stream URL on-demand in MusicRepository or PlayerViewModel
                songs.add(Song(songId, title, artist, image, ""))
            }
            songs
        } catch (e: Exception) {
            emptyList()
        }
    }
}
