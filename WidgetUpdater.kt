package com.letterboxd.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object WidgetUpdater {

    private const val TAG = "WidgetUpdater"
    private const val BASE_URL = "https://letterboxd.com"

    // ── Recents ──────────────────────────────────────────────────────────────

    suspend fun updateRecentsWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val username = WidgetPrefs.loadUsername(context, appWidgetId)
        if (username.isEmpty()) return

        val films = LetterboxdScraper.fetchRecents(username)
        val views = RemoteViews(context.packageName, R.layout.widget_recents)

        // Title taps open the profile
        val profileUri = Uri.parse("$BASE_URL/$username")
        val profileIntent = Intent(Intent.ACTION_VIEW, profileUri)
        val profilePending = PendingIntent.getActivity(
            context, appWidgetId,
            profileIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, profilePending)

        val posterIds = listOf(R.id.poster_1, R.id.poster_2, R.id.poster_3, R.id.poster_4)
        val ratingIds = listOf(R.id.rating_1, R.id.rating_2, R.id.rating_3, R.id.rating_4)

        for (i in 0..3) {
            if (i < films.size) {
                val film = films[i]
                val bitmap = downloadBitmap(film.posterUrl)
                if (bitmap != null) {
                    views.setImageViewBitmap(posterIds[i], roundCorners(bitmap, 20f))
                } else {
                    views.setImageViewResource(posterIds[i], R.drawable.poster_placeholder)
                }
                // Rating
                val ratingStr = formatRating(film.rating)
                views.setTextViewText(ratingIds[i], ratingStr)

                // Tap opens film page
                val filmUri = Uri.parse("$BASE_URL/film/${film.slug}")
                val filmIntent = Intent(Intent.ACTION_VIEW, filmUri)
                val filmPending = PendingIntent.getActivity(
                    context, appWidgetId * 10 + i,
                    filmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(posterIds[i], filmPending)
            } else {
                views.setImageViewResource(posterIds[i], R.drawable.poster_placeholder)
                views.setTextViewText(ratingIds[i], "")
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // ── Favourites ───────────────────────────────────────────────────────────

    suspend fun updateFavouritesWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val username = WidgetPrefs.loadUsername(context, appWidgetId)
        if (username.isEmpty()) return

        val films = LetterboxdScraper.fetchFavourites(username)
        val views = RemoteViews(context.packageName, R.layout.widget_favourites)

        val profileUri = Uri.parse("$BASE_URL/$username")
        val profileIntent = Intent(Intent.ACTION_VIEW, profileUri)
        val profilePending = PendingIntent.getActivity(
            context, appWidgetId,
            profileIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, profilePending)

        val posterIds = listOf(R.id.poster_1, R.id.poster_2, R.id.poster_3, R.id.poster_4)

        for (i in 0..3) {
            if (i < films.size) {
                val film = films[i]
                val bitmap = downloadBitmap(film.posterUrl)
                if (bitmap != null) {
                    views.setImageViewBitmap(posterIds[i], roundCorners(bitmap, 20f))
                } else {
                    views.setImageViewResource(posterIds[i], R.drawable.poster_placeholder)
                }
                val filmUri = Uri.parse("$BASE_URL/film/${film.slug}")
                val filmIntent = Intent(Intent.ACTION_VIEW, filmUri)
                val filmPending = PendingIntent.getActivity(
                    context, appWidgetId * 10 + i,
                    filmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(posterIds[i], filmPending)
            } else {
                views.setImageViewResource(posterIds[i], R.drawable.poster_placeholder)
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val stream = URL(url).openStream()
            BitmapFactory.decodeStream(stream)
        } catch (e: Exception) {
            Log.e(TAG, "downloadBitmap error: $url", e)
            null
        }
    }

    /** Applies rounded corners to a bitmap (mirroring the iOS cornerRadius: 10 style). */
    private fun roundCorners(src: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawRoundRect(RectF(0f, 0f, src.width.toFloat(), src.height.toFloat()), radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    /** Converts a float rating to star string, e.g. 3.5 → "★★★½". */
    fun formatRating(rating: Float): String {
        if (rating < 0) return ""
        val full = rating.toInt()
        val half = (rating % 1) != 0f
        return "★".repeat(full) + if (half) "½" else ""
    }
}
