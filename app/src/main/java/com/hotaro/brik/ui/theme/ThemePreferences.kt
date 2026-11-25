package com.hotaro.brik.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NavBarStyle {
    FLOATING,
    CLASSIC
}

object ThemePreferences {
    private const val PREFS_NAME = "brik_theme_preferences"
    private const val KEY_THEME_MODE = "pref_theme_mode"
    private const val KEY_COLOR_PALETTE = "pref_color_palette"
    private const val KEY_NAVBAR_STYLE = "pref_navbar_style"
    private const val KEY_APP_ICON = "pref_app_icon"

    // Defaults: 0 = System, "dynamic" = Material You, FLOATING = Default
    private val _themeMode = MutableStateFlow(0)
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _colorPalette = MutableStateFlow("dynamic")
    val colorPalette: StateFlow<String> = _colorPalette.asStateFlow()

    private val _navBarStyle = MutableStateFlow(NavBarStyle.FLOATING)
    val navBarStyle: StateFlow<NavBarStyle> = _navBarStyle.asStateFlow()

    private val _appIcon = MutableStateFlow("dynamic")
    val appIcon: StateFlow<String> = _appIcon.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getInt(KEY_THEME_MODE, 0)
        _colorPalette.value = prefs.getString(KEY_COLOR_PALETTE, "dynamic") ?: "dynamic"
        val navStyleStr = prefs.getString(KEY_NAVBAR_STYLE, NavBarStyle.FLOATING.name)
        _navBarStyle.value = runCatching { NavBarStyle.valueOf(navStyleStr ?: NavBarStyle.FLOATING.name) }
            .getOrDefault(NavBarStyle.FLOATING)
        val savedIcon = prefs.getString(KEY_APP_ICON, "dynamic") ?: "dynamic"
        _appIcon.value = if (savedIcon == "default") "dynamic" else savedIcon
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

    fun setNavBarStyle(context: Context, style: NavBarStyle) {
        _navBarStyle.value = style
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NAVBAR_STYLE, style.name).apply()
    }

    fun setAppIcon(context: Context, iconId: String) {
        _appIcon.value = iconId
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_ICON, iconId).apply()
    }
}
