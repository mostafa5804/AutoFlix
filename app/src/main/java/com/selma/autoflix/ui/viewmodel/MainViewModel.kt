package com.selma.autoflix.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.selma.autoflix.data.model.LiveTvChannel
import com.selma.autoflix.data.model.SavedSite
import com.selma.autoflix.data.repository.LiveTvRepository
import com.selma.autoflix.data.repository.SavedSitesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MainTab(val title: String) {
    LIVE_TV("تلویزیون زنده"),
    BROWSER("مرورگر وب"),
    SAVED_SITES("سایت‌های ذخیره")
}

class MainViewModel(
    private val liveTvRepository: LiveTvRepository,
    private val savedSitesRepository: SavedSitesRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(MainTab.LIVE_TV)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("all")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _browserUrl = MutableStateFlow("https://telewebion.com")
    val browserUrl: StateFlow<String> = _browserUrl.asStateFlow()

    val channels: StateFlow<List<LiveTvChannel>> = liveTvRepository.channelsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedSites: StateFlow<List<SavedSite>> = savedSitesRepository.sitesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sortAlphabetical = MutableStateFlow(false)
    val sortAlphabetical: StateFlow<Boolean> = _sortAlphabetical.asStateFlow()

    fun toggleSortOrder() {
        _sortAlphabetical.value = !_sortAlphabetical.value
    }

    val categories: StateFlow<List<String>> = channels.map { channelList ->
        val cats = channelList.map { it.category }.filter { it.isNotBlank() }.distinct()
        listOf("all", "national", "provincial", "film", "music", "kids", "international", "news")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("all", "national", "provincial", "film", "music", "kids", "international", "news"))

    val filteredChannels: StateFlow<List<LiveTvChannel>> = combine(
        channels,
        selectedCategory,
        searchQuery,
        sortAlphabetical
    ) { channelList, category, query, sortAz ->
        val list = channelList.filter { ch ->
            val matchesCat = matchesCategory(ch, category)
            val matchesQuery = query.isBlank() || ch.name.contains(query, ignoreCase = true) || ch.category.contains(query, ignoreCase = true)
            matchesCat && matchesQuery
        }
        if (sortAz) {
            list.sortedBy { it.name }
        } else {
            list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openUrlInBrowser(url: String) {
        _browserUrl.value = url
        _currentTab.value = MainTab.BROWSER
    }

    fun toggleChannelFavorite(channel: LiveTvChannel) {
        viewModelScope.launch {
            liveTvRepository.toggleFavorite(channel.streamUrl, !channel.isFavorite)
        }
    }

    fun toggleSiteFavorite(site: SavedSite) {
        viewModelScope.launch {
            savedSitesRepository.toggleFavorite(site.url, !site.isFavorite)
        }
    }

    fun deleteSite(site: SavedSite) {
        viewModelScope.launch {
            savedSitesRepository.deleteSite(site.url)
        }
    }

    fun addSite(title: String, url: String) {
        viewModelScope.launch {
            savedSitesRepository.addSite(title, url)
        }
    }

    fun importM3u(url: String, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch {
            try {
                val list = liveTvRepository.importFromM3uUrl(url)
                onResult(true, list.size)
            } catch (e: Exception) {
                onResult(false, 0)
            }
        }
    }

    class Factory(
        private val liveTvRepository: LiveTvRepository,
        private val savedSitesRepository: SavedSitesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(liveTvRepository, savedSitesRepository) as T
        }
    }

    companion object {
        fun matchesCategory(ch: LiveTvChannel, category: String): Boolean {
            return when (category) {
                "all", "همه", "همه شبکه‌ها" -> true
                "favorites", "علاقه‌مندی‌ها" -> ch.isFavorite
                "national", "سراسری", "شبکه‌های سراسری" -> ch.category.contains("سراسری") || ch.category.contains("سیما") || ch.name.contains("یک") || ch.name.contains("دو") || ch.name.contains("چهار") || ch.name.contains("پنج") || ch.name.contains("نسیم")
                "provincial", "استانی", "شبکه‌های استانی" -> ch.category.contains("استانی") || ch.name.contains("سهند") || ch.name.contains("فارس") || ch.name.contains("اصفهان") || ch.name.contains("خراسان") || ch.name.contains("خوزستان")
                "movies", "film", "فیلم", "فیلم و سریال", "ماهواره فیلم" -> ch.category.contains("فیلم") || ch.category.contains("سریال") || ch.name.contains("فیلم") || ch.name.contains("نمایش") || ch.name.contains("تماشا") || ch.name.contains("Cinema") || ch.name.contains("Movie") || ch.name.contains("Box") || ch.name.contains("Series") || ch.name.contains("MBC") || ch.name.contains("GEM")
                "music", "موزیک", "ماهواره موزیک", "موسیقی و اجرا" -> ch.category.contains("موزیک") || ch.category.contains("موسیقی") || ch.name.contains("موزیک") || ch.name.contains("نوا") || ch.name.contains("Music") || ch.name.contains("آونگ") || ch.name.contains("PMC") || ch.name.contains("رادیو جوان") || ch.name.contains("Radio") || ch.name.contains("Sun")
                "kids", "کودک", "ماهواره کودک", "سرگرمی کودک و خانواده" -> ch.category.contains("کودک") || ch.name.contains("پویا") || ch.name.contains("نهال") || ch.name.contains("امید") || ch.name.contains("Junior") || ch.name.contains("کودک") || ch.name.contains("Toon") || ch.name.contains("Cartoon")
                "sports", "ورزش", "ورزشی", "ورزشی و مستند" -> ch.category.contains("ورزش") || ch.name.contains("ورزش") || ch.name.contains("Varzish") || ch.name.contains("فوتبال") || ch.name.contains("سه") || ch.name.contains("Sport")
                "news", "خبری", "شبکه‌های خبری", "خبر و تحلیل روز" -> ch.category.contains("خبر") || ch.name.contains("خبر") || ch.name.contains("IRINN") || ch.name.contains("News")
                "international", "بین‌المللی", "شبکه‌های بین‌المللی", "خبر و محتوای جهانی" -> ch.category.contains("بین‌المللی") || ch.category.contains("برون") || ch.name.contains("پرس") || ch.name.contains("العالم") || ch.name.contains("جام") || ch.name.contains("سحر") || ch.name.contains("هیسپان") || ch.name.contains("Press")
                else -> ch.category.equals(category, ignoreCase = true)
            }
        }
    }
}
