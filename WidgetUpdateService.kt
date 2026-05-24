package com.letterboxd.widget

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Background service that refreshes all active Letterboxd widgets.
 * Called from WorkManager tasks scheduled in each WidgetProvider.
 */
class WidgetUpdateService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val widgetManager = AppWidgetManager.getInstance(this)

        CoroutineScope(Dispatchers.IO).launch {
            // Refresh all Recents widgets
            val recentsIds = widgetManager.getAppWidgetIds(
                ComponentName(this@WidgetUpdateService, RecentsWidgetProvider::class.java)
            )
            for (id in recentsIds) {
                WidgetUpdater.updateRecentsWidget(this@WidgetUpdateService, widgetManager, id)
            }

            // Refresh all Favourites widgets
            val favIds = widgetManager.getAppWidgetIds(
                ComponentName(this@WidgetUpdateService, FavouritesWidgetProvider::class.java)
            )
            for (id in favIds) {
                WidgetUpdater.updateFavouritesWidget(this@WidgetUpdateService, widgetManager, id)
            }

            stopSelf(startId)
        }

        return START_NOT_STICKY
    }
}
