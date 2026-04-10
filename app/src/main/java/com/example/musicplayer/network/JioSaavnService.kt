package com.example.musicplayer.network

import android.util.Log
import com.example.musicplayer.domain.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JioSaavnService @Inject constructor(
    private val client: OkHttpClient
) {
    private val TAG = "JioSaavn"
    
    private val API_BASE = "https://jiosaavn-api-privatecvc2.vercel.app"
    
    suspend fun searchSongs(query: String, limit: Int = 20): List<Song> = withContext(Dispatchers.IO) {
        try {
            val url = "$API_BASE/search/songs?query=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=$limit"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android 14) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Search failed: ${response.code}")
                    return@withContext emptyList()
                }
                
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                
                if (json.optString("status") != "SUCCESS") {
                    Log.e(TAG, "API error: ${json.optString("message")}")
                    return@withContext emptyList()
                }
                
                val results = json.getJSONObject("data").getJSONArray("results")
                val songs = mutableListOf<Song>()
                
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    
                    // Get highest quality audio URL (320kbps is best)
                    val downloadUrls = item.getJSONArray("downloadUrl")
                    var audioUrl = ""
                    
                    // Try to get 320kbps first
                    for (j in 0 until downloadUrls.length()) {
                        val dl = downloadUrls.getJSONObject(j)
                        if (dl.getString("quality") == "320kbps") {
                            audioUrl = dl.getString("link")
                            break
                        }
                    }
                    
                    // Fallback to highest available quality
                    if (audioUrl.isEmpty() && downloadUrls.length() > 0) {
                        audioUrl = downloadUrls.getJSONObject(downloadUrls.length() - 1).getString("link")
                    }
                    
                    // Get best image (500x500 preferred)
                    val images = item.getJSONArray("image")
                    var imageUrl = ""
                    
                    for (j in 0 until images.length()) {
                        val img = images.getJSONObject(j)
                        if (img.getString("quality") == "500x500") {
                            imageUrl = img.getString("link")
                            break
                        }
                    }
                    
                    if (imageUrl.isEmpty() && images.length() > 0) {
                        imageUrl = images.getJSONObject(images.length() - 1).getString("link")
                    }
                    
                    val song = Song(
                        id = item.getString("id"),
                        title = item.optString("name", "Unknown"),
                        artist = item.optString("primaryArtists", "Unknown"),
                        image = imageUrl,
                        audioUrl = audioUrl
                    )
                    
                    if (song.audioUrl.isNotEmpty()) {
                        songs.add(song)
                    }
                }
                
                Log.d(TAG, "Found ${songs.size} songs for: $query")
                return@withContext songs
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search error: ${e.message}")
            return@withContext emptyList()
        }
    }
}
