package com.example.musicplayer.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    @ColumnInfo val title: String,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String,
    @ColumnInfo(name = "is_downloaded") val isDownloaded: Boolean = false,
    @ColumnInfo(name = "file_path") val filePath: String? = null
)
