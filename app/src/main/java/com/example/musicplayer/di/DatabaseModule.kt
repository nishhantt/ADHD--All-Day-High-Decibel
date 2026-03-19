package com.example.musicplayer.di

import android.content.Context
import androidx.room.Room
import com.example.musicplayer.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase {
        return Room.databaseBuilder(ctx, AppDatabase::class.java, "musicplayer.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideBehaviorDao(db: AppDatabase) = db.behaviorDao()

    @Provides
    fun providePlaybackStateDao(db: AppDatabase) = db.playbackStateDao()
}
