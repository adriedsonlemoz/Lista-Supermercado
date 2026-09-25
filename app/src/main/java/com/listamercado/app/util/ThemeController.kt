package com.listamercado.app.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.listamercado.app.data.SettingsRepository

object ThemeController {
    fun applySavedMode(context: Context) {
        applyMode(SettingsRepository(context).themeMode())
    }

    fun applyMode(mode: String) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                SettingsRepository.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                SettingsRepository.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    fun label(mode: String): String = when (mode) {
        SettingsRepository.THEME_LIGHT -> "Claro"
        SettingsRepository.THEME_DARK -> "Escuro"
        else -> "Seguir sistema"
    }
}
