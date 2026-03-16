package com.example.musicplayer.domain.usecase

import com.example.musicplayer.data.FakePlayerRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ControlPlaybackUseCasesTest {

    @Test
    fun `play pause next previous seek emit corresponding commands`() = runBlocking {
        val fakeRepo = FakePlayerRepository()
        val useCases = ControlPlaybackUseCases(fakeRepo)

        useCases.play()
        useCases.pause()
        useCases.next()
        useCases.previous()
        useCases.seekTo(12345L)

        // five commands should have been emitted
        assertEquals(5, fakeRepo.emittedCommands.size)
        // simple type checks
        assertEquals(com.example.musicplayer.data.PlayerCommand.Resume::class, fakeRepo.emittedCommands[0]::class)
        assertEquals(com.example.musicplayer.data.PlayerCommand.Pause::class, fakeRepo.emittedCommands[1]::class)
        assertEquals(com.example.musicplayer.data.PlayerCommand.Next::class, fakeRepo.emittedCommands[2]::class)
        assertEquals(com.example.musicplayer.data.PlayerCommand.Previous::class, fakeRepo.emittedCommands[3]::class)
        assertEquals(com.example.musicplayer.data.PlayerCommand.Seek::class, fakeRepo.emittedCommands[4]::class)
    }
}
