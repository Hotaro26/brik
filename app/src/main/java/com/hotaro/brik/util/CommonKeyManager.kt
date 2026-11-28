package com.hotaro.brik.util

import android.content.Context
import com.hotaro.brik.R
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.repository.KeyRepository
import com.hotaro.brik.policy.DnsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CommonKeyOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconRes: Int
)

object CommonKeyManager {
    private const val PREFS_NAME = "brik_common_key_prefs"
    private const val KEY_TYPE = "pref_common_key_type"
    private const val KEY_SECRET_HASH = "pref_common_secret_hash"
    private const val KEY_QR_TOKEN = "pref_common_qr_token"
    private const val PREFIX_FOLLOW = "follow_"

    val ALL_OPTIONS = listOf(
        CommonKeyOption(
            id = "app_manager_security_key",
            title = "App Manager Guard",
            subtitle = "Require key to modify or turn off app restrictions",
            iconRes = R.drawable.apps_fill0
        ),
        CommonKeyOption(
            id = "grayscale_security_key",
            title = "Grayscale Guard",
            subtitle = "Require key to disable monochrome display filter",
            iconRes = R.drawable.contrast_fill0
        ),
        CommonKeyOption(
            id = "zen_security_key",
            title = "Zen Mode Lock",
            subtitle = "Require key to exit or abort running Zen session",
            iconRes = R.drawable.self_improvement_fill0
        ),
        CommonKeyOption(
            id = "wind_down_security_key",
            title = "Bedtime / Wind Down",
            subtitle = "Require key to unlock during bedtime hours",
            iconRes = R.drawable.dark_mode_fill0
        ),
        CommonKeyOption(
            id = "sideload_adb_lock",
            title = "Sideload & ADB Lock",
            subtitle = "Require key to alter APK sideload and ADB policies",
            iconRes = R.drawable.screen_lock_portrait_fill0
        ),
        CommonKeyOption(
            id = "dns_lock",
            title = "DNS Protection",
            subtitle = "Require key to modify private DNS blocking rules",
            iconRes = R.drawable.dns_fill0
        ),
        CommonKeyOption(
            id = "bags_common_key",
            title = "App Bags",
            subtitle = "Follow Common Key by default for app bags",
            iconRes = R.drawable.backpack_fill0
        )
    )

    private val _hasCommonKey = MutableStateFlow(false)
    val hasCommonKey: StateFlow<Boolean> = _hasCommonKey.asStateFlow()

    private val _commonKeyType = MutableStateFlow(KeyType.PASSWORD)
    val commonKeyType: StateFlow<KeyType> = _commonKeyType.asStateFlow()

    private val _followingMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val followingMap: StateFlow<Map<String, Boolean>> = _followingMap.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hash = prefs.getString(KEY_SECRET_HASH, null)
        _hasCommonKey.value = !hash.isNullOrBlank()
        val typeStr = prefs.getString(KEY_TYPE, null)
        _commonKeyType.value = runCatching {
            if (typeStr != null) KeyType.valueOf(typeStr) else KeyType.PASSWORD
        }.getOrDefault(KeyType.PASSWORD)

