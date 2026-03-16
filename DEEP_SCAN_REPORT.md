# Deep scan report – end-to-end behaviour and flaws

## 1. Where audio comes from (no YouTube audio extraction)

- **Search and metadata** use the **YouTube Data API** (needs `YOUTUBE_API_KEY` in `local.properties`):
  - `YouTubeApiService.searchVideos()` → search results (titles, thumbnails, video IDs).
  - `YouTubeApiService.getVideos()` → title/thumbnail for the selected video.
- **Audio is not from YouTube.**  
  Playback URL comes from `StreamExtractor`, which is bound to **DevStreamExtractor** in `RepositoryModule`.  
  DevStreamExtractor **ignores** `videoId` and always returns the same public sample MP3:
  - `https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_700KB.mp3`
- So: **every** tapped “song” plays that one file. Titles/thumbnails are from the API; sound is always the sample. There is no YouTube audio extraction in the app.

---

## 2. End-to-end flow (by feature)

| Feature | Flow | Status / flaw |
|--------|------|----------------|
| **Play** | Search → tap → ViewModel starts service → getStreamUseCase → PlayTrackUseCase → repo emits Play → service collects → ExoPlayerManager.prepareAndPlay(uri) | Works. Audio is sample MP3. |
| **Pause** | UI/notification → repo Pause → service pause() → ExoPlayerManager.pause() | Works. |
| **Resume** | UI/notification → repo Resume → service playCurrentFromQueueIfAny() → player.play() | Works. |
| **Next** | UI → ViewModel.next() → repo Next → service next() → play(queue[nextIdx]). Notification/Bluetooth → service next() from intent. | Works. **Flaw:** Repeat-one is not applied when next is triggered from notification/Bluetooth (see below). |
| **Previous** | Same as next, service previous() → play(queue[prevIdx]). | Works. Same repeat-one caveat. |
| **Repeat one** | ViewModel.repeatOne; only applied when user taps **in-app** Next – ViewModel replays current via ExoPlayerManager. When user taps Next on **notification** or **Bluetooth**, service next() runs and does **not** know repeat-one, so it goes to next track. | **Flaw:** Repeat-one is UI-only; notification/Bluetooth next ignores it. |
| **Seek** | UI Slider → viewModel.seekTo(ms) → ExoPlayerManager.seekTo(). Repo Seek → service seekTo(). | Works. |
| **Auto-play on select** | LaunchedEffect(videoId) → playVideo(videoId) → stream + start service + enqueueAndPlay. | Works. |
| **Background playback** | Service runs as foreground with notification; ExoPlayer in service. | Works. |
| **Notification controls** | MediaNotificationManager creates notification with PendingIntents that start service with ACTION_TOGGLE_PLAY / NEXT / PREV / STOP. onStartCommand handles them. | Works. |
| **Bluetooth / car controls** | MediaSession is created in service and bound to ExoPlayer. On Android 8+, media keys are routed to the active MediaSession, so play/pause/next/prev from Bluetooth/car should work. Service has MEDIA_BUTTON intent-filter for legacy path. | Expected to work. Not explicitly tested in code. |
| **Audio focus** | AudioFocusRequester in service; request on play, abandon on pause. Becoming-noisy receiver pauses when headphones unplugged. | Works. |
| **Search** | SearchViewModel → SearchUseCase → YouTubeRepository.searchVideos() (API). Results shown; tap → onPlayVideo(id). | Works **if** YOUTUBE_API_KEY is set. If key missing/invalid, API fails → Result.failure → results = emptyList(); **no error message** to user. |

---

## 3. Flaws found

1. **Audio source**  
   All playback uses the same sample MP3. No YouTube audio extraction. YouTube API is used only for search and metadata.

2. **Repeat-one only in UI**  
   When Next is pressed from notification or Bluetooth, the service runs next() and does not respect repeat-one (that state lives only in ViewModel). So repeat-one applies only to the in-app Next button.

3. **Service: listener and collector run every onStartCommand**  
   - `player.addListener(...)` is called on **every** onStartCommand (each notification tap or intent). Listeners are never removed → **multiple listeners** over time and duplicate callbacks.  
   - `lifecycleScope.launch { playerRepository.observeCommands().collect { ... } }` is also started every onStartCommand → **multiple concurrent collectors**. SharedFlow replays to all; behaviour can be redundant or confusing.

4. **No user feedback when search fails**  
   If YouTube API key is missing or invalid, search returns failure and ViewModel sets `results = emptyList()`. User sees no results and no message (e.g. “Search failed” or “Check API key”).

5. **ViewModel and service queue can diverge**  
   ViewModel keeps its own `queue` and `currentIndex`. When user uses notification next/prev, service updates playback and its `currentMedia`, but ViewModel state (e.g. displayed title) is not updated from service events, so **UI can show the previous track** until the user interacts in-app again.

6. **Repeat state not visible in UI**  
   `toggleRepeat()` flips `repeatOne` but there is no visual (e.g. tint or icon change) to show repeat-one is on.

7. **MEDIA_BUTTON and Bluetooth**  
   Media buttons are handled via MediaSession (and, for legacy, the service intent-filter). Bluetooth/car control should work when the session is active; no automated test or explicit media-button handling code path was verified.

---

## 4. Summary

- **Play / Pause / Next / Previous / Seek:** Implemented and wired; work from UI and notification.  
- **Audio:** Always the same sample MP3; **YouTube API is not used to extract audio**, only for search and metadata.  
- **Bluetooth / car:** Should work via MediaSession; no extra test.  
- **Repeat-one:** Works only from the in-app Next button; notification/Bluetooth Next ignores it.  
- **Reliability:** Service adds a new listener and a new command collector on every onStartCommand; search gives no error when the API key is missing or invalid.

Fix order suggested: (1) ~~Move listener and command collector to one-time setup~~ **DONE** – guard `listenerAndCollectorStarted` in MusicPlayerService so listener and collector run only once; (2) Optionally surface repeat state in the service so notification/Bluetooth can respect it; (3) Show a search error when the API fails; (4) Optionally sync UI with service playback (e.g. observe service/repo current media).
