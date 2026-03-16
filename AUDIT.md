# Music Player – Project Audit

## 1. Current Project Architecture

- **UI**: Single-activity, Jetpack Compose, Navigation (search → player).
- **Presentation**: `SearchScreen` / `SearchViewModel` (search + tap to play), `PlayerScreen` / `PlayerViewModel` (playback state, controls).
- **Domain**: Use cases – `SearchUseCase`, `GetStreamUseCase`, `PlayTrackUseCase`, `ControlPlaybackUseCases`. Models: `Video`, `PlayableMedia`.
- **Data**: `YouTubeRepository` (search + stream URL), `PlayerRepository` (queue + commands), `StreamExtractor` (YouTube → playable URL). Dev implementation uses a fixed sample MP3.
- **Player**: `MusicPlayerService` (foreground, media buttons, notification), `ExoPlayerManager` (single ExoPlayer), `MediaNotificationManager`, `AudioFocusRequester`.
- **DI**: Hilt; `RepositoryModule`, `NetworkModule`, `DatabaseModule`. Room is present but not required for the minimal feature set.

Flow today: Search → tap → navigate to `PlayerScreen(videoId)` → `PlayerViewModel.playVideo(videoId)` → `GetStreamUseCase` → `PlayTrackUseCase` → `PlayerRepository.enqueueAndPlay()` → **no service running**, so commands are never handled.

---

## 2. What Files Do

| File | Role |
|------|------|
| `MainActivity.kt` | Sets Compose content, requests notification permission, defines `AppRoot()` with NavHost (search ↔ player). |
| `MusicApp.kt` | Hilt application. |
| `MusicPlayerService.kt` | Intended foreground playback: MediaSession, notification, audio focus, `observeCommands()` to play/pause/seek. **Missing**: `next()`, `previous()`, and service is never started. |
| `ExoPlayerManager.kt` | Builds one ExoPlayer, `prepareAndPlay`, play/pause/seek/release. |
| `MediaNotificationManager.kt` | Creates/updates/cancels playback notification with play/pause/next/prev/stop. |
| `AudioFocusRequester.kt` | Request/abandon audio focus for media. |
| `PlayerRepository.kt` | Queue + `PlayerCommand` (Play, Pause, Next, Previous, Seek, SetQueue). Emits commands via `SharedFlow` (replay=0 → first command can be missed). |
| `YouTubeRepository.kt` | Search via YouTube Data API, stream URL via `StreamExtractor`, enriches with metadata. |
| `StreamExtractor.kt` / `DevStreamExtractor.kt` | Interface + dev impl that returns a fixed sample MP3 (not real YouTube audio). |
| `PlayerViewModel.kt` | `playVideo(videoId)`, toggle play/pause, next/prev, repeat-one, seek; observes ExoPlayer; keeps local queue + index. |
| `PlayerScreen.kt` | Red vinyl-style UI, progress, controls; uses deprecated Coil API and invalid Accompanist import. |
| `SearchScreen.kt` / `SearchViewModel.kt` | Search field, debounce, results list, tap → `onPlayVideo(id)`. |
| `YouTubeApiService.kt` | Retrofit interface for YouTube Data API search/videos. |
| `DatabaseModule.kt` / Room DAOs/entities | Persistence layer; not needed for “core features only”. |

---

## 3. What Parts Are Correct

- Compose + Navigation + Hilt wiring.
- ExoPlayer/Media3 usage in `ExoPlayerManager` (prepare, play, pause, seek).
- Notification with play/pause/next/prev/stop and `MediaNotificationManager`.
- Audio focus and becoming-noisy receiver in the service.
- `PlayerRepository` command set (Play, Pause, Next, Previous, Seek).
- Search → stream → `PlayableMedia` → enqueueAndPlay flow (except service not started).
- Player UI concept (vinyl, controls, repeat) and repeat-one logic in ViewModel.
- Theme is dark-oriented (can be tightened to dark-only).

---

## 4. What Parts Are Broken or Incomplete

1. **Service never started**  
   Nothing calls `startService(MusicPlayerService)`. So `observeCommands().collect { }` never runs and Play/Pause/Next/Previous from the repository are never applied.

2. **`MusicPlayerService`: `next()` and `previous()` missing**  
   Notification and media buttons trigger `ACTION_NEXT` / `ACTION_PREV`, which call `next()` and `previous()` in the service, but those methods do not exist → runtime crash.