        val map = mutableMapOf<String, Boolean>()
        ALL_OPTIONS.forEach { opt ->
            map[opt.id] = prefs.getBoolean(PREFIX_FOLLOW + opt.id, false)
        }
        _followingMap.value = map
    }

    fun isFollowing(context: Context, featureId: String): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREFIX_FOLLOW + featureId, false)
    }

    fun isFollowingAndReady(context: Context, featureId: String): Boolean {
        return isFollowing(context, featureId) && hasKey(context)
    }

    fun hasKey(context: Context): Boolean {
        val hash = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SECRET_HASH, null)
        return !hash.isNullOrBlank()
    }

    fun getKeyType(context: Context): KeyType {
        val typeStr = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TYPE, null)
        return runCatching {
            if (typeStr != null) KeyType.valueOf(typeStr) else KeyType.PASSWORD
        }.getOrDefault(KeyType.PASSWORD)
    }

    fun getSecretHash(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SECRET_HASH, null)
    }

    fun getQrToken(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_QR_TOKEN, null)
    }

    fun verifyCommonKey(context: Context, input: String): Boolean {
        val expectedHash = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SECRET_HASH, null) ?: return false
        return expectedHash.equals(QrUtils.sha256(input), ignoreCase = true)
    }

    fun setCommonKey(context: Context, keyType: KeyType, secret: String) {
        val hash = QrUtils.sha256(secret)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TYPE, keyType.name)
            .putString(KEY_SECRET_HASH, hash)
            .putString(KEY_QR_TOKEN, if (keyType == KeyType.QR_CODE) secret else null)
            .apply()
        _hasCommonKey.value = true
        _commonKeyType.value = keyType

        // Sync to all features currently toggled to follow Common Key
        ALL_OPTIONS.forEach { opt ->
            if (isFollowing(context, opt.id)) {
                syncKeyToFeature(context, opt.id, keyType, secret)
            }
        }
    }

    fun clearCommonKey(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        ALL_OPTIONS.forEach { opt ->
            if (prefs.getBoolean(PREFIX_FOLLOW + opt.id, false)) {
                clearFeatureKey(context, opt.id)
                editor.putBoolean(PREFIX_FOLLOW + opt.id, false)
            }
        }
        editor
            .remove(KEY_TYPE)
            .remove(KEY_SECRET_HASH)
            .remove(KEY_QR_TOKEN)
            .apply()
        _hasCommonKey.value = false
        _commonKeyType.value = KeyType.PASSWORD
        _followingMap.value = ALL_OPTIONS.associate { it.id to false }
        KeyRepository(context).syncExistingSystemKeys(context)
    }

    fun setFollowing(context: Context, featureId: String, follow: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREFIX_FOLLOW + featureId, follow).apply()
        _followingMap.value = _followingMap.value.toMutableMap().apply { put(featureId, follow) }

        if (follow && hasKey(context)) {
            val type = getKeyType(context)
            val qr = prefs.getString(KEY_QR_TOKEN, null)
            val hash = prefs.getString(KEY_SECRET_HASH, null)
            if (qr != null && type == KeyType.QR_CODE) {
                syncKeyToFeature(context, featureId, type, qr)
            } else if (!hash.isNullOrBlank()) {
                syncHashToFeature(context, featureId, type, hash, qr)
            }
        } else if (!follow) {
            clearFeatureKey(context, featureId)
        }
        KeyRepository(context).syncExistingSystemKeys(context)
    }

    private fun clearFeatureKey(context: Context, featureId: String) {
        when (featureId) {
            "app_manager_security_key" -> AppLimitManager.clearSecurityKey(context)
            "grayscale_security_key" -> GrayscaleManager.clearSecurityKey(context)
            "zen_security_key" -> ZenManager.clearSecurityKey(context)
            "wind_down_security_key" -> {
                context.getSharedPreferences("brik_wind_down_prefs", Context.MODE_PRIVATE).edit()
                    .remove("pref_wd_key_type")
                    .remove("pref_wd_secret_hash")
                    .remove("pref_wd_qr_token")
                    .apply()
                WindDownManager.init(context)
            }
            "sideload_adb_lock" -> SideloadLockManager.clearSecurityKey(context)
            "dns_lock" -> DnsManager.clearSecurityKey(context)
        }
    }

    private fun syncKeyToFeature(context: Context, featureId: String, keyType: KeyType, secret: String) {
        when (featureId) {
            "app_manager_security_key" -> AppLimitManager.setSecurityKey(context, keyType, secret)
            "grayscale_security_key" -> GrayscaleManager.setSecurityKey(context, keyType, secret)
            "zen_security_key" -> ZenManager.setSecurityKey(context, keyType, secret)
            "wind_down_security_key" -> WindDownManager.setSecurityKey(context, keyType, secret)
            "sideload_adb_lock" -> SideloadLockManager.setSecurityKey(context, keyType, secret)
            "dns_lock" -> DnsManager.setSecurityKey(context, keyType, secret)
        }
    }

    private fun syncHashToFeature(context: Context, featureId: String, keyType: KeyType, hash: String, qrToken: String?) {
        val repo = KeyRepository(context)
        when (featureId) {
            "app_manager_security_key" -> {
                context.getSharedPreferences("brik_app_manager_prefs", Context.MODE_PRIVATE).edit()
                    .putString("pref_app_manager_key_type", keyType.name)
                    .putString("pref_app_manager_secret_hash", hash)
                    .putString("pref_app_manager_qr_token", qrToken)
                    .apply()
                repo.registerSystemKeyIfMissing(AppLimitManager.SYSTEM_KEY_ID, AppLimitManager.SYSTEM_KEY_NAME, keyType, hash, qrToken)
                AppLimitManager.init(context)
            }
            "grayscale_security_key" -> {
                context.getSharedPreferences("brik_grayscale_preferences", Context.MODE_PRIVATE).edit()
                    .putString("pref_grayscale_key_type", keyType.name)
                    .putString("pref_grayscale_secret_hash", hash)
                    .putString("pref_grayscale_qr_token", qrToken)
                    .apply()
                repo.registerSystemKeyIfMissing("grayscale_security_key", "Grayscale Security Key", keyType, hash, qrToken)
                GrayscaleManager.init(context)
            }
            "zen_security_key" -> {
                context.getSharedPreferences("brik_zen_preferences", Context.MODE_PRIVATE).edit()
                    .putString("pref_zen_key_type", keyType.name)
                    .putString("pref_zen_secret_hash", hash)
                    .putString("pref_zen_qr_token", qrToken)
                    .apply()
                repo.registerSystemKeyIfMissing("zen_security_key", "Zen Mode Security Key", keyType, hash, qrToken)
            }
            "wind_down_security_key" -> {
                context.getSharedPreferences("brik_wind_down_prefs", Context.MODE_PRIVATE).edit()
                    .putString("pref_wd_key_type", keyType.name)
                    .putString("pref_wd_secret_hash", hash)
                    .putString("pref_wd_qr_token", qrToken)
                    .apply()
            }
            "sideload_adb_lock" -> {
                context.getSharedPreferences("brik_sideload_lock_prefs", Context.MODE_PRIVATE).edit()
                    .putString("pref_sideload_lock_key_type", keyType.name)
                    .putString("pref_sideload_lock_secret_hash", hash)
                    .putString("pref_sideload_lock_qr_token", qrToken)
                    .apply()
                repo.registerSystemKeyIfMissing("sideload_adb_lock", "Sideload & ADB Security Key", keyType, hash, qrToken)
            }
            "dns_lock" -> {
                context.getSharedPreferences("brik_dns_lock_prefs", Context.MODE_PRIVATE).edit()
                    .putBoolean("dns_lock_enabled", true)
                    .putString("dns_key_type", keyType.name)
                    .putString("dns_key_hash", hash)
                    .apply()
                repo.registerSystemKeyIfMissing("dns_lock", "DNS Protection Key", keyType, hash, qrToken)
            }
        }
    }
}
