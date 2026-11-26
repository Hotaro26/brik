package com.hotaro.brik.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppLimit(
    val packageName: String,
    val dailyLimitMinutes: Int,   // 0 = no limit
    val blockMode: String         // "hide" or "suspend"
)

class AppLimitRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("brik_app_limits", Context.MODE_PRIVATE)
    private val DELIM = "|"

    private val _limits = MutableStateFlow<Map<String, AppLimit>>(load())
    val limits: StateFlow<Map<String, AppLimit>> = _limits.asStateFlow()

    private fun load(): Map<String, AppLimit> {
        val all = prefs.all
        return all.entries.mapNotNull { (key, value) ->
            val parts = (value as? String)?.split(DELIM) ?: return@mapNotNull null
            if (parts.size < 2) return@mapNotNull null
            val minutes = parts[0].toIntOrNull() ?: return@mapNotNull null
            val mode = parts[1]
            key to AppLimit(key, minutes, mode)
        }.toMap()
    }

    fun setLimit(packageName: String, dailyLimitMinutes: Int, blockMode: String) {
        prefs.edit().putString(packageName, "$dailyLimitMinutes$DELIM$blockMode").apply()
        _limits.value = load()
    }

    fun clearLimit(packageName: String) {
        prefs.edit().remove(packageName).apply()
        _limits.value = load()
    }

    fun getLimit(packageName: String): AppLimit? = _limits.value[packageName]
}
