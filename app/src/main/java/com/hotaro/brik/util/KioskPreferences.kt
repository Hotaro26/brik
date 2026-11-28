package com.hotaro.brik.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object KioskPreferences {
    private const val PREFS_NAME = "brik_kiosk_preferences"
    private const val KEY_ALLOW_CALLS_WINDOW = "pref_kiosk_allow_calls_window"
    private const val KEY_ALLOW_EMERGENCY_DIALER = "pref_kiosk_allow_emergency_dialer"
    private const val KEY_AUTO_RESUME_CALL = "pref_kiosk_auto_resume_call"
    private const val KEY_LOCKDOWN_NOTIFICATION_SHADE = "pref_kiosk_lockdown_notification_shade"
    private const val KEY_ALLOW_CONTROL_PANELS_STATUS_BAR = "pref_kiosk_allow_control_panels_status_bar"
    private const val KEY_SHOW_BATTERY_INDICATOR = "pref_kiosk_show_battery_indicator"
    private const val KEY_EXIT_HOLD_SECONDS = "pref_kiosk_exit_hold_seconds"
    private const val KEY_APP_NAMES_ALIGNMENT = "pref_kiosk_app_names_alignment"
    private const val KEY_BLOCK_ADB_IN_KIOSK = "pref_kiosk_block_adb"

    private val _allowIncomingCallsWindow = MutableStateFlow(true)
    val allowIncomingCallsWindow: StateFlow<Boolean> = _allowIncomingCallsWindow.asStateFlow()

    private val _allowEmergencyDialer = MutableStateFlow(true)
    val allowEmergencyDialer: StateFlow<Boolean> = _allowEmergencyDialer.asStateFlow()

    private val _autoResumeAfterCall = MutableStateFlow(true)
    val autoResumeAfterCall: StateFlow<Boolean> = _autoResumeAfterCall.asStateFlow()

    private val _lockdownNotificationShade = MutableStateFlow(true)
    val lockdownNotificationShade: StateFlow<Boolean> = _lockdownNotificationShade.asStateFlow()

    private val _allowControlPanelsStatusBar = MutableStateFlow(false)
    val allowControlPanelsStatusBar: StateFlow<Boolean> = _allowControlPanelsStatusBar.asStateFlow()

    private val _showBatteryIndicator = MutableStateFlow(true)
    val showBatteryIndicator: StateFlow<Boolean> = _showBatteryIndicator.asStateFlow()

    private val _appNamesAlignment = MutableStateFlow("center")
    val appNamesAlignment: StateFlow<String> = _appNamesAlignment.asStateFlow()

    private val _exitHoldSeconds = MutableStateFlow(3)
    val exitHoldSeconds: StateFlow<Int> = _exitHoldSeconds.asStateFlow()

    private val _blockAdbInKiosk = MutableStateFlow(false)
    val blockAdbInKiosk: StateFlow<Boolean> = _blockAdbInKiosk.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _allowIncomingCallsWindow.value = prefs.getBoolean(KEY_ALLOW_CALLS_WINDOW, true)
        _allowEmergencyDialer.value = prefs.getBoolean(KEY_ALLOW_EMERGENCY_DIALER, true)
        _autoResumeAfterCall.value = prefs.getBoolean(KEY_AUTO_RESUME_CALL, true)
        _allowControlPanelsStatusBar.value = prefs.getBoolean(KEY_ALLOW_CONTROL_PANELS_STATUS_BAR, false)
        _lockdownNotificationShade.value = prefs.getBoolean(KEY_LOCKDOWN_NOTIFICATION_SHADE, !_allowControlPanelsStatusBar.value)
        _showBatteryIndicator.value = prefs.getBoolean(KEY_SHOW_BATTERY_INDICATOR, true)
        _appNamesAlignment.value = prefs.getString(KEY_APP_NAMES_ALIGNMENT, "center") ?: "center"
        _exitHoldSeconds.value = prefs.getInt(KEY_EXIT_HOLD_SECONDS, 3)
        _blockAdbInKiosk.value = prefs.getBoolean(KEY_BLOCK_ADB_IN_KIOSK, false)
    }

    fun setAllowIncomingCallsWindow(context: Context, value: Boolean) {
        _allowIncomingCallsWindow.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ALLOW_CALLS_WINDOW, value).apply()
    }

    fun setAllowEmergencyDialer(context: Context, value: Boolean) {
        _allowEmergencyDialer.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ALLOW_EMERGENCY_DIALER, value).apply()
    }

    fun setAutoResumeAfterCall(context: Context, value: Boolean) {
        _autoResumeAfterCall.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_RESUME_CALL, value).apply()
    }

    fun setLockdownNotificationShade(context: Context, value: Boolean) {
        _lockdownNotificationShade.value = value
        _allowControlPanelsStatusBar.value = !value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOCKDOWN_NOTIFICATION_SHADE, value)
            .putBoolean(KEY_ALLOW_CONTROL_PANELS_STATUS_BAR, !value)
            .apply()
    }

    fun setAllowControlPanelsStatusBar(context: Context, value: Boolean) {
        _allowControlPanelsStatusBar.value = value
        _lockdownNotificationShade.value = !value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ALLOW_CONTROL_PANELS_STATUS_BAR, value)
            .putBoolean(KEY_LOCKDOWN_NOTIFICATION_SHADE, !value)
            .apply()
    }

    fun setShowBatteryIndicator(context: Context, value: Boolean) {
        _showBatteryIndicator.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_BATTERY_INDICATOR, value).apply()
    }

    fun setAppNamesAlignment(context: Context, value: String) {
        _appNamesAlignment.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_APP_NAMES_ALIGNMENT, value).apply()
    }

    fun setExitHoldSeconds(context: Context, value: Int) {
        _exitHoldSeconds.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_EXIT_HOLD_SECONDS, value).apply()
    }

    fun setBlockAdbInKiosk(context: Context, value: Boolean) {
        _blockAdbInKiosk.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_BLOCK_ADB_IN_KIOSK, value).apply()
    }
}
