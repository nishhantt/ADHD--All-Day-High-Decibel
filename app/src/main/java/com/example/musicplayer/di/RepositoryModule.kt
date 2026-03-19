package com.example.musicplayer.di

import com.example.musicplayer.data.MusicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.musicplayer.network.SaavnService
import com.example.musicplayer.network.YouTubeSearchService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMusicRepository(
        saavnService: SaavnService,
        youtubeService: YouTubeSearchService
    ): MusicRepository {
        return MusicRepository(saavnService, youtubeService)
    }
}
