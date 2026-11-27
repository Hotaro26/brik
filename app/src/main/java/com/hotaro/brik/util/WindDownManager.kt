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
import java.util.Calendar

object WindDownManager {
    private const val PREFS_NAME = "brik_wind_down_preferences"
    private const val KEY_ENABLED = "pref_wd_enabled"
    private const val KEY_SLEEP_HOUR = "pref_wd_sleep_hour"
    private const val KEY_SLEEP_MINUTE = "pref_wd_sleep_minute"
    private const val KEY_WAKE_HOUR = "pref_wd_wake_hour"
    private const val KEY_WAKE_MINUTE = "pref_wd_wake_minute"
    private const val KEY_NO_APPS_MODE = "pref_wd_no_apps_mode"
    private const val KEY_ALLOWED_PACKAGES = "pref_wd_allowed_packages"
    private const val KEY_ALLOW_EMERGENCY = "pref_wd_allow_emergency"
    private const val KEY_KEY_TYPE = "pref_wd_key_type"
    private const val KEY_SECRET_HASH = "pref_wd_secret_hash"
    private const val KEY_QR_TOKEN = "pref_wd_qr_token"
    private const val KEY_IS_PREVIEW = "pref_wd_is_preview"
    private const val KEY_FLOATING_CONTROLS = "pref_wd_floating_controls"
    private const val KEY_FLOATING_AUTOHIDE_SECONDS = "pref_wd_floating_autohide_seconds"

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    private val _floatingControlsEnabled = MutableStateFlow(true)
    val floatingControlsEnabled: StateFlow<Boolean> = _floatingControlsEnabled.asStateFlow()

    private val _floatingControlsAutoHideSeconds = MutableStateFlow(3)
    val floatingControlsAutoHideSeconds: StateFlow<Int> = _floatingControlsAutoHideSeconds.asStateFlow()

    private val _sleepHour = MutableStateFlow(23)
    val sleepHour: StateFlow<Int> = _sleepHour.asStateFlow()

    private val _sleepMinute = MutableStateFlow(0)
    val sleepMinute: StateFlow<Int> = _sleepMinute.asStateFlow()

    private val _wakeHour = MutableStateFlow(7)
    val wakeHour: StateFlow<Int> = _wakeHour.asStateFlow()

    private val _wakeMinute = MutableStateFlow(0)
    val wakeMinute: StateFlow<Int> = _wakeMinute.asStateFlow()

    private val _isNoAppsMode = MutableStateFlow(false)
    val isNoAppsMode: StateFlow<Boolean> = _isNoAppsMode.asStateFlow()

    private val _allowedPackages = MutableStateFlow<Set<String>>(emptySet())
    val allowedPackages: StateFlow<Set<String>> = _allowedPackages.asStateFlow()

    private val _allowEmergencyUnlock = MutableStateFlow(true)
    val allowEmergencyUnlock: StateFlow<Boolean> = _allowEmergencyUnlock.asStateFlow()

    private val _hasSecurityKey = MutableStateFlow(false)
    val hasSecurityKey: StateFlow<Boolean> = _hasSecurityKey.asStateFlow()

    private val _isWindDownActive = MutableStateFlow(false)
    val isWindDownActive: StateFlow<Boolean> = _isWindDownActive.asStateFlow()

    private val _isPreviewMode = MutableStateFlow(false)
    val isPreviewMode: StateFlow<Boolean> = _isPreviewMode.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isEnabled.value = prefs.getBoolean(KEY_ENABLED, false)
        _sleepHour.value = prefs.getInt(KEY_SLEEP_HOUR, 23)
        _sleepMinute.value = prefs.getInt(KEY_SLEEP_MINUTE, 0)
        _wakeHour.value = prefs.getInt(KEY_WAKE_HOUR, 7)
        _wakeMinute.value = prefs.getInt(KEY_WAKE_MINUTE, 0)
        _isNoAppsMode.value = prefs.getBoolean(KEY_NO_APPS_MODE, false)
        _allowEmergencyUnlock.value = prefs.getBoolean(KEY_ALLOW_EMERGENCY, true)
        _floatingControlsEnabled.value = prefs.getBoolean(KEY_FLOATING_CONTROLS, true)
        _floatingControlsAutoHideSeconds.value = prefs.getInt(KEY_FLOATING_AUTOHIDE_SECONDS, 3)

        val hash = prefs.getString(KEY_SECRET_HASH, null)
        _hasSecurityKey.value = !hash.isNullOrBlank() || ZenManager.hasKey(context) || EmergencyManager.hasKey(context)

        val savedAllowed = prefs.getStringSet(KEY_ALLOWED_PACKAGES, null)
        _allowedPackages.value = if (!savedAllowed.isNullOrEmpty()) {
            savedAllowed.take(10).toSet()
        } else {
            getDefaultEssentialPackages(context)
        }

