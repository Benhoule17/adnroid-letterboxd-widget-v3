package com.letterboxd.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecentsWidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default result is CANCELED (user backed out)
        setResult(RESULT_CANCELED)

        setContentView(R.layout.activity_config)

        appWidgetId = intent?.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val usernameInput = findViewById<EditText>(R.id.username_input)
        val addButton = findViewById<Button>(R.id.add_widget_button)

        // Pre-fill if already configured (e.g. widget reconfigure)
        val existing = WidgetPrefs.loadUsername(this, appWidgetId)
        if (existing.isNotEmpty()) usernameInput.setText(existing)

        addButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            if (username.isEmpty()) {
                Toast.makeText(this, R.string.error_no_username, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            WidgetPrefs.saveUsername(this, appWidgetId, username)

            val appWidgetManager = AppWidgetManager.getInstance(this)
            CoroutineScope(Dispatchers.Main).launch {
                WidgetUpdater.updateRecentsWidget(this@RecentsWidgetConfigActivity, appWidgetManager, appWidgetId)
            }

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
