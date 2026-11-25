package com.hotaro.brik.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EmergencyManager {
    private const val PREFS_NAME = "brik_emergency_prefs"
    private const val KEY_EMERGENCY_ACTIVE = "pref_emergency_active"
    private const val KEY_EMERGENCY_ACTIVATED_AT = "pref_emergency_activated_at"

    private val _isEmergencyModeActive = MutableStateFlow(false)
    val isEmergencyModeActive: StateFlow<Boolean> = _isEmergencyModeActive.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isEmergencyModeActive.value = prefs.getBoolean(KEY_EMERGENCY_ACTIVE, false)
    }

    fun setEmergencyModeActive(context: Context, active: Boolean) {
        _isEmergencyModeActive.value = active
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_EMERGENCY_ACTIVE, active)
            .putLong(KEY_EMERGENCY_ACTIVATED_AT, if (active) System.currentTimeMillis() else 0L)
            .apply()
    }
}
