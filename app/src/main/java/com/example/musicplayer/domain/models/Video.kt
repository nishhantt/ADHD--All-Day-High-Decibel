package com.example.musicplayer.domain.models

data class Video(
    val id: String,
    val title: String,
    val thumbnailUrl: String
)

data class PlayableMedia(
    val uriString: String,
    val mimeType: String? = null,
    val title: String? = null,
    val thumbnailUrl: String? = null
)
