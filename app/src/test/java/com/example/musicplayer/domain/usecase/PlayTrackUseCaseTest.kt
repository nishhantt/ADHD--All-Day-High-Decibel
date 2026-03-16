package com.example.musicplayer.domain.usecase

import com.example.musicplayer.data.FakePlayerRepository
import com.example.musicplayer.domain.models.PlayableMedia
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayTrackUseCaseTest {

    @Test
    fun `enqueueAndPlay emits play command and updates queue`() = runBlocking {
        val fakeRepo = FakePlayerRepository()
        val useCase = PlayTrackUseCase(fakeRepo)

        val media = PlayableMedia(uriString = "https://example.com/sample.mp3", title = "Sample")
        useCase.execute(media)

        // repository should have recorded the play command and queue updated
        assertEquals(1, fakeRepo.emittedCommands.size)
        val cmd = fakeRepo.emittedCommands[0]
        when (cmd) {
            is com.example.musicplayer.data.PlayerCommand.Play -> {
                assertEquals(media.uriString, cmd.media.uriString)
            }
            else -> throw AssertionError("Expected Play command")
        }
        val queued = fakeRepo.observeQueue().value
        assertEquals(1, queued.size)
        assertEquals(media.uriString, queued[0].uriString)
    }
}
