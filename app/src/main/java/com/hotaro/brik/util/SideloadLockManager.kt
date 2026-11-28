package com.hotaro.brik.util

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.UserManager
import android.provider.Settings
import com.hotaro.brik.admin.BrikAdminReceiver
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.repository.KeyRepository
import com.hotaro.brik.policy.BridgeType
import com.hotaro.brik.policy.PolicyBridgeManager
import com.rosan.dhizuku.api.Dhizuku
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SideloadLockManager {
    private const val PREFS_NAME = "brik_sideload_lock_prefs"
    private const val KEY_SIDELOAD_RESTRICTION_ACTIVE = "pref_sideload_restriction_active"
    private const val KEY_RESTRICT_UNKNOWN_SOURCES = "pref_restrict_unknown_sources"
    private const val KEY_RESTRICT_PLAY_STORE = "pref_restrict_play_store"
    private const val KEY_RESTRICT_DEVELOPER_OPTIONS = "pref_restrict_developer_options"
    private const val KEY_LOCK_KEY_TYPE = "pref_sideload_lock_key_type"
    private const val KEY_LOCK_SECRET_HASH = "pref_sideload_lock_secret_hash"
    private const val KEY_LOCK_QR_TOKEN = "pref_sideload_lock_qr_token"

    private val _isRestrictionActive = MutableStateFlow(false)
    val isRestrictionActive: StateFlow<Boolean> = _isRestrictionActive.asStateFlow()

    private val _restrictUnknownSources = MutableStateFlow(true)
    val restrictUnknownSources: StateFlow<Boolean> = _restrictUnknownSources.asStateFlow()

    private val _restrictPlayStore = MutableStateFlow(false)
    val restrictPlayStore: StateFlow<Boolean> = _restrictPlayStore.asStateFlow()

    private val _restrictDeveloperOptions = MutableStateFlow(true)
    val restrictDeveloperOptions: StateFlow<Boolean> = _restrictDeveloperOptions.asStateFlow()

    private val _hasSecurityKey = MutableStateFlow(false)
    val hasSecurityKey: StateFlow<Boolean> = _hasSecurityKey.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isRestrictionActive.value = prefs.getBoolean(KEY_SIDELOAD_RESTRICTION_ACTIVE, false)
        _restrictUnknownSources.value = prefs.getBoolean(KEY_RESTRICT_UNKNOWN_SOURCES, true)
        _restrictPlayStore.value = prefs.getBoolean(KEY_RESTRICT_PLAY_STORE, false)
        _restrictDeveloperOptions.value = prefs.getBoolean(KEY_RESTRICT_DEVELOPER_OPTIONS, true)
        val hash = prefs.getString(KEY_LOCK_SECRET_HASH, null)
        val repoLock = KeyRepository(context).getSideloadLock()
        if (hash.isNullOrBlank() && repoLock != null) {
            // Restore from active locks if not in separate prefs
            prefs.edit()
                .putString(KEY_LOCK_KEY_TYPE, repoLock.keyType.name)
                .putString(KEY_LOCK_SECRET_HASH, repoLock.secretHash)
                .putString(KEY_LOCK_QR_TOKEN, repoLock.qrToken)
                .apply()
        }
        _hasSecurityKey.value = !hash.isNullOrBlank() || repoLock != null || EmergencyManager.hasKey(context)

        if (_isRestrictionActive.value) {
            applyPolicies(context, true)
        }
    }

    fun hasKey(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hash = prefs.getString(KEY_LOCK_SECRET_HASH, null)
        val repoLock = KeyRepository(context).getSideloadLock()
        return !hash.isNullOrBlank() || repoLock != null || EmergencyManager.hasKey(context)
    }

    fun getKeyType(context: Context): KeyType {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val typeStr = prefs.getString(KEY_LOCK_KEY_TYPE, null)
        if (typeStr != null) {
            return runCatching { KeyType.valueOf(typeStr) }.getOrDefault(KeyType.PASSWORD)
        }
        val repoLock = KeyRepository(context).getSideloadLock()
        if (repoLock != null) {
            return repoLock.keyType
        }
        return EmergencyManager.getKeyType(context)
    }

    fun setSecurityKey(context: Context, keyType: KeyType, secret: String) {
        val hash = QrUtils.sha256(secret)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LOCK_KEY_TYPE, keyType.name)
            .putString(KEY_LOCK_SECRET_HASH, hash)
            .putString(KEY_LOCK_QR_TOKEN, if (keyType == KeyType.QR_CODE) secret else null)
            .apply()
        KeyRepository(context).setSideloadLockKey(keyType, secret)
        _hasSecurityKey.value = true
    }

    fun verifySecurityKey(context: Context, inputSecret: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expectedHash = prefs.getString(KEY_LOCK_SECRET_HASH, null)
        val repoLock = KeyRepository(context).getSideloadLock()
        val targetHash = expectedHash ?: repoLock?.secretHash
        val inputHash = QrUtils.sha256(inputSecret)
        if (targetHash != null && targetHash.equals(inputHash, ignoreCase = true)) {
            return true
        }
        return EmergencyManager.verifyEmergencyKey(context, inputSecret)
    }

    fun clearSecurityKey(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_LOCK_KEY_TYPE)
            .remove(KEY_LOCK_SECRET_HASH)
            .remove(KEY_LOCK_QR_TOKEN)
            .apply()
        KeyRepository(context).removeSideloadLock()
        _hasSecurityKey.value = EmergencyManager.hasKey(context)
    }

    fun setRestrictUnknownSources(context: Context, enabled: Boolean) {
        _restrictUnknownSources.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_RESTRICT_UNKNOWN_SOURCES, enabled).apply()
        if (_isRestrictionActive.value) {
            applyPolicies(context, true)
        }
    }

    fun setRestrictPlayStore(context: Context, enabled: Boolean) {
        _restrictPlayStore.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_RESTRICT_PLAY_STORE, enabled).apply()
        if (_isRestrictionActive.value) {
            applyPolicies(context, true)
        }
    }

    fun setRestrictDeveloperOptions(context: Context, enabled: Boolean) {
        _restrictDeveloperOptions.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_RESTRICT_DEVELOPER_OPTIONS, enabled).apply()
        if (_isRestrictionActive.value) {
            applyPolicies(context, true)
        }
    }

    fun setRestrictionActive(context: Context, active: Boolean): Result<Boolean> {
        val result = applyPolicies(context, active)
        if (result.isSuccess) {
            _isRestrictionActive.value = active
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_SIDELOAD_RESTRICTION_ACTIVE, active).apply()
        }
        return result
    }

    private fun applyPolicies(context: Context, active: Boolean): Result<Boolean> {
        val status = PolicyBridgeManager.getBridgeStatus(context)
        return runCatching {
            when (status.activeBridge) {
                BridgeType.DEVICE_OWNER -> {
                    val dpm = context.getSystemService(DevicePolicyManager::class.java)!!
                    val dar = BrikAdminReceiver.getComponentName(context)

                    if (active) {
                        if (_restrictUnknownSources.value) {
                            dpm.addUserRestriction(dar, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                        } else {
                            dpm.clearUserRestriction(dar, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                        }

                        if (_restrictPlayStore.value) {
                            dpm.addUserRestriction(dar, UserManager.DISALLOW_INSTALL_APPS)
                        } else {
                            dpm.clearUserRestriction(dar, UserManager.DISALLOW_INSTALL_APPS)
                        }

                        if (_restrictDeveloperOptions.value) {
                            dpm.addUserRestriction(dar, UserManager.DISALLOW_DEBUGGING_FEATURES)
                            runCatching {
                                dpm.setGlobalSetting(dar, Settings.Global.ADB_ENABLED, "0")
                                dpm.setGlobalSetting(dar, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, "0")
                            }
                        } else {
                            dpm.clearUserRestriction(dar, UserManager.DISALLOW_DEBUGGING_FEATURES)
                        }
                    } else {
                        dpm.clearUserRestriction(dar, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                        dpm.clearUserRestriction(dar, UserManager.DISALLOW_INSTALL_APPS)
                        dpm.clearUserRestriction(dar, UserManager.DISALLOW_DEBUGGING_FEATURES)
                    }
                    true
                }
                BridgeType.DHIZUKU -> {
                    val dpm = PolicyBridgeManager.getDhizukuDpm(context)
                    val dar = Dhizuku.getOwnerComponent()

                    if (active) {
                        if (_restrictUnknownSources.value) {
                            dpm.addUserRestriction(dar, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                        } else {
                            dpm.clearUserRestriction(dar, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                        }

                        if (_restrictPlayStore.value) {
                            dpm.addUserRestriction(dar, UserManager.DISALLOW_INSTALL_APPS)
                        } else {
                            dpm.clearUserRestriction(dar, UserManager.DISALLOW_INSTALL_APPS)
                        }

                        if (_restrictDeveloperOptions.value) {
                            dpm.addUserRestriction(dar, UserManager.DISALLOW_DEBUGGING_FEATURES)
                            runCatching {
                                dpm.setGlobalSetting(dar, Settings.Global.ADB_ENABLED, "0")
                                dpm.setGlobalSetting(dar, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, "0")
                            }
                        } else {
                            dpm.clearUserRestriction(dar, UserManager.DISALLOW_DEBUGGING_FEATURES)
                        }
                    } else {
                        dpm.clearUserRestriction(dar, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
                        dpm.clearUserRestriction(dar, UserManager.DISALLOW_INSTALL_APPS)
                        dpm.clearUserRestriction(dar, UserManager.DISALLOW_DEBUGGING_FEATURES)
                    }
                    true
                }
                else -> {
                    throw IllegalStateException("Device Owner or Dhizuku privilege is required to manage system restrictions.")
                }
            }
        }
    }

    private const val KEY_TEMP_KIOSK_ADB_BLOCK = "pref_temp_kiosk_adb_block"

    fun setTemporaryKioskAdbBlock(context: Context, block: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentlyTempBlocked = prefs.getBoolean(KEY_TEMP_KIOSK_ADB_BLOCK, false)
        if (!block && !currentlyTempBlocked) return

        val status = PolicyBridgeManager.getBridgeStatus(context)
        runCatching {
            when (status.activeBridge) {
                BridgeType.DEVICE_OWNER -> {
                    val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return
                    val dar = BrikAdminReceiver.getComponentName(context)
                    if (block) {
                        dpm.addUserRestriction(dar, UserManager.DISALLOW_DEBUGGING_FEATURES)
                        runCatching { dpm.setGlobalSetting(dar, Settings.Global.ADB_ENABLED, "0") }
                        prefs.edit().putBoolean(KEY_TEMP_KIOSK_ADB_BLOCK, true).apply()
                    } else {
                        prefs.edit().putBoolean(KEY_TEMP_KIOSK_ADB_BLOCK, false).apply()
                        if (!(_isRestrictionActive.value && _restrictDeveloperOptions.value)) {
                            dpm.clearUserRestriction(dar, UserManager.DISALLOW_DEBUGGING_FEATURES)
                            runCatching { dpm.setGlobalSetting(dar, Settings.Global.ADB_ENABLED, "1") }
                        }
                    }
                }
                BridgeType.DHIZUKU -> {
                    val dpm = PolicyBridgeManager.getDhizukuDpm(context)
                    val dar = Dhizuku.getOwnerComponent()
                    if (block) {
                        dpm.addUserRestriction(dar, UserManager.DISALLOW_DEBUGGING_FEATURES)
                        runCatching { dpm.setGlobalSetting(dar, Settings.Global.ADB_ENABLED, "0") }
                        prefs.edit().putBoolean(KEY_TEMP_KIOSK_ADB_BLOCK, true).apply()
                    } else {
                        prefs.edit().putBoolean(KEY_TEMP_KIOSK_ADB_BLOCK, false).apply()
                        if (!(_isRestrictionActive.value && _restrictDeveloperOptions.value)) {
                            dpm.clearUserRestriction(dar, UserManager.DISALLOW_DEBUGGING_FEATURES)
                            runCatching { dpm.setGlobalSetting(dar, Settings.Global.ADB_ENABLED, "1") }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}
