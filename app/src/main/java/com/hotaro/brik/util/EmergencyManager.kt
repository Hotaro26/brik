package com.hotaro.brik.util

import android.content.Context
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.repository.KeyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EmergencyManager {
    private const val PREFS_NAME = "brik_emergency_prefs"
    private const val KEY_EMERGENCY_ACTIVE = "pref_emergency_active"
    private const val KEY_EMERGENCY_ACTIVATED_AT = "pref_emergency_activated_at"
    private const val KEY_EMERGENCY_KEY_TYPE = "pref_emergency_key_type"
    private const val KEY_EMERGENCY_SECRET_HASH = "pref_emergency_secret_hash"
    private const val KEY_EMERGENCY_QR_TOKEN = "pref_emergency_qr_token"
    private const val KEY_AUTO_DEACTIVATE_SCREEN_LOCK = "pref_auto_deactivate_screen_lock"
    private const val KEY_ALLOW_COMMON_KEY_DPM = "pref_allow_common_key_dpm"

    private val _isEmergencyModeActive = MutableStateFlow(false)
    val isEmergencyModeActive: StateFlow<Boolean> = _isEmergencyModeActive.asStateFlow()

    private val _hasEmergencyKey = MutableStateFlow(false)
    val hasEmergencyKey: StateFlow<Boolean> = _hasEmergencyKey.asStateFlow()

    private val _autoDeactivateOnScreenLock = MutableStateFlow(true)
    val autoDeactivateOnScreenLock: StateFlow<Boolean> = _autoDeactivateOnScreenLock.asStateFlow()

    private val _allowCommonKeyDpm = MutableStateFlow(true)
    val allowCommonKeyDpm: StateFlow<Boolean> = _allowCommonKeyDpm.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isEmergencyModeActive.value = prefs.getBoolean(KEY_EMERGENCY_ACTIVE, false)
        val hash = prefs.getString(KEY_EMERGENCY_SECRET_HASH, null)
        _hasEmergencyKey.value = !hash.isNullOrBlank()
        _autoDeactivateOnScreenLock.value = prefs.getBoolean(KEY_AUTO_DEACTIVATE_SCREEN_LOCK, true)
        _allowCommonKeyDpm.value = prefs.getBoolean(KEY_ALLOW_COMMON_KEY_DPM, true)
    }

    fun hasKey(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hash = prefs.getString(KEY_EMERGENCY_SECRET_HASH, null)
        return !hash.isNullOrBlank()
    }

    fun getKeyType(context: Context): KeyType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val typeStr = prefs.getString(KEY_EMERGENCY_KEY_TYPE, KeyType.PASSWORD.name)
        return runCatching { KeyType.valueOf(typeStr ?: KeyType.PASSWORD.name) }.getOrDefault(KeyType.PASSWORD)
    }

    fun setEmergencyKey(context: Context, keyType: KeyType, secret: String) {
        val hash = QrUtils.sha256(secret)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_EMERGENCY_KEY_TYPE, keyType.name)
            .putString(KEY_EMERGENCY_SECRET_HASH, hash)
            .putString(KEY_EMERGENCY_QR_TOKEN, if (keyType == KeyType.QR_CODE) secret else null)
            .apply()
        _hasEmergencyKey.value = true
    }

    fun verifyEmergencyKey(context: Context, inputSecret: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expectedHash = prefs.getString(KEY_EMERGENCY_SECRET_HASH, null)
        val inputHash = QrUtils.sha256(inputSecret)
        if (expectedHash != null && expectedHash.equals(inputHash, ignoreCase = true)) {
            return true
        }
        if (_allowCommonKeyDpm.value) {
            val dpmLock = KeyRepository(context).getDpmUninstallLock()
            if (dpmLock != null && dpmLock.secretHash.equals(inputHash, ignoreCase = true)) {
                return true
            }
            if (ZenManager.verifySecurityKey(context, inputSecret)) {
                return true
            }
        }
        return false
    }

    fun setAllowCommonKeyDpm(context: Context, enabled: Boolean) {
        _allowCommonKeyDpm.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ALLOW_COMMON_KEY_DPM, enabled).apply()
    }

    fun clearEmergencyKey(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_EMERGENCY_KEY_TYPE)
            .remove(KEY_EMERGENCY_SECRET_HASH)
            .remove(KEY_EMERGENCY_QR_TOKEN)
            .apply()
        _hasEmergencyKey.value = false
    }

    fun setEmergencyModeActive(context: Context, active: Boolean) {
        _isEmergencyModeActive.value = active
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_EMERGENCY_ACTIVE, active)
            .putLong(KEY_EMERGENCY_ACTIVATED_AT, if (active) System.currentTimeMillis() else 0L)
            .apply()
    }

    fun setAutoDeactivateOnScreenLock(context: Context, enabled: Boolean) {
        _autoDeactivateOnScreenLock.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_DEACTIVATE_SCREEN_LOCK, enabled).apply()
    }

    fun onScreenLocked(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val autoDeactivate = prefs.getBoolean(KEY_AUTO_DEACTIVATE_SCREEN_LOCK, true)
        if (autoDeactivate && _isEmergencyModeActive.value) {
            setEmergencyModeActive(context, false)
        }
    }
}
