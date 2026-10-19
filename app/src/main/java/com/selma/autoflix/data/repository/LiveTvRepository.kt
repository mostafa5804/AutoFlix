package com.selma.autoflix.data.repository

import android.content.Context
import com.selma.autoflix.R
import com.selma.autoflix.data.local.ChannelDao
import com.selma.autoflix.data.local.toEntity
import com.selma.autoflix.data.model.LiveTvChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.regex.Pattern

class LiveTvRepository(
    private val context: Context,
    private val channelDao: ChannelDao
) {
    val channelsFlow: Flow<List<LiveTvChannel>> = channelDao.getAllChannelsFlow().map { list ->
        list.map { it.toModel() }
    }

    suspend fun getChannels(): List<LiveTvChannel> {
        seedDefaultsIfEmpty()
        return channelDao.getAllChannels().map { it.toModel() }
    }

    suspend fun toggleFavorite(streamUrl: String, isFavorite: Boolean) {
        channelDao.updateFavorite(streamUrl, isFavorite)
    }

    suspend fun deleteChannel(streamUrl: String) {
        channelDao.deleteChannel(streamUrl)
    }

    suspend fun importFromM3uUrl(url: String): List<LiveTvChannel> = withContext(Dispatchers.IO) {
        val stream = URL(url).openStream()
        val channels = parseM3u(stream, isSeeded = false, sourceUrl = url)
        if (channels.isNotEmpty()) {
            channelDao.insertChannels(channels.map { it.toEntity() })
        }
        channels
    }

    suspend fun seedDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        val count = channelDao.getCount()
        if (count == 0) {
            try {
                val inputStream = context.resources.openRawResource(R.raw.live_tv_seed)
                val channels = parseM3u(inputStream, isSeeded = true, sourceUrl = null)
                if (channels.isNotEmpty()) {
                    channelDao.insertChannels(channels.map { it.toEntity() })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun parseM3u(inputStream: InputStream, isSeeded: Boolean, sourceUrl: String?): List<LiveTvChannel> {
        val channels = mutableListOf<LiveTvChannel>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

        var currentExtInf: String? = null
        var categoryOrder = 0
        var channelOrder = 0
        var lastCategory = ""

        val tvgIdPattern = Pattern.compile("tvg-id=\"([^\"]*)\"")
        val tvgNamePattern = Pattern.compile("tvg-name=\"([^\"]*)\"")
        val tvgLogoPattern = Pattern.compile("tvg-logo=\"([^\"]*)\"")
        val groupTitlePattern = Pattern.compile("group-title=\"([^\"]*)\"")
        val tvgQualityPattern = Pattern.compile("tvg-quality=\"([^\"]*)\"")

        reader.useLines { lines ->
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.startsWith("#EXTINF:")) {
                    currentExtInf = line
                } else if (line.isNotEmpty() && !line.startsWith("#") && currentExtInf != null) {
                    val extInf = currentExtInf!!
                    val streamUrl = line

                    val commaIndex = extInf.lastIndexOf(',')
                    val displayName = if (commaIndex != -1 && commaIndex < extInf.length - 1) {
                        extInf.substring(commaIndex + 1).trim()
                    } else {
                        "شبکه بدون نام"
                    }

                    fun extract(pattern: Pattern): String? {
                        val m = pattern.matcher(extInf)
                        return if (m.find()) m.group(1) else null
                    }

                    val channelId = extract(tvgIdPattern) ?: extract(tvgNamePattern) ?: displayName
                    val logoUrl = extract(tvgLogoPattern)
                    val category = extract(groupTitlePattern) ?: "شبکه‌های سراسری سیما"
                    val quality = extract(tvgQualityPattern)

                    if (category != lastCategory) {
                        categoryOrder++
                        channelOrder = 0
                        lastCategory = category
                    } else {
                        channelOrder++
                    }

                    channels.add(
                        LiveTvChannel(
                            id = channelId,
                            name = displayName,
                            streamUrl = streamUrl,
                            logoUrl = logoUrl,
                            category = category,
                            quality = quality,
                            categoryOrder = categoryOrder,
                            channelOrder = channelOrder,
                            sourceUrl = sourceUrl,
                            isSeeded = isSeeded,
                            isFavorite = false,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    currentExtInf = null
                }
            }
        }
        return channels
    }
}
