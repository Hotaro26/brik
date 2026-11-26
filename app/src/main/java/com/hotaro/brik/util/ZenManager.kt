package com.hotaro.brik.util

import android.app.Activity
import android.content.Context
import android.provider.Telephony
import android.telecom.TelecomManager
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.policy.PolicyBridgeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ZenManager {
    private const val PREFS_NAME = "brik_zen_preferences"
    private const val KEY_ZEN_ACTIVE = "pref_zen_active"
    private const val KEY_ZEN_START_TIME = "pref_zen_start_time"
    private const val KEY_ZEN_END_TIME = "pref_zen_end_time"
    private const val KEY_ZEN_IS_INFINITE = "pref_zen_is_infinite"
    private const val KEY_ZEN_ALLOWED_PACKAGES = "pref_zen_allowed_packages"
    private const val KEY_ZEN_USE_KIOSK = "pref_zen_use_kiosk"
    private const val KEY_ZEN_FLOATING_CONTROLS = "pref_zen_floating_controls"
    private const val KEY_ZEN_FLOATING_AUTOHIDE_SECONDS = "pref_zen_floating_autohide_seconds"
    private const val KEY_ZEN_KEY_TYPE = "pref_zen_key_type"
    private const val KEY_ZEN_KEY_NAME = "pref_zen_key_name"
    private const val KEY_ZEN_SECRET_HASH = "pref_zen_secret_hash"
    private const val KEY_ZEN_QR_TOKEN = "pref_zen_qr_token"
    private const val KEY_ZEN_HOLD_DURATION_SECONDS = "pref_zen_hold_duration_seconds"
    private const val KEY_ZEN_CONFIGURED_DURATION = "pref_zen_configured_duration"
    private const val KEY_ZEN_CONFIGURED_INFINITE = "pref_zen_configured_infinite"

    private val _isZenActive = MutableStateFlow(false)
    val isZenActive: StateFlow<Boolean> = _isZenActive.asStateFlow()

    private val _isInfinite = MutableStateFlow(false)
    val isInfinite: StateFlow<Boolean> = _isInfinite.asStateFlow()

    private val _sessionEndTime = MutableStateFlow(0L)
    val sessionEndTime: StateFlow<Long> = _sessionEndTime.asStateFlow()

    private val _allowedPackages = MutableStateFlow<Set<String>>(emptySet())
    val allowedPackages: StateFlow<Set<String>> = _allowedPackages.asStateFlow()

    private val _useKiosk = MutableStateFlow(true)
    val useKiosk: StateFlow<Boolean> = _useKiosk.asStateFlow()

    private val _floatingControlsEnabled = MutableStateFlow(true)
    val floatingControlsEnabled: StateFlow<Boolean> = _floatingControlsEnabled.asStateFlow()

    private val _floatingControlsAutoHideSeconds = MutableStateFlow(3)
    val floatingControlsAutoHideSeconds: StateFlow<Int> = _floatingControlsAutoHideSeconds.asStateFlow()

    private val _hasSecurityKey = MutableStateFlow(false)
    val hasSecurityKey: StateFlow<Boolean> = _hasSecurityKey.asStateFlow()

    private val _holdDurationSeconds = MutableStateFlow(3)
    val holdDurationSeconds: StateFlow<Int> = _holdDurationSeconds.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val active = prefs.getBoolean(KEY_ZEN_ACTIVE, false)
        val infinite = prefs.getBoolean(KEY_ZEN_IS_INFINITE, false)
        val endTime = prefs.getLong(KEY_ZEN_END_TIME, 0L)
        val now = System.currentTimeMillis()

        if (active && !infinite && endTime > 0L && now >= endTime) {
            _isZenActive.value = false
            prefs.edit().putBoolean(KEY_ZEN_ACTIVE, false).apply()
        } else {
            _isZenActive.value = active
        }

        _isInfinite.value = infinite
        _sessionEndTime.value = endTime
        val savedAllowed = prefs.getStringSet(KEY_ZEN_ALLOWED_PACKAGES, null)
        _allowedPackages.value = if (!savedAllowed.isNullOrEmpty()) savedAllowed else getDefaultEssentialPackages(context)
        _useKiosk.value = prefs.getBoolean(KEY_ZEN_USE_KIOSK, true)
        _floatingControlsEnabled.value = prefs.getBoolean(KEY_ZEN_FLOATING_CONTROLS, true)
        _floatingControlsAutoHideSeconds.value = prefs.getInt(KEY_ZEN_FLOATING_AUTOHIDE_SECONDS, 3)
        val hash = prefs.getString(KEY_ZEN_SECRET_HASH, null)
        _hasSecurityKey.value = !hash.isNullOrBlank() || EmergencyManager.hasKey(context)
        _holdDurationSeconds.value = prefs.getInt(KEY_ZEN_HOLD_DURATION_SECONDS, 3)
    }

    fun getDefaultEssentialPackages(context: Context): Set<String> {
        val set = mutableSetOf<String>()
        try {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val dialer = telecom?.defaultDialerPackage
            if (!dialer.isNullOrBlank()) set.add(dialer)
        } catch (_: Exception) {}

        try {
            val sms = Telephony.Sms.getDefaultSmsPackage(context)
            if (!sms.isNullOrBlank()) set.add(sms)
        } catch (_: Exception) {}

        return set
    }

    fun setAllowedPackages(context: Context, packages: Set<String>) {
        val capped = packages.take(10).toSet()
        _allowedPackages.value = capped
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_ZEN_ALLOWED_PACKAGES, capped).apply()
    }

    fun setUseKiosk(context: Context, useKiosk: Boolean) {
        _useKiosk.value = useKiosk
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ZEN_USE_KIOSK, useKiosk).apply()
    }

    fun setFloatingControlsEnabled(context: Context, enabled: Boolean) {
        _floatingControlsEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ZEN_FLOATING_CONTROLS, enabled).apply()
    }

    fun setFloatingControlsAutoHideSeconds(context: Context, seconds: Int) {
        _floatingControlsAutoHideSeconds.value = seconds
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_ZEN_FLOATING_AUTOHIDE_SECONDS, seconds).apply()
    }

    fun setHoldDurationSeconds(context: Context, seconds: Int) {
        _holdDurationSeconds.value = seconds
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_ZEN_HOLD_DURATION_SECONDS, seconds).apply()
    }

    fun getConfiguredDuration(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ZEN_CONFIGURED_DURATION, 30)
    }

    fun setConfiguredDuration(context: Context, duration: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_ZEN_CONFIGURED_DURATION, duration).apply()
    }

    fun isConfiguredInfinite(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ZEN_CONFIGURED_INFINITE, false)
    }

    fun setConfiguredInfinite(context: Context, isInfinite: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ZEN_CONFIGURED_INFINITE, isInfinite).apply()
    }

    fun startZenSession(
        context: Context,
        activity: Activity?,
        isInfinite: Boolean,
        durationMinutes: Int,
        allowedPackages: Set<String>,
        useKiosk: Boolean
    ) {
        val now = System.currentTimeMillis()
        val endTime = if (isInfinite) 0L else now + (durationMinutes * 60 * 1000L)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ZEN_ACTIVE, true)
            .putLong(KEY_ZEN_START_TIME, now)
            .putLong(KEY_ZEN_END_TIME, endTime)
            .putBoolean(KEY_ZEN_IS_INFINITE, isInfinite)
            .putStringSet(KEY_ZEN_ALLOWED_PACKAGES, allowedPackages)
            .putBoolean(KEY_ZEN_USE_KIOSK, useKiosk)
            .putInt(KEY_ZEN_CONFIGURED_DURATION, durationMinutes)
            .putBoolean(KEY_ZEN_CONFIGURED_INFINITE, isInfinite)
            .apply()

        _isZenActive.value = true
        _isInfinite.value = isInfinite
        _sessionEndTime.value = endTime
        _allowedPackages.value = allowedPackages
        _useKiosk.value = useKiosk

        if (useKiosk) {
            try {
                val packagesToAllow = (allowedPackages + context.packageName).toList()
                PolicyBridgeManager.setLockTaskPackages(context, packagesToAllow)
                activity?.startLockTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun endZenSession(context: Context, activity: Activity?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ZEN_ACTIVE, false)
            .putLong(KEY_ZEN_END_TIME, 0L)
            .apply()

        _isZenActive.value = false
        _sessionEndTime.value = 0L

        try {
            activity?.stopLockTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isSessionExpired(): Boolean {
        if (_isInfinite.value) return false
        val endTime = _sessionEndTime.value
        return endTime > 0L && System.currentTimeMillis() >= endTime
    }

    fun setSecurityKey(context: Context, keyType: KeyType, secret: String, keyName: String? = null) {
        val hash = QrUtils.sha256(secret)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ZEN_KEY_TYPE, keyType.name)
            .putString(KEY_ZEN_SECRET_HASH, hash)
            .putString(KEY_ZEN_QR_TOKEN, if (keyType == KeyType.QR_CODE) secret else null)
            .putString(KEY_ZEN_KEY_NAME, keyName)
            .apply()
        com.hotaro.brik.data.repository.KeyRepository(context).setSystemSecurityKey(
            id = "zen_security_key",
            name = keyName ?: "Zen Mode Security Key",
            keyType = keyType,
            secret = secret
        )
        _hasSecurityKey.value = true
    }

    fun getKeyType(context: Context): KeyType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val typeStr = prefs.getString(KEY_ZEN_KEY_TYPE, null)
        if (typeStr != null) {
            return runCatching { KeyType.valueOf(typeStr) }.getOrDefault(KeyType.PASSWORD)
        }
        return EmergencyManager.getKeyType(context)
    }

    fun getActiveKeyName(context: Context, savedKeys: List<com.hotaro.brik.data.model.SavedKey> = emptyList()): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customName = prefs.getString(KEY_ZEN_KEY_NAME, null)
        if (!customName.isNullOrBlank()) return customName

        val qrToken = prefs.getString(KEY_ZEN_QR_TOKEN, null)
        if (!qrToken.isNullOrBlank()) {
            val saved = savedKeys.find { it.token == qrToken }
            if (saved != null) return saved.name
            return "QR Key"
        }

        val hash = prefs.getString(KEY_ZEN_SECRET_HASH, null)
        if (!hash.isNullOrBlank()) {
            val type = getKeyType(context)
            return if (type == KeyType.PASSWORD) "Password" else "QR Key"
        }

        // Shared key fallback: Device Owner / Emergency key
        val dpmLock = com.hotaro.brik.data.repository.KeyRepository(context).getDpmUninstallLock()
        if (dpmLock != null && !dpmLock.secretHash.isNullOrBlank()) {
            if (!dpmLock.qrToken.isNullOrBlank()) {
                val saved = savedKeys.find { it.token == dpmLock.qrToken }
                if (saved != null) return saved.name
                return "Device Owner (QR)"
            }
            return "Device Owner Key"
        }

        val dpmPrefs = context.getSharedPreferences("brik_admin_pref", Context.MODE_PRIVATE)
        val dpmHash = dpmPrefs.getString("pref_dpm_uninstall_secret_hash", null)
        if (!dpmHash.isNullOrBlank()) {
            val dpmQr = dpmPrefs.getString("pref_dpm_uninstall_qr_token", null)
            if (!dpmQr.isNullOrBlank()) {
                val saved = savedKeys.find { it.token == dpmQr }
                if (saved != null) return saved.name
                return "Device Owner (QR)"
            }
            return "Device Owner Key"
        }

        if (EmergencyManager.hasKey(context)) {
            val emType = EmergencyManager.getKeyType(context)
            return if (emType == KeyType.PASSWORD) "Emergency Password" else "Emergency QR"
        }

        return "No Key Set"
    }

    fun verifySecurityKey(context: Context, input: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expectedHash = prefs.getString(KEY_ZEN_SECRET_HASH, null)
        if (expectedHash.isNullOrBlank()) {
            return EmergencyManager.verifyEmergencyKey(context, input)
        }
        val inputHash = QrUtils.sha256(input)
        return expectedHash.equals(inputHash, ignoreCase = true)
    }

    fun clearSecurityKey(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_ZEN_SECRET_HASH)
            .remove(KEY_ZEN_QR_TOKEN)
            .remove(KEY_ZEN_KEY_TYPE)
            .remove(KEY_ZEN_KEY_NAME)
            .apply()
        com.hotaro.brik.data.repository.KeyRepository(context).removeSystemSecurityKey("zen_security_key")
        _hasSecurityKey.value = EmergencyManager.hasKey(context)
    }

    fun hasKey(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hash = prefs.getString(KEY_ZEN_SECRET_HASH, null)
        return !hash.isNullOrBlank() || EmergencyManager.hasKey(context)
    }
}
