package com.selma.autoflix.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.selma.autoflix.data.model.LiveTvChannel

@Entity(tableName = "live_tv_channels")
data class ChannelEntity(
    @PrimaryKey
    val streamUrl: String,
    val channelId: String,
    val name: String,
    val logoUrl: String? = null,
    val category: String = "",
    val quality: String? = null,
    val categoryOrder: Int = 0,
    val channelOrder: Int = 0,
    val sourceUrl: String? = null,
    val isSeeded: Boolean = false,
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toModel(): LiveTvChannel = LiveTvChannel(
        id = channelId,
        name = name,
        streamUrl = streamUrl,
        logoUrl = logoUrl,
        category = category,
        quality = quality,
        categoryOrder = categoryOrder,
        channelOrder = channelOrder,
        sourceUrl = sourceUrl,
        isSeeded = isSeeded,
        isFavorite = isFavorite,
        updatedAt = updatedAt
    )
}

fun LiveTvChannel.toEntity(): ChannelEntity = ChannelEntity(
    streamUrl = streamUrl,
    channelId = id,
    name = name,
    logoUrl = logoUrl,
    category = category,
    quality = quality,
    categoryOrder = categoryOrder,
    channelOrder = channelOrder,
    sourceUrl = sourceUrl,
    isSeeded = isSeeded,
    isFavorite = isFavorite,
    updatedAt = updatedAt
)
