package com.example.musicplayer.network

import com.example.musicplayer.BuildConfig
import com.example.musicplayer.domain.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class YouTubeSearchService(private val client: OkHttpClient) {

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.YOUTUBE_API_KEY
        if (apiKey.isBlank() || apiKey == "your_key_here") return@withContext emptyList()

        val url = "https://www.googleapis.com/youtube/v3/search?" +
                "part=snippet&" +
                "maxResults=10&" +
                "q=${URLEncoder.encode(query, "UTF-8")}&" +
                "type=video&" +
                "videoCategoryId=10&" +
                "key=$apiKey"

        return@withContext try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()
            
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
                
                // Prefix YouTube ID for routing in MusicRepository
                val songId = "yt_$id"
                
                // Using a proxy for direct audio stream
                val audioUrl = "https://youtube-to-mp3-proxy.terasp.net/api/stream?id=$id" 

                songs.add(Song(songId, title, artist, image, audioUrl))
            }
            songs
        } catch (e: Exception) {
            emptyList()
        }
    }
}
