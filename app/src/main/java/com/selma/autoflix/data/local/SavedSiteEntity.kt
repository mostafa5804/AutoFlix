package com.selma.autoflix.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.selma.autoflix.data.model.SavedSite

@Entity(
    tableName = "saved_sites",
    indices = [Index(value = ["url"], unique = true)]
)
data class SavedSiteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
) {
    fun toModel(): SavedSite = SavedSite(
        id = id,
        title = title,
        url = url,
        createdAt = createdAt,
        isFavorite = isFavorite
    )
}

fun SavedSite.toEntity(): SavedSiteEntity = SavedSiteEntity(
    id = id,
    title = title,
    url = url,
    createdAt = createdAt,
    isFavorite = isFavorite
)