3. **First play command can be lost**  
   `PlayerRepository` uses `MutableSharedFlow(replay = 0)`. If the service starts after the first `Play` is emitted, the subscription misses that command.

4. **MainActivity missing import**  
   `Modifier.fillMaxSize()` is used without `import androidx.compose.foundation.layout.fillMaxSize` → compile error.

5. **PlayerScreen: invalid / deprecated APIs**  
   - `coil.compose.rememberImagePainter` is deprecated; Coil 2.x uses `rememberAsyncImagePainter` or `AsyncImage`.  
   - `com.google.accompanist.drawablepainter.rememberDrawablePainter` is used but Accompanist is not a dependency → compile error.

6. **PlayerScreen: missing import**  
   `collectAsState` is used but not imported from `androidx.compose.runtime`.

7. **Android 14+ foreground service**  
   Target SDK 34 requires `android:foregroundServiceType="mediaPlayback"` and `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission; otherwise `startForeground` can throw.

8. **No real YouTube audio**  
   `DevStreamExtractor` returns a static MP3. For “playback from YouTube audio stream” you need a real extractor (e.g. yt-dlp or similar); that is a separate, larger change.

9. **Duplicate queue / index**  
   Queue and “current” are in both `PlayerRepository` and `PlayerViewModel`; next/previous in the service need the queue and current index, which the service does not currently track.

10. **Unused / extra code**  
    Room, `DownloadWorker`, playlists, etc., are present but out of scope for the minimal list; they can be removed or left dormant.

---

## 5. What Needs to Be Fixed First

**Priority order:**

1. **Manifest**  
   Add `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `android:foregroundServiceType="mediaPlayback"` so the service can run as foreground on Android 14+.

2. **Start the service when playing**  
   When the user selects a song, start `MusicPlayerService` (e.g. from `PlayerViewModel` with `@ApplicationContext`) before or as you call `playTrackUseCase.execute(playable)`.

3. **Repository command replay**  
   Use `MutableSharedFlow<PlayerCommand>(replay = 1)` so the service, when it subscribes, receives the latest command (e.g. the initial Play).

4. **Implement `next()` and `previous()` in the service**  
   Service should track “current” media (or index), read queue from `PlayerRepository.observeQueue()`, and play the next or previous item (and call `play(media)`).

5. **Fix compile errors**  
   MainActivity: add `fillMaxSize` import. PlayerScreen: remove Accompanist import, switch to Coil’s `AsyncImage`/`rememberAsyncImagePainter`, add `collectAsState` import.

6. **Dark theme only**  
   Lock the app to dark theme (theme + optional `uiMode`).

7. **UI cleanup**  
   Remove “T” button and download button if present; keep search bar, vinyl-style player, and repeat (no shuffle).

8. **Optional**  
   Add a real YouTube stream extractor for “high quality audio stream” and improve error handling/retries (network errors, ExoPlayer errors).

---

## 6. Recommended Architecture (Minimal and Stable)

- **Single source of truth for playback**: Service owns ExoPlayer and interprets commands. Repository holds queue + “current index” (or service infers current from “last played” and queue).
- **UI only sends commands**: ViewModel starts the service, then calls use cases that push commands (and optionally set queue) to the repository; ViewModel can observe ExoPlayer (or service state) for position/isPlaying for UI only.
- **No queue UI** for minimal scope: queue is used only for next/previous and repeat-one.
- **Stream URL**: One `StreamExtractor` implementation (dev = sample URL; later = real YouTube audio) used by `GetStreamUseCase` / `YouTubeRepository`.

---

## 7. Implementation Order

1. Manifest: foreground service type + permission.  
2. PlayerRepository: `replay = 1` for commands; optionally expose or track current index if needed for next/prev.  
3. MusicPlayerService: implement `next()` and `previous()` using queue + current media/index; ensure notification and media buttons call them.  
4. PlayerViewModel: inject `@ApplicationContext`, start `MusicPlayerService` before `playTrackUseCase.execute(playable)`.  
5. MainActivity: add missing `fillMaxSize` import.  
6. PlayerScreen: fix Coil and remove Accompanist; add `collectAsState`; ensure dark, vinyl UI only (no T, no download).  
7. SearchScreen: dark theme, search bar at top.  
8. (Later) Replace `DevStreamExtractor` with a real YouTube audio extractor and improve error handling.

After these steps, the app should: play/pause/next/previous (including from notification and media buttons), repeat-one, search, and auto-play on selection, with stable background playback and a minimal dark UI.