        checkActive(context, null)
    }

    fun setEnabled(context: Context, enabled: Boolean, activity: Activity? = null) {
        _isEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        checkActive(context, activity)
    }

    fun setSchedule(context: Context, sleepH: Int, sleepM: Int, wakeH: Int, wakeM: Int, activity: Activity? = null) {
        _sleepHour.value = sleepH
        _sleepMinute.value = sleepM
        _wakeHour.value = wakeH
        _wakeMinute.value = wakeM

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_SLEEP_HOUR, sleepH)
            .putInt(KEY_SLEEP_MINUTE, sleepM)
            .putInt(KEY_WAKE_HOUR, wakeH)
            .putInt(KEY_WAKE_MINUTE, wakeM)
            .apply()

        checkActive(context, activity)
    }

    fun setNoAppsMode(context: Context, noApps: Boolean) {
        _isNoAppsMode.value = noApps
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NO_APPS_MODE, noApps).apply()
    }

    fun setAllowedPackages(context: Context, packages: Set<String>) {
        val capped = packages.take(10).toSet()
        _allowedPackages.value = capped
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_ALLOWED_PACKAGES, capped).apply()
    }

    fun setAllowEmergencyUnlock(context: Context, allow: Boolean) {
        _allowEmergencyUnlock.value = allow
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ALLOW_EMERGENCY, allow).apply()
    }

    fun setFloatingControlsEnabled(context: Context, enabled: Boolean) {
        _floatingControlsEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FLOATING_CONTROLS, enabled).apply()
    }

    fun setFloatingControlsAutoHideSeconds(context: Context, seconds: Int) {
        _floatingControlsAutoHideSeconds.value = seconds
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_FLOATING_AUTOHIDE_SECONDS, seconds).apply()
    }

    fun setSecurityKey(context: Context, keyType: KeyType, secret: String) {
        val hash = QrUtils.sha256(secret)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_KEY_TYPE, keyType.name)
            .putString(KEY_SECRET_HASH, hash)
            .putString(KEY_QR_TOKEN, if (keyType == KeyType.QR_CODE) secret else null)
            .apply()
        _hasSecurityKey.value = true
    }

    fun getKeyType(context: Context): KeyType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val typeStr = prefs.getString(KEY_KEY_TYPE, null)
        if (typeStr != null) {
            return runCatching { KeyType.valueOf(typeStr) }.getOrDefault(KeyType.PASSWORD)
        }
        return ZenManager.getKeyType(context)
    }

    fun verifySecurityKey(context: Context, input: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expectedHash = prefs.getString(KEY_SECRET_HASH, null)
        if (expectedHash.isNullOrBlank()) {
            // Fall back to Zen or Emergency manager key
            return ZenManager.verifySecurityKey(context, input)
        }
        val inputHash = QrUtils.sha256(input)
        return expectedHash.equals(inputHash, ignoreCase = true)
    }

    fun hasKey(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hash = prefs.getString(KEY_SECRET_HASH, null)
        return !hash.isNullOrBlank() || ZenManager.hasKey(context) || EmergencyManager.hasKey(context)
    }

    fun startPreviewMode(context: Context, activity: Activity?) {
        _isPreviewMode.value = true
        _isWindDownActive.value = true
        engageKiosk(context, activity)
    }

    fun stopPreviewMode(activity: Activity?) {
        _isPreviewMode.value = false
        _isWindDownActive.value = false
        disengageKiosk(activity)
    }

    fun checkActive(context: Context, activity: Activity?): Boolean {
        if (_isPreviewMode.value) {
            _isWindDownActive.value = true
            return true
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        if (!enabled) {
            if (_isWindDownActive.value) {
                _isWindDownActive.value = false
                disengageKiosk(activity)
            }
            return false
        }

        val cal = Calendar.getInstance()
        val nowH = cal.get(Calendar.HOUR_OF_DAY)
        val nowM = cal.get(Calendar.MINUTE)
        val sleepH = prefs.getInt(KEY_SLEEP_HOUR, 23)
        val sleepM = prefs.getInt(KEY_SLEEP_MINUTE, 0)
        val wakeH = prefs.getInt(KEY_WAKE_HOUR, 7)
        val wakeM = prefs.getInt(KEY_WAKE_MINUTE, 0)

        val shouldBeActive = isWithinWindDownWindow(nowH, nowM, sleepH, sleepM, wakeH, wakeM)

        if (shouldBeActive && !_isWindDownActive.value) {
            _isWindDownActive.value = true
            engageKiosk(context, activity)
        } else if (!shouldBeActive && _isWindDownActive.value) {
            _isWindDownActive.value = false
            disengageKiosk(activity)
        }

        return shouldBeActive
    }

    fun isWithinWindDownWindow(nowH: Int, nowM: Int, sleepH: Int, sleepM: Int, wakeH: Int, wakeM: Int): Boolean {
        val nowMinutes = nowH * 60 + nowM
        val sleepMinutes = sleepH * 60 + sleepM
        val wakeMinutes = wakeH * 60 + wakeM

        return if (sleepMinutes <= wakeMinutes) {
            nowMinutes in sleepMinutes until wakeMinutes
        } else {
            // Crosses midnight (e.g., 23:00 to 07:00)
            nowMinutes >= sleepMinutes || nowMinutes < wakeMinutes
        }
    }

    fun getMillisUntilWake(now: Long, wakeH: Int, wakeM: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, wakeH)
            set(Calendar.MINUTE, wakeM)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return (cal.timeInMillis - now).coerceAtLeast(0L)
    }

    private fun engageKiosk(context: Context, activity: Activity?) {
        try {
            val packagesToAllow = if (_isNoAppsMode.value) {
                listOf(context.packageName)
            } else {
                (_allowedPackages.value + context.packageName).toList()
            }
            PolicyBridgeManager.setLockTaskPackages(context, packagesToAllow)
            PolicyBridgeManager.setLockTaskFeatures(
                context = context,
                disableSystemGestures = true,
                lockdownNotificationShade = !KioskPreferences.allowControlPanelsStatusBar.value
            )
            activity?.startLockTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun disengageKiosk(activity: Activity?) {
        try {
            activity?.stopLockTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
}
