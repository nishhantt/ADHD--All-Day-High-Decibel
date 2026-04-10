package com.example.musicplayer.di

import com.example.musicplayer.data.LocalMusicRepository
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.network.PhoneBackendService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMusicRepository(
        phoneBackendService: PhoneBackendService,
        localMusicRepository: LocalMusicRepository
    ): MusicRepository {
        return MusicRepository(phoneBackendService, localMusicRepository)
    }
}
