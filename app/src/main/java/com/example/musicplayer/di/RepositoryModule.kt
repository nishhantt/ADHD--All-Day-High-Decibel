package com.example.musicplayer.di

import com.example.musicplayer.data.StreamExtractor
import com.example.musicplayer.data.YouTubeRepository
import com.example.musicplayer.data.YouTubeRepositoryImpl
import com.example.musicplayer.data.BackendStreamExtractor
import com.example.musicplayer.data.PlayerRepository
import com.example.musicplayer.data.PlayerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindYouTubeRepository(impl: YouTubeRepositoryImpl): YouTubeRepository

    @Binds
    @Singleton
    abstract fun bindStreamExtractor(impl: BackendStreamExtractor): StreamExtractor

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository
}
