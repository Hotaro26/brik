package com.hotaro.brik.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemePreferences {
    private const val PREFS_NAME = "brik_theme_preferences"
    private const val KEY_THEME_MODE = "pref_theme_mode"
    private const val KEY_COLOR_PALETTE = "pref_color_palette"

    // Defaults: 0 = System, "dynamic" = Material You
    private val _themeMode = MutableStateFlow(0)
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _colorPalette = MutableStateFlow("dynamic")
    val colorPalette: StateFlow<String> = _colorPalette.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getInt(KEY_THEME_MODE, 0)
        _colorPalette.value = prefs.getString(KEY_COLOR_PALETTE, "dynamic") ?: "dynamic"
    }

    fun setThemeMode(context: Context, mode: Int) {
        _themeMode.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    fun setColorPalette(context: Context, palette: String) {
        _colorPalette.value = palette
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_COLOR_PALETTE, palette).apply()
    }
}
