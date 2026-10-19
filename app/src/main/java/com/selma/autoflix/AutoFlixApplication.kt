package com.selma.autoflix

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.selma.autoflix.data.local.AutoFlixDatabase
import com.selma.autoflix.data.repository.LiveTvRepository
import com.selma.autoflix.data.repository.SavedSitesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AutoFlixApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AutoFlixDatabase
        private set

    lateinit var savedSitesRepository: SavedSitesRepository
        private set

    lateinit var liveTvRepository: LiveTvRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("AutoFlixCrash", "FATAL CRASH on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        database = AutoFlixDatabase.getInstance(this)
        savedSitesRepository = SavedSitesRepository(database.savedSiteDao())
        liveTvRepository = LiveTvRepository(this, database.channelDao())

        // Pre-seed repositories in background
        applicationScope.launch {
            try {
                savedSitesRepository.seedDefaultsIfEmpty()
                liveTvRepository.seedDefaultsIfEmpty()
            } catch (e: Exception) {
                Log.e("AutoFlixApp", "Error seeding defaults", e)
            }
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val defaultChannel = NotificationChannel(
                CHANNEL_ID_MEDIA,
                "AutoFlix Media",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AutoFlix Media Playback Notifications"
            }
            notificationManager.createNotificationChannel(defaultChannel)
        }
    }

    companion object {
        const val CHANNEL_ID_MEDIA = "autoflix_media_channel"

        lateinit var instance: AutoFlixApplication
            private set
    }
}
