package com.example.musicplayer.network

import android.util.Log
import com.example.musicplayer.domain.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneBackendService @Inject constructor(
    private val client: OkHttpClient
) {
    private val TAG = "PhoneBackend"
    
    private var BASE_URL = "http://127.0.0.1:7860"
    
    private val searchCache = mutableMapOf<String, List<Song>>()
    private val urlCache = mutableMapOf<String, String>()
    private val cacheMutex = Mutex()
    private val inFlightRequests = mutableSetOf<String>()
    private val requestMutex = Mutex()
    
    fun setBaseUrl(url: String) {
        BASE_URL = url
    }
    
    fun getBaseUrl(): String = BASE_URL
    
    private fun getUrlCacheKey(songId: String, artist: String, title: String): String {
        return "$songId|$artist|$title"
    }
    
    suspend fun searchSongs(query: String, limit: Int = 25): List<Song> = withContext(Dispatchers.IO) {
        val cacheKey = query.lowercase().trim()
        
        searchCache[cacheKey]?.let { cached ->
            Log.d(TAG, "Search cache hit: $query")
            return@withContext cached
        }
        
        requestMutex.withLock {
            if (inFlightRequests.contains(cacheKey)) {
                return@withContext searchCache[cacheKey] ?: emptyList()
            }
            inFlightRequests.add(cacheKey)
        }
        
        try {
            val url = "$BASE_URL/api/mobile/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            
            client.newCall(request).execute().use { response ->
                requestMutex.withLock { inFlightRequests.remove(cacheKey) }
                
                if (!response.isSuccessful) {
                    Log.e(TAG, "Search failed: ${response.code}")
                    return@withContext emptyList()
                }
                
                val body = response.body?.string() ?: return@withContext emptyList()
                
                val jsonArray = org.json.JSONArray(body)
                val songs = mutableListOf<Song>()
                
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val coverXl = item.optString("cover_xl", item.optString("cover", ""))
                    
                    songs.add(Song(
                        id = item.optString("id", ""),
                        title = item.optString("title", "Unknown"),
                        artist = item.optString("artist", "Unknown"),
                        image = coverXl,
                        audioUrl = ""
                    ))
                }
                
                Log.d(TAG, "Found ${songs.size} songs")
                
                if (searchCache.size > 100) {
                    val keysToRemove = searchCache.keys.take(50).toList()
                    keysToRemove.forEach { searchCache.remove(it) }
                }
                searchCache[cacheKey] = songs
                return@withContext songs
            }
        } catch (e: Exception) {
            requestMutex.withLock { inFlightRequests.remove(cacheKey) }
            Log.e(TAG, "Search error: ${e.message}")
            return@withContext emptyList()
        }
    }
    
    suspend fun getStreamUrl(songId: String, artist: String, title: String): String? = withContext(Dispatchers.IO) {
        val cacheKey = getUrlCacheKey(songId, artist, title)
        
        urlCache[cacheKey]?.let { cachedUrl ->
            Log.d(TAG, "URL cache hit: $title")
            return@withContext cachedUrl
        }
        
        val url = "$BASE_URL/api/mobile/play_fast?id=$songId&artist=${java.net.URLEncoder.encode(artist, "UTF-8")}&title=${java.net.URLEncoder.encode(title, "UTF-8")}"
        
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Play failed: ${response.code}")
                    return@withContext null
                }
                
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                
                if (json.has("error")) {
                    return@withContext null
                }
                
                val source = json.optString("source", "")
                val streamUrl = when (source) {
                    "local" -> json.optString("url", "")
                    else -> json.optString("url", "").ifBlank { json.optString("direct_url", "") }
                }
                
                if (streamUrl.isNotBlank()) {
                    urlCache[cacheKey] = streamUrl
                    Log.d(TAG, "Cached URL for: $title")
                }
                
                return@withContext streamUrl.ifBlank { null }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Stream URL error: ${e.message}")
            return@withContext null
        }
    }
    
    suspend fun prefetchUrls(songs: List<Song>) = withContext(Dispatchers.IO) {
        songs.take(5).forEach { song ->
            val cacheKey = getUrlCacheKey(song.id, song.artist, song.title)
            
            if (urlCache.containsKey(cacheKey)) {
                return@forEach
            }
            
            launch(Dispatchers.IO) {
                try {
                    val url = "$BASE_URL/api/mobile/play_fast?id=${song.id}&artist=${java.net.URLEncoder.encode(song.artist, "UTF-8")}&title=${java.net.URLEncoder.encode(song.title, "UTF-8")}"
                    
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0")
                        .build()
                    
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (body != null) {
                                val json = JSONObject(body)
                                val streamUrl = json.optString("url", "").ifBlank { json.optString("direct_url", "") }
                                if (streamUrl.isNotBlank()) {
                                    urlCache[cacheKey] = streamUrl
                                    Log.d(TAG, "Prefetched: ${song.title}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Silent fail for prefetch
                }
            }
        }
    }
    
    suspend fun getRecommendations(songId: String): List<Song> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/api/mobile/recommend?song_id=$songId"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                
                val songs = mutableListOf<Song>()
                val seenIds = mutableSetOf<String>()
                
                listOf("behavior_based", "content_based").forEach { key ->
                    val arr = json.optJSONArray(key)
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val id = item.optString("id", "")
                            if (id.isNotBlank() && id !in seenIds) {
                                seenIds.add(id)
                                songs.add(Song(
                                    id = id,
                                    title = item.optString("title", "Unknown"),
                                    artist = item.optString("artist", "Unknown"),
                                    image = item.optString("cover_xl", item.optString("cover", "")),
                                    audioUrl = ""
                                ))
                            }
                        }
                    }
                }
                return@withContext songs
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recommendations error: ${e.message}")
            return@withContext emptyList()
        }
    }
    
    suspend fun getUpNext(songId: String, limit: Int = 10): List<Song> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/api/mobile/up_next?song_id=$songId&limit=$limit"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                
                val body = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = org.json.JSONArray(body)
                val songs = mutableListOf<Song>()
                
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    songs.add(Song(
                        id = item.optString("id", ""),
                        title = item.optString("title", "Unknown"),
                        artist = item.optString("artist", "Unknown"),
                        image = item.optString("cover_xl", item.optString("cover", "")),
                        audioUrl = ""
                    ))
                }
                return@withContext songs
            }
        } catch (e: Exception) {
            Log.e(TAG, "Up next error: ${e.message}")
            return@withContext emptyList()
        }
    }
    
    suspend fun getChart(): List<Song> = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/api/mobile/chart"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                
                val body = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = org.json.JSONArray(body)
                val songs = mutableListOf<Song>()
                
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    songs.add(Song(
                        id = item.optString("id", ""),
                        title = item.optString("title", "Unknown"),
                        artist = item.optString("artist", "Unknown"),
                        image = item.optString("cover_xl", item.optString("cover", "")),
                        audioUrl = ""
                    ))
                }
                return@withContext songs
            }
        } catch (e: Exception) {
            Log.e(TAG, "Chart error: ${e.message}")
            return@withContext emptyList()
        }
    }
    
    suspend fun updateTransition(previousSongId: String?, currentSongId: String) {
        if (previousSongId.isNullOrBlank() || currentSongId.isBlank()) return
        try {
            val url = "$BASE_URL/api/mobile/play?previous_song_id=$previousSongId&id=$currentSongId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { }
        } catch (e: Exception) {
            // Silent fail
        }
    }
    
    fun clearUrlCache() {
        urlCache.clear()
    }
}
