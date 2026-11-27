package com.hotaro.brik.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AssistivePreferences {
    private const val PREFS_NAME = "brik_assistive_preferences"
    private const val KEY_ENABLED = "pref_assistive_enabled"
    private const val KEY_AUTOHIDE_SECONDS = "pref_assistive_autohide_seconds"
    private const val KEY_SHOW_WIFI = "pref_assistive_show_wifi"
    private const val KEY_SHOW_DATA = "pref_assistive_show_data"
    private const val KEY_SHOW_HOTSPOT = "pref_assistive_show_hotspot"
    private const val KEY_SHOW_FLASHLIGHT = "pref_assistive_show_flashlight"
    private const val KEY_SHOW_BLUETOOTH = "pref_assistive_show_bluetooth"
    private const val KEY_SHOW_DND = "pref_assistive_show_dnd"
    private const val KEY_SHOW_PHONE = "pref_assistive_show_phone"
    private const val KEY_SHOW_BRIGHTNESS_SLIDER = "pref_assistive_show_brightness_slider"

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _autoHideSeconds = MutableStateFlow(3)
    val autoHideSeconds: StateFlow<Int> = _autoHideSeconds.asStateFlow()

    private val _showWifi = MutableStateFlow(true)
    val showWifi: StateFlow<Boolean> = _showWifi.asStateFlow()

    private val _showData = MutableStateFlow(true)
    val showData: StateFlow<Boolean> = _showData.asStateFlow()

    private val _showHotspot = MutableStateFlow(true)
    val showHotspot: StateFlow<Boolean> = _showHotspot.asStateFlow()

    private val _showFlashlight = MutableStateFlow(true)
    val showFlashlight: StateFlow<Boolean> = _showFlashlight.asStateFlow()

    private val _showBluetooth = MutableStateFlow(true)
    val showBluetooth: StateFlow<Boolean> = _showBluetooth.asStateFlow()

    private val _showDnd = MutableStateFlow(false)
    val showDnd: StateFlow<Boolean> = _showDnd.asStateFlow()

    private val _showPhone = MutableStateFlow(true)
    val showPhone: StateFlow<Boolean> = _showPhone.asStateFlow()

    private val _showBrightnessSlider = MutableStateFlow(true)
    val showBrightnessSlider: StateFlow<Boolean> = _showBrightnessSlider.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _enabled.value = prefs.getBoolean(KEY_ENABLED, true)
        _autoHideSeconds.value = prefs.getInt(KEY_AUTOHIDE_SECONDS, 3)
        _showWifi.value = prefs.getBoolean(KEY_SHOW_WIFI, true)
        _showData.value = prefs.getBoolean(KEY_SHOW_DATA, true)
        _showHotspot.value = prefs.getBoolean(KEY_SHOW_HOTSPOT, true)
        _showFlashlight.value = prefs.getBoolean(KEY_SHOW_FLASHLIGHT, true)
        _showBluetooth.value = prefs.getBoolean(KEY_SHOW_BLUETOOTH, true)
        _showDnd.value = prefs.getBoolean(KEY_SHOW_DND, false)
        _showPhone.value = prefs.getBoolean(KEY_SHOW_PHONE, true)
        _showBrightnessSlider.value = prefs.getBoolean(KEY_SHOW_BRIGHTNESS_SLIDER, true)
    }

    fun setEnabled(context: Context, value: Boolean) {
        _enabled.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun setAutoHideSeconds(context: Context, value: Int) {
        _autoHideSeconds.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_AUTOHIDE_SECONDS, value).apply()
    }

    fun setShowWifi(context: Context, value: Boolean) {
        _showWifi.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_WIFI, value).apply()
    }

    fun setShowData(context: Context, value: Boolean) {
        _showData.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_DATA, value).apply()
    }

    fun setShowHotspot(context: Context, value: Boolean) {
        _showHotspot.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_HOTSPOT, value).apply()
    }

    fun setShowFlashlight(context: Context, value: Boolean) {
        _showFlashlight.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_FLASHLIGHT, value).apply()
    }

    fun setShowBluetooth(context: Context, value: Boolean) {
        _showBluetooth.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_BLUETOOTH, value).apply()
    }

    fun setShowDnd(context: Context, value: Boolean) {
        _showDnd.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_DND, value).apply()
    }

    fun setShowPhone(context: Context, value: Boolean) {
        _showPhone.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_PHONE, value).apply()
    }

    fun setShowBrightnessSlider(context: Context, value: Boolean) {
        _showBrightnessSlider.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_BRIGHTNESS_SLIDER, value).apply()
    }
}
