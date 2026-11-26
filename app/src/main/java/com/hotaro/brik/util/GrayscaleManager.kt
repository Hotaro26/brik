package com.hotaro.brik.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import com.hotaro.brik.data.model.KeyType
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

object GrayscaleManager {
    private const val PREFS_NAME = "brik_grayscale_preferences"
    private const val KEY_SYSTEMWIDE_ENABLED = "pref_systemwide_grayscale_enabled"
    private const val KEY_SYSTEMWIDE_EXCEPTIONS = "pref_systemwide_excepted_apps"
    private const val KEY_APP_SPECIFIC_ENABLED = "pref_app_specific_grayscale_enabled"
    private const val KEY_APP_SPECIFIC_APPS = "pref_app_specific_apps"
    private const val KEY_KEY_GUARD_ENABLED = "pref_grayscale_key_guard_enabled"
    private const val KEY_KEY_TYPE = "pref_grayscale_key_type"
    private const val KEY_SECRET_HASH = "pref_grayscale_secret_hash"
    private const val KEY_QR_TOKEN = "pref_grayscale_qr_token"

    private val _isSystemwideEnabled = MutableStateFlow(false)
    val isSystemwideEnabled: StateFlow<Boolean> = _isSystemwideEnabled.asStateFlow()

    private val _systemwideExceptions = MutableStateFlow<Set<String>>(emptySet())
    val systemwideExceptions: StateFlow<Set<String>> = _systemwideExceptions.asStateFlow()

    private val _isAppSpecificEnabled = MutableStateFlow(false)
    val isAppSpecificEnabled: StateFlow<Boolean> = _isAppSpecificEnabled.asStateFlow()

    private val _appSpecificApps = MutableStateFlow<Set<String>>(emptySet())
    val appSpecificApps: StateFlow<Set<String>> = _appSpecificApps.asStateFlow()

    private val _isKeyGuardEnabled = MutableStateFlow(false)
    val isKeyGuardEnabled: StateFlow<Boolean> = _isKeyGuardEnabled.asStateFlow()

    private val _hasSecurityKey = MutableStateFlow(false)
    val hasSecurityKey: StateFlow<Boolean> = _hasSecurityKey.asStateFlow()

    private var lastForegroundPackage: String? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isSystemwideEnabled.value = prefs.getBoolean(KEY_SYSTEMWIDE_ENABLED, false)
        _systemwideExceptions.value = prefs.getStringSet(KEY_SYSTEMWIDE_EXCEPTIONS, null) ?: emptySet()
        _isAppSpecificEnabled.value = prefs.getBoolean(KEY_APP_SPECIFIC_ENABLED, false)
        _appSpecificApps.value = prefs.getStringSet(KEY_APP_SPECIFIC_APPS, null) ?: emptySet()
        _isKeyGuardEnabled.value = prefs.getBoolean(KEY_KEY_GUARD_ENABLED, false)

        val hash = prefs.getString(KEY_SECRET_HASH, null)
        val repoLock = com.hotaro.brik.data.repository.KeyRepository(context).getSystemSecurityKey("grayscale_security_key")
        if (hash.isNullOrBlank() && repoLock != null) {
            prefs.edit()
                .putString(KEY_KEY_TYPE, repoLock.keyType.name)
                .putString(KEY_SECRET_HASH, repoLock.secretHash)
                .putString(KEY_QR_TOKEN, repoLock.qrToken)
                .apply()
        }
        _hasSecurityKey.value = !hash.isNullOrBlank() || repoLock != null || EmergencyManager.hasKey(context) || ZenManager.hasKey(context)

