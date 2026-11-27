package com.hotaro.brik.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NavBarStyle {
    FLOATING,
    GLASS,
    CLASSIC
}

enum class PullRefreshAnimStyle(val displayName: String) {
    SMILEY("Smiley"),
    CAT("Cat"),
    BUTTERFLY("Butterfly"),
    BOOKS("Books"),
    LOTUS("Man in Lotus"),
    RANDOM("Random Mixed")
}

object ThemePreferences {
    private const val PREFS_NAME = "brik_theme_preferences"
    private const val KEY_THEME_MODE = "pref_theme_mode"
    private const val KEY_COLOR_PALETTE = "pref_color_palette"
    private const val KEY_NAVBAR_STYLE = "pref_navbar_style"
    private const val KEY_APP_ICON = "pref_app_icon"
    private const val KEY_CLOCK_FORMAT = "pref_clock_format"
    private const val KEY_PULL_REFRESH_ANIM = "pref_pull_refresh_anim"
    private const val KEY_MATERIAL_SHAPES_ENABLED = "pref_material_shapes_enabled"
    private const val KEY_MATERIAL_SHAPES_SMOOTH_ANIM = "pref_material_shapes_smooth_anim"
    private const val KEY_MATERIAL_SHAPES_RANDOM_WINDOW = "pref_material_shapes_random_window"
    private const val KEY_MATERIAL_SHAPE_TYPE = "pref_material_shape_type"

    // Defaults: 0 = System, "dynamic" = Material You, FLOATING = Default
    private val _themeMode = MutableStateFlow(0)
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _colorPalette = MutableStateFlow("dynamic")
    val colorPalette: StateFlow<String> = _colorPalette.asStateFlow()

    private val _navBarStyle = MutableStateFlow(NavBarStyle.GLASS)
    val navBarStyle: StateFlow<NavBarStyle> = _navBarStyle.asStateFlow()

    private val _appIcon = MutableStateFlow("dynamic")
    val appIcon: StateFlow<String> = _appIcon.asStateFlow()

    // true = 24-hour, false = 12-hour (AM/PM)
    private val _use24HourClock = MutableStateFlow(false)
    val use24HourClock: StateFlow<Boolean> = _use24HourClock.asStateFlow()

    private val _pullRefreshAnim = MutableStateFlow(PullRefreshAnimStyle.SMILEY)
    val pullRefreshAnim: StateFlow<PullRefreshAnimStyle> = _pullRefreshAnim.asStateFlow()

    // Material 3 Expressive Shapes preferences (Defaults: 8-leaf clover, animating)
    private val _materialShapesEnabled = MutableStateFlow(true)
    val materialShapesEnabled: StateFlow<Boolean> = _materialShapesEnabled.asStateFlow()

    private val _materialShapesSmoothAnim = MutableStateFlow(true)
    val materialShapesSmoothAnim: StateFlow<Boolean> = _materialShapesSmoothAnim.asStateFlow()

    private val _materialShapesRandomWindow = MutableStateFlow(false)
    val materialShapesRandomWindow: StateFlow<Boolean> = _materialShapesRandomWindow.asStateFlow()

    private val _materialShapeType = MutableStateFlow(ExpressiveShapeType.CLOVER_8.name)
    val materialShapeType: StateFlow<String> = _materialShapeType.asStateFlow()

    private val _windowSeed = MutableStateFlow(0)
    val windowSeed: StateFlow<Int> = _windowSeed.asStateFlow()

    fun bumpWindowSeed() {
        _windowSeed.value = (_windowSeed.value + 1) % 1000
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeMode.value = prefs.getInt(KEY_THEME_MODE, 0)
        _colorPalette.value = prefs.getString(KEY_COLOR_PALETTE, "dynamic") ?: "dynamic"
        val navStyleStr = prefs.getString(KEY_NAVBAR_STYLE, NavBarStyle.GLASS.name)
        _navBarStyle.value = runCatching { NavBarStyle.valueOf(navStyleStr ?: NavBarStyle.GLASS.name) }
            .getOrDefault(NavBarStyle.GLASS)
        val savedIcon = prefs.getString(KEY_APP_ICON, "dynamic") ?: "dynamic"
        _appIcon.value = if (savedIcon == "default") "dynamic" else savedIcon
        _use24HourClock.value = prefs.getBoolean(KEY_CLOCK_FORMAT, false)
        val animStr = prefs.getString(KEY_PULL_REFRESH_ANIM, PullRefreshAnimStyle.SMILEY.name)
        _pullRefreshAnim.value = runCatching { PullRefreshAnimStyle.valueOf(animStr ?: PullRefreshAnimStyle.SMILEY.name) }
            .getOrDefault(PullRefreshAnimStyle.SMILEY)
        _materialShapesEnabled.value = prefs.getBoolean(KEY_MATERIAL_SHAPES_ENABLED, true)
        _materialShapesSmoothAnim.value = prefs.getBoolean(KEY_MATERIAL_SHAPES_SMOOTH_ANIM, true)
        val savedShape = prefs.getString(KEY_MATERIAL_SHAPE_TYPE, null)
        _materialShapeType.value = if (savedShape == null || savedShape == "random") ExpressiveShapeType.CLOVER_8.name else savedShape
        _materialShapesRandomWindow.value = if (!prefs.contains(KEY_MATERIAL_SHAPES_RANDOM_WINDOW) || savedShape == "random") false else prefs.getBoolean(KEY_MATERIAL_SHAPES_RANDOM_WINDOW, false)
    }

    fun setMaterialShapesEnabled(context: Context, enabled: Boolean) {
        _materialShapesEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MATERIAL_SHAPES_ENABLED, enabled).apply()
    }

    fun setMaterialShapesSmoothAnim(context: Context, enabled: Boolean) {
        _materialShapesSmoothAnim.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MATERIAL_SHAPES_SMOOTH_ANIM, enabled).apply()
    }

    fun setMaterialShapesRandomWindow(context: Context, enabled: Boolean) {
        _materialShapesRandomWindow.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MATERIAL_SHAPES_RANDOM_WINDOW, enabled).apply()
    }

    fun setMaterialShapeType(context: Context, type: String) {
        _materialShapeType.value = type
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MATERIAL_SHAPE_TYPE, type).apply()
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

    fun setUse24HourClock(context: Context, use24: Boolean) {
        _use24HourClock.value = use24
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_CLOCK_FORMAT, use24).apply()
    }

    fun setPullRefreshAnim(context: Context, style: PullRefreshAnimStyle) {
        _pullRefreshAnim.value = style
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PULL_REFRESH_ANIM, style.name).apply()
    }

    fun formatTime(hour: Int, minute: Int, use24: Boolean = _use24HourClock.value): String {
        return if (use24) {
            String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute)
        } else {
            val ampm = if (hour >= 12) "PM" else "AM"
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            String.format(java.util.Locale.getDefault(), "%d:%02d %s", h12, minute, ampm)
        }
    }

    fun getTimePattern(use24: Boolean = _use24HourClock.value): String {
        return if (use24) "HH:mm" else "h:mm a"
    }
}
