package com.example.musicplayer.presentation.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.example.musicplayer.data.FakePlayerRepository
import com.example.musicplayer.data.YouTubeRepository
import com.example.musicplayer.domain.models.PlayableMedia
import com.example.musicplayer.domain.usecase.ControlPlaybackUseCases
import com.example.musicplayer.domain.usecase.GetStreamUseCase
import com.example.musicplayer.domain.usecase.PlayTrackUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito

class PlayerViewModelTest {

    private class FakeYouTubeRepo(val playable: PlayableMedia) : YouTubeRepository {
        override fun searchVideos(query: String): Flow<Result<List<com.example.musicplayer.domain.models.Video>>> = flow { emit(Result.success(emptyList())) }
        override fun getPlayableStream(videoId: String): Flow<Result<PlayableMedia>> = flow { emit(Result.success(playable)) }
    }

    @Test
    fun `playVideo enqueues and updates repository`() = runBlocking {
        val fakeRepo = FakePlayerRepository()
        val playable = PlayableMedia(uriString = "https://example.com/sample.mp3", title = "Test Title", thumbnailUrl = null)
        val yt = FakeYouTubeRepo(playable)
        val getStream = GetStreamUseCase(yt)
        val playTrack = PlayTrackUseCase(fakeRepo)
        val control = ControlPlaybackUseCases(fakeRepo)
        val context = Mockito.mock(Context::class.java)
        val exoMock = Mockito.mock(com.example.musicplayer.player.ExoPlayerManager::class.java)
        val vm = PlayerViewModel(context, exoMock, getStream, playTrack, control, SavedStateHandle(), enableObservers = false)

        val job = vm.playVideo("vid123")
        job.join()

        // ensure repository received Play command
        assertEquals(1, fakeRepo.emittedCommands.size)
        val cmd = fakeRepo.emittedCommands[0]
        assert(cmd is com.example.musicplayer.data.PlayerCommand.Play)
        val media = (cmd as com.example.musicplayer.data.PlayerCommand.Play).media
        assertEquals(playable.uriString, media.uriString)

        // ensure ViewModel UI state was updated
        val state = vm.uiState.value
        assert(state is PlayerUiState.Playing)
        val playing = state as PlayerUiState.Playing
        assertEquals(playable.title, playing.media.title)
    }
}
