package com.example.musicplayer.network

import android.util.Log
import com.example.musicplayer.BuildConfig
import com.example.musicplayer.domain.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class SaavnService(private val client: OkHttpClient) {

    /**
     * Brainstormed Fix: 
     * 1. Public APIs like saavn.dev block mobile apps via Cloudflare (Returning HTML instead of JSON).
     * 2. The solution is to proxy the request through YOUR OWN backend (Render).
     * 3. This implementation tries the backend proxy first, then falls back to a direct call with fixed headers.
     */
    suspend fun searchSongs(query: String): List<Song> {
        return withContext(Dispatchers.IO) {
            val cleanQuery = query.trim().replace("\n", "")
            if (cleanQuery.isEmpty()) return@withContext emptyList()

            val sanitizedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            
            // Try Backend Proxy First (Recommended Approach)
            val backendBase = BuildConfig.EXTRACTOR_BACKEND_URL.trimEnd('/')
            if (backendBase.isNotBlank() && backendBase.contains("http")) {
                val backendUrl = "$backendBase/saavn/search?query=$sanitizedQuery"
                Log.d("SaavnAPI", "Trying backend proxy: $backendUrl")
                val results = fetchAndParse(backendUrl, isDirect = false)
                if (results.isNotEmpty()) return@withContext results
            }

            // Fallback: Direct Call with Working API (saavn.sumit.co)
            val directUrl = "https://saavn.sumit.co/api/search/songs?query=$sanitizedQuery"
            Log.d("SaavnAPI", "Direct call to working API: $directUrl")
            fetchAndParse(directUrl, isDirect = true)
        }
    }

    suspend fun getSongDetails(id: String): Song? = withContext(Dispatchers.IO) {
        val url = "https://saavn.sumit.co/api/songs?id=$id"
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val results = json.optJSONArray("data") ?: return@withContext null
            if (results.length() > 0) {
                parseSongItem(results.getJSONObject(0))
            } else null
        } catch (e: Exception) {
            Log.e("SaavnAPI", "Failed to fetch song details for $id", e)
            null
        }
    }

    private fun fetchAndParse(url: String, isDirect: Boolean): List<Song> {
        val requestBuilder = Request.Builder().url(url)
        
        if (isDirect) {
            requestBuilder.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
            requestBuilder.addHeader("Accept", "application/json")
        }

        return try {
            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string() ?: return emptyList()

            if (body.trim().startsWith("<")) {
                Log.e("SaavnAPI", "Blocked by Cloudflare/Bot detection (HTML returned)")
                return emptyList()
            }

            val json = JSONObject(body)
            // Handle new structure: { success: true, data: { results: [...] } }
            val data = json.optJSONObject("data") ?: json
            val results = data.optJSONArray("results") ?: json.optJSONArray("data") ?: return emptyList()

            val songs = mutableListOf<Song>()
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)

                // Safe extraction of 320kbps URL
                val downloadUrls = item.getJSONArray("downloadUrl")
                val audioUrl = if (downloadUrls.length() > 4) {
                    downloadUrls.getJSONObject(4).getString("url")
                } else {
                    downloadUrls.getJSONObject(downloadUrls.length() - 1).getString("url")
                }

                // Safe extraction of Image
                val images = item.getJSONArray("image")
                val image = if (images.length() > 2) {
                    images.getJSONObject(2).getString("url")
                } else {
                    images.getJSONObject(images.length() - 1).getString("url")
                }

                // Extract artist name from artists.primary[0].name
                var artist = "Unknown Artist"
                try {
                    val artistsObj = item.optJSONObject("artists")
                    if (artistsObj != null) {
                        val primaryArtists = artistsObj.optJSONArray("primary")
                        if (primaryArtists != null && primaryArtists.length() > 0) {
                            artist = primaryArtists.getJSONObject(0).getString("name")
                        }
                    } else {
                        // Fallback for older API versions
                        artist = item.optString("primaryArtists", "Unknown Artist")
                    }
                } catch (e: Exception) {
                    Log.w("SaavnAPI", "Could not parse artist for ${item.optString("name")}")
                }

                songs.add(
                    Song(
                        id = item.getString("id"),
                        title = item.getString("name"),
                        artist = artist,
                        image = image,
                        audioUrl = audioUrl
                    )
                )
            }
            songs
        } catch (e: Exception) {
            Log.e("SaavnAPI", "Request failed for $url", e)
            emptyList()
        }
    }

    suspend fun globalSearch(query: String): com.example.musicplayer.domain.models.SearchResult = withContext(Dispatchers.IO) {
        val sanitizedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://saavn.sumit.co/api/search?query=$sanitizedQuery"
        
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext com.example.musicplayer.domain.models.SearchResult()
            
            val json = JSONObject(body)
            val data = json.getJSONObject("data")
            
            val songs = parseSearchSongs(data.optJSONObject("songs")?.optJSONArray("results"))
            val albums = parseSearchAlbums(data.optJSONObject("albums")?.optJSONArray("results"))
            val artists = parseSearchArtists(data.optJSONObject("artists")?.optJSONArray("results"))
            
            com.example.musicplayer.domain.models.SearchResult(
                topResult = songs.firstOrNull(),
                songs = songs,
                albums = albums,
                artists = artists
            )
        } catch (e: Exception) {
            Log.e("SaavnAPI", "Global search failed", e)
            com.example.musicplayer.domain.models.SearchResult()
        }
    }

    private fun parseSearchSongs(results: org.json.JSONArray?): List<Song> {
        if (results == null) return emptyList()
        val songs = mutableListOf<Song>()
        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)
            songs.add(
                Song(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    artist = item.optString("primaryArtists", "Unknown Artist"),
                    image = parseSearchImageUrl(item.optJSONArray("image")),
                    audioUrl = "" // Stream URL needs song-specific call
                )
            )
        }
        return songs
    }

    private fun parseSearchAlbums(results: org.json.JSONArray?): List<com.example.musicplayer.domain.models.Album> {
        if (results == null) return emptyList()
        val albums = mutableListOf<com.example.musicplayer.domain.models.Album>()
        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)
            albums.add(
                com.example.musicplayer.domain.models.Album(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    artist = item.optString("artist", "Various Artists"),
                    image = parseSearchImageUrl(item.optJSONArray("image")),
                    url = item.optString("url")
                )
            )
        }
        return albums
    }

    private fun parseSearchArtists(results: org.json.JSONArray?): List<com.example.musicplayer.domain.models.Artist> {
        if (results == null) return emptyList()
        val artists = mutableListOf<com.example.musicplayer.domain.models.Artist>()
        for (i in 0 until results.length()) {
            val item = results.getJSONObject(i)
            artists.add(
                com.example.musicplayer.domain.models.Artist(
                    id = item.getString("id"),
                    name = item.getString("title"),
                    image = parseSearchImageUrl(item.optJSONArray("image")),
                    url = item.optString("url")
                )
            )
        }
        return artists
    }

    private fun parseSearchImageUrl(images: org.json.JSONArray?): String {
        if (images == null || images.length() == 0) return ""
        return images.getJSONObject(images.length() - 1).getString("url")
    }

    suspend fun getAlbumDetails(albumId: String): List<Song> = withContext(Dispatchers.IO) {
        val url = "https://saavn.sumit.co/api/albums?id=$albumId"
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            val songsJson = json.getJSONObject("data").getJSONArray("songs")
            
            val songs = mutableListOf<Song>()
            for (i in 0 until songsJson.length()) {
                val item = songsJson.getJSONObject(i)
                songs.add(parseSongItem(item))
            }
            songs
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getArtistSongs(artistId: String): List<Song> = withContext(Dispatchers.IO) {
        val url = "https://saavn.sumit.co/api/artists?id=$artistId"
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            val songsJson = json.getJSONObject("data").getJSONArray("songs")
            
            val songs = mutableListOf<Song>()
            for (i in 0 until songsJson.length()) {
                val item = songsJson.getJSONObject(i)
                songs.add(parseSongItem(item))
            }
            songs
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseSongItem(item: JSONObject): Song {
        val downloadUrls = item.getJSONArray("downloadUrl")
        val audioUrl = downloadUrls.getJSONObject(downloadUrls.length() - 1).getString("url")
        val images = item.getJSONArray("image")
        val image = images.getJSONObject(images.length() - 1).getString("url")
        
        return Song(
            id = item.getString("id"),
            title = item.getString("name"),
            artist = item.getJSONObject("artists").getJSONArray("primary").getJSONObject(0).getString("name"),
            image = image,
            audioUrl = audioUrl
        )
    }
}
