package com.hotaro.brik.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.hotaro.brik.ui.theme.ThemePreferences

object StealthUtils {
    private const val PREFS_NAME = "brik_stealth_prefs"
    private const val KEY_DIALER_CODE = "dialer_secret_code"
    const val DEFAULT_CODE = "2745" // Spells "BRIK" on phone keypad

    fun getActiveAliasComponent(context: Context): ComponentName {
        val currentIconId = ThemePreferences.appIcon.value
        val option = AppIconManager.icons.find { it.id == currentIconId } ?: AppIconManager.icons.first()
        return ComponentName(context, option.alias)
    }

    fun isAppHidden(context: Context): Boolean {
        val pm = context.packageManager
        val state = pm.getComponentEnabledSetting(getActiveAliasComponent(context))
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    fun setAppHidden(context: Context, hidden: Boolean) {
        val pm = context.packageManager
        val currentAlias = getActiveAliasComponent(context)
        val newState = if (hidden) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        pm.setComponentEnabledSetting(
            currentAlias,
            newState,
            PackageManager.DONT_KILL_APP
        )
    }

    fun getDialerCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DIALER_CODE, DEFAULT_CODE) ?: DEFAULT_CODE
    }

    fun setDialerCode(context: Context, code: String) {
        val cleanCode = code.filter { it.isDigit() }
        if (cleanCode.isNotEmpty()) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_DIALER_CODE, cleanCode).apply()
        }
    }
}
