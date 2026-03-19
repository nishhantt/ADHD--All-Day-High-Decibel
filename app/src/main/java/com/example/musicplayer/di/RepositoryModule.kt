package com.example.musicplayer.di

import com.example.musicplayer.data.LocalMusicRepository
import com.example.musicplayer.data.MusicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.example.musicplayer.network.SaavnService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMusicRepository(
        saavnService: SaavnService,
        youtubeService: com.example.musicplayer.network.YouTubeSearchService,
        youtubeExtractor: com.example.musicplayer.network.YouTubeExtractor,
        localMusicRepository: LocalMusicRepository
    ): MusicRepository {
        return MusicRepository(saavnService, youtubeService, youtubeExtractor, localMusicRepository)
    }
}
