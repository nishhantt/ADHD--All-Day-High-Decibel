package com.example.musicplayer.di

import com.example.musicplayer.network.SaavnService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(logger)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            // Forcing HTTP/1.1 if HTTP/2 causes PROTOCOL_ERROR with this specific API
            .protocols(listOf(Protocol.HTTP_1_1, Protocol.HTTP_2))
            .build()
    }

    @Provides
    @Singleton
    fun provideSaavnService(client: OkHttpClient): SaavnService {
        return SaavnService(client)
    }

    @Provides
    @Singleton
    fun provideYouTubeSearchService(client: OkHttpClient): com.example.musicplayer.network.YouTubeSearchService {
        return com.example.musicplayer.network.YouTubeSearchService(client)
    }

    @Provides
    @Singleton
    fun provideSoundCloudService(client: OkHttpClient): com.example.musicplayer.network.SoundCloudService {
        return com.example.musicplayer.network.SoundCloudService(client)
    }
}
