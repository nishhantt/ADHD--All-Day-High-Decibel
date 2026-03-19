package com.example.musicplayer.di

import com.example.musicplayer.data.LocalMusicRepository
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.network.YouTubeSearchService
import com.example.musicplayer.network.YouTubeExtractor
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
        youtubeService: YouTubeSearchService,
        youtubeExtractor: YouTubeExtractor,
        localMusicRepository: LocalMusicRepository
    ): MusicRepository {
        return MusicRepository(youtubeService, youtubeExtractor, localMusicRepository)
    }
}
