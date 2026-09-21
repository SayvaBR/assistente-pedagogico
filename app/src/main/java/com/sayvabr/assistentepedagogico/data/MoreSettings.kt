package com.sayvabr.assistentepedagogico.data

import android.content.Context

/** Local-only preferences used by the More/settings flows. No preference is synced remotely. */
data class MoreSettings(
    val notifications: Boolean = true,
    val reminders: Boolean = true,
    val dailySummary: Boolean = true,
    val productUpdates: Boolean = false,
    val theme: String = "Claro",
    val fontSize: String = "Padrão",
    val reduceMotion: Boolean = false,
    val language: String = "Português (Brasil)",
)

object MoreSettingsStore {
    const val FILE = "more_settings"
    private const val NOTIFICATIONS = "notifications"
    private const val REMINDERS = "reminders"
    private const val DAILY_SUMMARY = "daily_summary"
    private const val PRODUCT_UPDATES = "product_updates"
    private const val THEME = "theme"
    private const val FONT_SIZE = "font_size"
    private const val REDUCE_MOTION = "reduce_motion"
    private const val LANGUAGE = "language"

    fun read(context: Context): MoreSettings {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return MoreSettings(
            notifications = prefs.getBoolean(NOTIFICATIONS, true),
            reminders = prefs.getBoolean(REMINDERS, true),
            dailySummary = prefs.getBoolean(DAILY_SUMMARY, true),
            productUpdates = prefs.getBoolean(PRODUCT_UPDATES, false),
            theme = prefs.getString(THEME, "Claro") ?: "Claro",
            fontSize = prefs.getString(FONT_SIZE, "Padrão") ?: "Padrão",
            reduceMotion = prefs.getBoolean(REDUCE_MOTION, false),
            language = prefs.getString(LANGUAGE, "Português (Brasil)") ?: "Português (Brasil)",
        )
    }

    fun write(context: Context, settings: MoreSettings) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean(NOTIFICATIONS, settings.notifications)
            .putBoolean(REMINDERS, settings.reminders)
            .putBoolean(DAILY_SUMMARY, settings.dailySummary)
            .putBoolean(PRODUCT_UPDATES, settings.productUpdates)
            .putString(THEME, settings.theme)
            .putString(FONT_SIZE, settings.fontSize)
            .putBoolean(REDUCE_MOTION, settings.reduceMotion)
            .putString(LANGUAGE, settings.language)
            .apply()
    }
}
