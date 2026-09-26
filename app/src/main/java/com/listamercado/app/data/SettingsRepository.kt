package com.listamercado.app.data

import android.content.Context

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun themeMode(): String = prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME, mode).apply()
    }

    fun lastSeenWhatsNewVersionCode(): Int = prefs.getInt(KEY_LAST_WHATS_NEW_VERSION, 0)

    fun markWhatsNewSeen(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_WHATS_NEW_VERSION, versionCode).apply()
    }

    companion object {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        private const val PREFS_NAME = "lista_mercado_settings"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LAST_WHATS_NEW_VERSION = "last_whats_new_version_code"
    }
}
