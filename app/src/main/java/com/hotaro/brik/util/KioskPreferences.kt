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
    private const val KEY_ALLOW_SYSTEM_GESTURES = "pref_kiosk_allow_system_gestures"
    private const val KEY_EXIT_HOLD_SECONDS = "pref_kiosk_exit_hold_seconds"

    private val _allowIncomingCallsWindow = MutableStateFlow(true)
    val allowIncomingCallsWindow: StateFlow<Boolean> = _allowIncomingCallsWindow.asStateFlow()

    private val _allowEmergencyDialer = MutableStateFlow(true)
    val allowEmergencyDialer: StateFlow<Boolean> = _allowEmergencyDialer.asStateFlow()

    private val _autoResumeAfterCall = MutableStateFlow(true)
    val autoResumeAfterCall: StateFlow<Boolean> = _autoResumeAfterCall.asStateFlow()

    private val _lockdownNotificationShade = MutableStateFlow(true)
    val lockdownNotificationShade: StateFlow<Boolean> = _lockdownNotificationShade.asStateFlow()

    private val _allowSystemGestures = MutableStateFlow(true)
    val allowSystemGestures: StateFlow<Boolean> = _allowSystemGestures.asStateFlow()

    private val _exitHoldSeconds = MutableStateFlow(3)
    val exitHoldSeconds: StateFlow<Int> = _exitHoldSeconds.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _allowIncomingCallsWindow.value = prefs.getBoolean(KEY_ALLOW_CALLS_WINDOW, true)
        _allowEmergencyDialer.value = prefs.getBoolean(KEY_ALLOW_EMERGENCY_DIALER, true)
        _autoResumeAfterCall.value = prefs.getBoolean(KEY_AUTO_RESUME_CALL, true)
        _lockdownNotificationShade.value = prefs.getBoolean(KEY_LOCKDOWN_NOTIFICATION_SHADE, true)
        _allowSystemGestures.value = prefs.getBoolean(KEY_ALLOW_SYSTEM_GESTURES, true)
        _exitHoldSeconds.value = prefs.getInt(KEY_EXIT_HOLD_SECONDS, 3)
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
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LOCKDOWN_NOTIFICATION_SHADE, value).apply()
    }

    fun setAllowSystemGestures(context: Context, value: Boolean) {
        _allowSystemGestures.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ALLOW_SYSTEM_GESTURES, value).apply()
    }

    fun setExitHoldSeconds(context: Context, value: Int) {
        _exitHoldSeconds.value = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_EXIT_HOLD_SECONDS, value).apply()
    }
}
