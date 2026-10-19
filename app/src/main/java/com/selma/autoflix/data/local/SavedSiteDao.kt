package com.selma.autoflix.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedSiteDao {

    @Query("SELECT * FROM saved_sites ORDER BY isFavorite DESC, createdAt DESC")
    fun getAllSitesFlow(): Flow<List<SavedSiteEntity>>

    @Query("SELECT * FROM saved_sites ORDER BY isFavorite DESC, createdAt DESC")
    suspend fun getAllSites(): List<SavedSiteEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSite(site: SavedSiteEntity): Long

    @Query("UPDATE saved_sites SET isFavorite = :isFavorite WHERE url = :url")
    suspend fun updateFavorite(url: String, isFavorite: Boolean)

    @Query("DELETE FROM saved_sites WHERE url = :url")
    suspend fun deleteSite(url: String)

    @Query("SELECT COUNT(*) FROM saved_sites")
    suspend fun getCount(): Int
}
