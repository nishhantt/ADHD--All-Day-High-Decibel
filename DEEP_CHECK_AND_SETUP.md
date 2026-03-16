# Deep check report + setup guide

## 1. Deep check results

### Search (YouTube API → results)

| Part | File | Status |
|------|------|--------|
| API key | `build.gradle` → `BuildConfig.YOUTUBE_API_KEY` from `local.properties` | OK (fixed duplicate `youtubeKey` decl.) |
| Search API | `YouTubeApiService.searchVideos(q, key)` | OK |
| Repository | `YouTubeRepositoryImpl.searchVideos()` → calls API, maps to `Video(id, title, thumbnailUrl)` | OK |
| Use case | `SearchUseCase.execute(query)` → repository | OK |
| ViewModel | `SearchViewModel.search(query)` → use case, sets `results` / `loading` | OK |
| UI | `SearchScreen`: TextField → debounce 400ms → search; shows `results`; tap → `onPlayVideo(video.id)` | OK |
| Navigation | `MainActivity`: SearchScreen `onPlayVideo = { videoId -> navigate("player/$videoId") }` | OK |

**Result:** Search is wired correctly. Same video id from the search result is passed to the player.

---

### Extraction (backend → playable URL)

| Part | File | Status |
|------|------|--------|
| Backend URL | `build.gradle` → `BuildConfig.EXTRACTOR_BACKEND_URL` from `local.properties` | OK |
| API contract | `ExtractorBackendApi`: GET `/stream?videoId=xxx` → `StreamResponse(url, title?, mimeType?)` | OK |
| Retrofit | `NetworkModule`: separate Retrofit with `EXTRACTOR_BACKEND_URL` (trailing slash), `@Named("extractorBackend")` | OK |
| Extractor | `BackendStreamExtractor`: if URL blank → dev sample; else call backend, on success return `PlayableMedia(res.url, ...)`, on failure → dev sample | OK |
| DI | `RepositoryModule`: `StreamExtractor` bound to `BackendStreamExtractor` | OK |
| Repository | `YouTubeRepositoryImpl.getPlayableStream(videoId)` → `extractor.getPlayableMedia(videoId)` then enriches with YouTube metadata (title, thumb) | OK |

**Result:** Extraction path is correct. Backend must return JSON `{ "url": "https://...", "title": "...", "mimeType": "..." }`.

---

### Play (stream URL → ExoPlayer)

| Part | File | Status |
|------|------|--------|
| Use case | `GetStreamUseCase.execute(videoId)` → `repository.getPlayableStream(videoId)` | OK |
| ViewModel | `PlayerViewModel.playVideo(videoId)`: getStreamUseCase.collect → onSuccess start service, playTrackUseCase.execute(playable), update UI state | OK |
| Service start | `startForegroundService(MusicPlayerService)` before emitting play command | OK |
| Repository | `PlayTrackUseCase` → `PlayerRepository.enqueueAndPlay(media)` (queue + emit Play command) | OK |
| Service | `MusicPlayerService` collects commands → `Play(cmd.media)` → `exoPlayerManager.prepareAndPlay(Uri.parse(media.uriString), ...)` | OK |
| ExoPlayer | `ExoPlayerManager.prepareAndPlay(uri, ...)` → setMediaItem, prepare, play | OK |

**Result:** Playing is wired correctly. The `uriString` from extraction (backend or fallback) is what gets played.

---

### Summary

- **Search:** Query → YouTube API → results → tap passes **videoId** to player. OK.
- **Extraction:** **videoId** → backend GET `/stream?videoId=xxx` → JSON with **url** → `PlayableMedia(uriString = url)`. If no backend or error → sample MP3. OK.
- **Play:** `PlayableMedia.uriString` → service → ExoPlayer. OK.

**Fixes applied:** Removed duplicate `def youtubeKey` in `build.gradle`. Improved Python extractor to pick an audio format with a direct URL more reliably.

---

## 2. Step-by-step setup guide (so the correct song plays every time)

### Step 1: Get a YouTube API key (for search)

1. Go to [Google Cloud Console](https://console.cloud.google.com/).
2. Create or select a project.
3. Enable **YouTube Data API v3** (APIs & Services → Library → search “YouTube Data API v3” → Enable).
4. Create credentials: APIs & Services → Credentials → Create credentials → **API key**.
5. Copy the API key.

### Step 2: Create `local.properties` in the project root

On your PC, in the folder `d:\desktopi\musicPlayer`, create or edit **`local.properties`** (same folder as `build.gradle`). Add:

```properties
YOUTUBE_API_KEY=paste_your_api_key_here
EXTRACTOR_BACKEND_URL=http://YOUR_PC_IP:8080/
```

- Replace `paste_your_api_key_here` with the key from Step 1.
- Replace `YOUR_PC_IP` with your PC’s IP (e.g. `192.168.1.10`). Your phone and PC must be on the same Wi‑Fi.
- Keep the trailing slash in `EXTRACTOR_BACKEND_URL`.

To find your PC IP (Windows): open Command Prompt or PowerShell and run `ipconfig`, then use the **IPv4 Address** of your Wi‑Fi adapter.

### Step 3: Run the extractor backend on your PC

1. Install Python 3 if needed.
2. Open Command Prompt or PowerShell and run:

   ```powershell
   pip install flask yt-dlp
   ```

3. Go to the project folder and start the server:

   ```powershell
   cd d:\desktopi\musicPlayer
   python extractor_server.py
   ```

4. You should see something like: `Running on http://0.0.0.0:8080`. Leave this window open while testing.

### Step 4: Allow the backend through Windows Firewall (if needed)

If the phone cannot reach the PC, allow Python on port 8080:

1. Windows Search → “Windows Defender Firewall” → “Allow an app or feature through Windows Defender Firewall”.
2. Click “Change settings” → “Allow another app” → Browse → select your Python executable (e.g. `C:\Users\...\AppData\Local\Programs\Python\...\python.exe`) or add “Python” if listed.
3. Ensure both Private and Public are checked, or add an inbound rule for TCP port 8080.

### Step 5: Build and install the app

1. Open the project in Android Studio (or use Docker build).
2. Build the APK: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
3. Install on your phone (copy `app/build/outputs/apk/debug/app-debug.apk` to the phone and open it, or use **Run** with the phone connected via USB).

### Step 6: Test on the phone

1. Open the **Music Player** app.
2. Allow **notifications** if asked.
3. In the search bar type a song (e.g. “baby justin bieber”).
4. Wait for results (from YouTube API).
5. Tap a result.

**Expected:** The app calls your backend with that video’s id; the backend returns the stream URL for that video; the app plays that song (correct audio for the result you tapped).

If you hear the sample MP3 instead: check that the backend is running, that `EXTRACTOR_BACKEND_URL` in `local.properties` matches your PC IP and port (with `/` at the end), that you rebuilt the APK after changing `local.properties`, and that the phone and PC are on the same network.
