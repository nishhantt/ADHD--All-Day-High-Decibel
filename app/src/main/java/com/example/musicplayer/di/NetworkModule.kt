package com.example.musicplayer.di

import com.example.musicplayer.BuildConfig
import com.example.musicplayer.network.YouTubeApiService
import com.example.musicplayer.network.ExtractorBackendApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BASIC
        return OkHttpClient.Builder()
            .addInterceptor(logger)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideYouTubeApi(retrofit: Retrofit): YouTubeApiService {
        return retrofit.create(YouTubeApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("extractorBackend")
    fun provideExtractorBackendRetrofit(client: OkHttpClient): Retrofit {
        val base = BuildConfig.EXTRACTOR_BACKEND_URL
        val url = when {
            base.isBlank() -> "https://example.com/"
            base.endsWith("/") -> base
            else -> "$base/"
        }
        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideExtractorBackendApi(@Named("extractorBackend") retrofit: Retrofit): ExtractorBackendApi {
        return retrofit.create(ExtractorBackendApi::class.java)
    }
}
