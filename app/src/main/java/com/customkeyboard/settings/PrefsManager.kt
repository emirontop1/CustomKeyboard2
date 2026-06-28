package com.customkeyboard.settings

import android.content.Context
import android.graphics.Color

class PrefsManager(private val context: Context) {

    private val prefs = context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)

    fun isDarkTheme() = prefs.getBoolean(PrefsKeys.DARK_THEME, true)
    fun setDarkTheme(dark: Boolean) = prefs.edit().putBoolean(PrefsKeys.DARK_THEME, dark).apply()

    fun getAccentColor(): Int {
        val hex = prefs.getString(PrefsKeys.ACCENT_COLOR, "#4F8EF7") ?: "#4F8EF7"
        return try { Color.parseColor(hex) } catch (e: Exception) { Color.parseColor("#4F8EF7") }
    }
    fun setAccentColor(hex: String) = prefs.edit().putString(PrefsKeys.ACCENT_COLOR, hex).apply()

    fun isHapticEnabled() = prefs.getBoolean(PrefsKeys.HAPTIC, true)
    fun setHapticEnabled(enabled: Boolean) = prefs.edit().putBoolean(PrefsKeys.HAPTIC, enabled).apply()

    fun isSoundEnabled() = prefs.getBoolean(PrefsKeys.SOUND, true)
    fun setSoundEnabled(enabled: Boolean) = prefs.edit().putBoolean(PrefsKeys.SOUND, enabled).apply()
}
