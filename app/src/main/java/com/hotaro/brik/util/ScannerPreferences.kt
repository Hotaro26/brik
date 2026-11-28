package com.hotaro.brik.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class QrEngine(val displayName: String, val description: String) {
    GOOGLE_ML_KIT("Google ML Kit", "High speed, extreme angle & motion blur tolerance"),
    ZXING("ZXing Engine", "Classic 100% open-source engine with ISBN/EAN support")
}

object ScannerPreferences {
    private const val PREFS_NAME = "brik_scanner_preferences"
    private const val KEY_ENGINE = "pref_qr_engine"

    private val _engine = MutableStateFlow(QrEngine.GOOGLE_ML_KIT)
    val engine: StateFlow<QrEngine> = _engine.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_ENGINE, QrEngine.GOOGLE_ML_KIT.name)
        _engine.value = runCatching { QrEngine.valueOf(saved ?: "") }.getOrDefault(QrEngine.GOOGLE_ML_KIT)
    }

    fun getEngine(context: Context): QrEngine {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_ENGINE, QrEngine.GOOGLE_ML_KIT.name)
        return runCatching { QrEngine.valueOf(saved ?: "") }.getOrDefault(QrEngine.GOOGLE_ML_KIT)
    }

    fun setEngine(context: Context, engine: QrEngine) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ENGINE, engine.name).apply()
        _engine.value = engine
    }
}
