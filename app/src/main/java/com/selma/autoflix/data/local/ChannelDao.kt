package com.selma.autoflix.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM live_tv_channels ORDER BY categoryOrder ASC, channelOrder ASC, name ASC")
    fun getAllChannelsFlow(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM live_tv_channels ORDER BY categoryOrder ASC, channelOrder ASC, name ASC")
    suspend fun getAllChannels(): List<ChannelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("UPDATE live_tv_channels SET isFavorite = :isFavorite WHERE streamUrl = :streamUrl")
    suspend fun updateFavorite(streamUrl: String, isFavorite: Boolean)

    @Query("DELETE FROM live_tv_channels WHERE streamUrl = :streamUrl")
    suspend fun deleteChannel(streamUrl: String)

    @Query("SELECT COUNT(*) FROM live_tv_channels")
    suspend fun getCount(): Int
}
