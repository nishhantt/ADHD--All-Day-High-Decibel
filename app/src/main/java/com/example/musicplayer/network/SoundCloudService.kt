package com.example.musicplayer.network

import com.example.musicplayer.domain.models.Song
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundCloudService @Inject constructor(
    private val client: OkHttpClient
) {
    private val SEARCH_URL = "https://api-v2.soundcloud.com/search/tracks?q=%s&client_id=LBCcHmS96G6h0ST69X2WpC9fK5V6GvB5&limit=10"

    suspend fun search(query: String): List<Song> {
        return try {
            val url = String.format(SEARCH_URL, query)
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return emptyList()
            
            val json = JSONObject(body)
            val tracks = json.getJSONArray("collection")
            val songs = mutableListOf<Song>()
            
            for (i in 0 until tracks.length()) {
                val track = tracks.getJSONObject(i)
                songs.add(
                    Song(
                        id = "sc_${track.getLong("id")}",
                        title = track.getString("title"),
                        artist = track.getJSONObject("user").getString("username"),
                        image = track.optString("artwork_url", "").replace("large", "t500x500"),
                        audioUrl = "" // Trigger resolver in PlayerViewModel
                    )
                )
            }
            songs
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun resolveStreamUrl(trackId: String): String {
        return try {
            "https://soundcloud-stream-proxy.terasp.net/stream?id=$trackId&client_id=$CLIENT_ID"
        } catch (e: Exception) {
            ""
        }
    }
}
