package com.letterboxd.widget

import android.content.Context

object WidgetPrefs {

    private const val PREFS_NAME = "com.letterboxd.widget.prefs"
    private const val KEY_USERNAME = "username_"

    fun saveUsername(context: Context, appWidgetId: Int, username: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USERNAME + appWidgetId, username)
            .apply()
    }

    fun loadUsername(context: Context, appWidgetId: Int): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME + appWidgetId, "BenHoule") ?: "BenHoule"
    }

    fun deleteUsername(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_USERNAME + appWidgetId)
            .apply()
    }
}