        // Sync initial state
        evaluateAndApplyGrayscale(context)
    }

    fun onForegroundAppChanged(context: Context, packageName: String) {
        lastForegroundPackage = packageName
        evaluateAndApplyGrayscale(context, packageName)
    }

    fun evaluateAndApplyGrayscale(context: Context, currentPackage: String? = lastForegroundPackage): Boolean {
        val shouldBeGrayscale = when {
            _isSystemwideEnabled.value -> {
                if (currentPackage != null && _systemwideExceptions.value.contains(currentPackage)) {
                    false
                } else {
                    true
                }
            }
            _isAppSpecificEnabled.value -> {
                if (currentPackage != null && _appSpecificApps.value.contains(currentPackage)) {
                    true
                } else {
                    false
                }
            }
            else -> false
        }
        return applyGrayscale(context, shouldBeGrayscale)
    }

    fun setSystemwideEnabled(context: Context, enabled: Boolean): Boolean {
        _isSystemwideEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SYSTEMWIDE_ENABLED, enabled).apply()
        return evaluateAndApplyGrayscale(context)
    }

    fun setSystemwideExceptions(context: Context, packages: Set<String>) {
        _systemwideExceptions.value = packages
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_SYSTEMWIDE_EXCEPTIONS, packages).apply()
        evaluateAndApplyGrayscale(context)
    }

    fun setAppSpecificEnabled(context: Context, enabled: Boolean) {
        _isAppSpecificEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_APP_SPECIFIC_ENABLED, enabled).apply()
        evaluateAndApplyGrayscale(context)
    }

    fun setAppSpecificApps(context: Context, packages: Set<String>) {
        _appSpecificApps.value = packages
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_APP_SPECIFIC_APPS, packages).apply()
        evaluateAndApplyGrayscale(context)
    }

    fun setKeyGuardEnabled(context: Context, enabled: Boolean) {
        _isKeyGuardEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_KEY_GUARD_ENABLED, enabled).apply()
    }

    fun setSecurityKey(context: Context, keyType: KeyType, secret: String) {
        val hash = QrUtils.sha256(secret)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_KEY_TYPE, keyType.name)
            .putString(KEY_SECRET_HASH, hash)
            .putString(KEY_QR_TOKEN, if (keyType == KeyType.QR_CODE) secret else null)
            .apply()
        com.hotaro.brik.data.repository.KeyRepository(context).setSystemSecurityKey(
            id = "grayscale_security_key",
            name = "Grayscale Security Key",
            keyType = keyType,
            secret = secret
        )
        _hasSecurityKey.value = true
    }

    fun hasKey(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hash = prefs.getString(KEY_SECRET_HASH, null)
        val repoLock = com.hotaro.brik.data.repository.KeyRepository(context).getSystemSecurityKey("grayscale_security_key")
        return !hash.isNullOrBlank() || repoLock != null || EmergencyManager.hasKey(context) || ZenManager.hasKey(context)
    }

    fun getKeyType(context: Context): KeyType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val typeStr = prefs.getString(KEY_KEY_TYPE, null)
        if (typeStr != null) {
            return runCatching { KeyType.valueOf(typeStr) }.getOrDefault(KeyType.PASSWORD)
        }
        val repoLock = com.hotaro.brik.data.repository.KeyRepository(context).getSystemSecurityKey("grayscale_security_key")
        if (repoLock != null) {
            return repoLock.keyType
        }
        return EmergencyManager.getKeyType(context)
    }

    fun verifySecurityKey(context: Context, input: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expectedHash = prefs.getString(KEY_SECRET_HASH, null)
            ?: com.hotaro.brik.data.repository.KeyRepository(context).getSystemSecurityKey("grayscale_security_key")?.secretHash
        if (expectedHash.isNullOrBlank()) {
            return EmergencyManager.verifyEmergencyKey(context, input) || ZenManager.verifySecurityKey(context, input)
        }
        val inputHash = QrUtils.sha256(input)
        return expectedHash.equals(inputHash, ignoreCase = true)
    }

    fun clearSecurityKey(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_SECRET_HASH)
            .remove(KEY_QR_TOKEN)
            .remove(KEY_KEY_TYPE)
            .apply()
        com.hotaro.brik.data.repository.KeyRepository(context).removeSystemSecurityKey("grayscale_security_key")
        _hasSecurityKey.value = EmergencyManager.hasKey(context)
    }

    fun hasPermission(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    fun tryGrantPermissionViaPrivileged(context: Context): Boolean {
        if (hasPermission(context)) return true
        val cmd = "pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
        return executePrivilegedCommand(cmd) && hasPermission(context)
    }

    private fun executePrivilegedCommand(cmd: String): Boolean {
        return try {
            if (Shell.isAppGrantedRoot() == true) {
                Shell.cmd(cmd).exec().isSuccess
            } else if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                method.isAccessible = true
                val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
                process.waitFor() == 0
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun applyGrayscale(context: Context, enable: Boolean): Boolean {
        val value = if (enable) 1 else 0

        // Attempt to auto-grant WRITE_SECURE_SETTINGS via privileged bridge if missing
        if (!hasPermission(context)) {
            tryGrantPermissionViaPrivileged(context)
        }

        // Direct Settings.Secure write if permission is present
        val directSuccess = runCatching {
            Settings.Secure.putInt(context.contentResolver, "accessibility_display_daltonizer_enabled", value)
            if (enable) {
                Settings.Secure.putInt(context.contentResolver, "accessibility_display_daltonizer", 0)
            }
            true
        }.getOrDefault(false)

        if (directSuccess) return true

        // Fallback: Privileged shell command (Root or Shizuku)
        val cmd = if (enable) {
            "settings put secure accessibility_display_daltonizer_enabled 1 && settings put secure accessibility_display_daltonizer 0"
        } else {
            "settings put secure accessibility_display_daltonizer_enabled 0"
        }
        return executePrivilegedCommand(cmd)
    }

    fun isSystemGrayscaleActive(context: Context): Boolean {
        return runCatching {
            Settings.Secure.getInt(context.contentResolver, "accessibility_display_daltonizer_enabled", 0) == 1
        }.getOrDefault(false)
    }
}
