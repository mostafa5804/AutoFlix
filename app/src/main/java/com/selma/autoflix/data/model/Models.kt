package com.selma.autoflix.data.model

data class SavedSite(
    val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

data class LiveTvChannel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String = "",
    val quality: String? = null,
    val categoryOrder: Int = 0,
    val channelOrder: Int = 0,
    val sourceUrl: String? = null,
    val isSeeded: Boolean = false,
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
