package com.selma.autoflix.data.repository

import com.selma.autoflix.data.local.SavedSiteDao
import com.selma.autoflix.data.local.SavedSiteEntity
import com.selma.autoflix.data.local.toEntity
import com.selma.autoflix.data.model.SavedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SavedSitesRepository(
    private val savedSiteDao: SavedSiteDao
) {
    val sitesFlow: Flow<List<SavedSite>> = savedSiteDao.getAllSitesFlow().map { list ->
        list.map { it.toModel() }
    }

    suspend fun getSavedSites(): List<SavedSite> {
        seedDefaultsIfEmpty()
        return savedSiteDao.getAllSites().map { it.toModel() }
    }

    suspend fun addSite(title: String, url: String): Long {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return -1L
        return savedSiteDao.insertSite(
            SavedSiteEntity(
                title = title.ifBlank { trimmedUrl },
                url = trimmedUrl,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleFavorite(url: String, isFavorite: Boolean) {
        savedSiteDao.updateFavorite(url, isFavorite)
    }

    suspend fun deleteSite(url: String) {
        savedSiteDao.deleteSite(url)
    }

    suspend fun seedDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        val count = savedSiteDao.getCount()
        if (count == 0) {
            val defaults = listOf(
                SavedSite(title = "تلوبیون", url = "https://telewebion.com"),
                SavedSite(title = "فیلیمو", url = "https://filimo.com"),
                SavedSite(title = "نماوا", url = "https://namava.ir"),
                SavedSite(title = "فوتبال ۳۶۰", url = "https://football360.ir"),
                SavedSite(title = "آنتن", url = "https://anten.ir"),
                SavedSite(title = "آپارات", url = "https://aparat.com")
            )
            for (site in defaults) {
                savedSiteDao.insertSite(site.toEntity())
            }
        }
    }
}
