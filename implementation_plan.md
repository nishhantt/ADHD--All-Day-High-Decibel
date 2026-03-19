# Music Player Enhancements: Search, Controls, and Autoplay

This plan outlines the steps to complete the music player application, ensuring a seamless user experience from search to playback with full controls and autoplay functionality.

## Proposed Changes

### [Component] Audio Playback Engine

#### [MODIFY] [ExoPlayerManager.kt](file:///d:/desktopi/musicPlayer/app/src/main/java/com/example/musicplayer/player/ExoPlayerManager.kt)
- Update `playSong` to optionally support playlist preparation.
- Ensure state transitions are handled smoothly for autoplay.

#### [MODIFY] [MusicPlayerService.kt](file:///d:/desktopi/musicPlayer/app/src/main/java/com/example/musicplayer/player/MusicPlayerService.kt)
- Implement handling for playback control intents (`ACTION_TOGGLE_PLAY`, `ACTION_NEXT`, `ACTION_PREV`, `ACTION_STOP`).
- Ensure these intents correctly command the shared `ExoPlayer` instance via `ExoPlayerManager`.

---

### [Component] Presentation Layer (UI & Logic)

#### [MODIFY] [PlayerViewModel.kt](file:///d:/desktopi/musicPlayer/app/src/main/java/com/example/musicplayer/presentation/player/PlayerViewModel.kt)
- Add `playlist: List<Song>` and `currentIndex: Int` to manage the playback queue.
- Implement `next()` and `previous()` methods to navigate the playlist.
- update `playSong(song)` to handle playback of a selected song and potentially initialize the queue.
- Implement a periodic position update loop to expose `currentPosition` and `duration` to the UI.
- Add an `ExoPlayer.Listener` to automatically trigger `next()` when a song finishes (`STATE_ENDED`).

#### [MODIFY] [PlayerScreen.kt](file:///d:/desktopi/musicPlayer/app/src/main/java/com/example/musicplayer/presentation/player/PlayerScreen.kt)
- Connect the `SkipPrevious`, `Pause/Play`, and `SkipNext` UI buttons to the corresponding `PlayerViewModel` methods.
- Link the `Slider` (progress bar) to the `currentPosition` and `duration` provided by the `ViewModel`.
- Enable seeking by connecting `Slider`'s `onValueChange` to `viewModel.seekTo(ms)`.

#### [MODIFY] [SearchViewModel.kt](file:///d:/desktopi/musicPlayer/app/src/main/java/com/example/musicplayer/presentation/search/SearchViewModel.kt)
- Verify and fine-tune the search debounce logic (currently 500ms) to ensure "on each letter typed" behavior is responsive yet efficient.

## Verification Plan

### Automated Tests
- Run existing unit tests (e.g., `PlayerViewModelTest.kt`) after updating them to match the new constructor and logic.
- Command: `./gradlew test` (from the project root).

### Manual Verification
1. **Search Functionality**:
   - Open the app and tap the search bar.
   - Type a song name (e.g., "Blinding Lights") and verify that results appear dynamically.
   - Verify that the search clears when the 'X' button is tapped.
2. **Playback Controls**:
   - Select a song from the search results.
   - Verify the song starts playing in the `PlayerScreen`.
   - Test Play/Pause button.
   - Test Next/Previous buttons (verify they switch tracks if a playlist exists).
   - Test the Slider (verify it reflects current progress and allows seeking).
3. **Autoplay**:
   - Let a song finish and verify that the next song in the queue (if any) starts automatically.
4. **Foreground Service**:
   - Verify that the music continues playing when the app is in the background.
   - Verify that the notification shows the correct song info and has functional controls.
