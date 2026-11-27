package com.hotaro.brik.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.repository.AppLimitRepository
import com.hotaro.brik.data.repository.KeyRepository
import com.hotaro.brik.policy.PolicyBridgeManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

object AppLimitManager {
    private const val PREFS_NAME = "brik_app_manager_prefs"
    private const val KEY_GUARD_ENABLED = "pref_app_manager_key_guard_enabled"
    private const val KEY_TYPE = "pref_app_manager_key_type"
    private const val KEY_SECRET_HASH = "pref_app_manager_secret_hash"
    private const val KEY_QR_TOKEN = "pref_app_manager_qr_token"
    const val SYSTEM_KEY_ID = "app_manager_security_key"
    const val SYSTEM_KEY_NAME = "App Manager Security Key"

    private val _isKeyGuardEnabled = MutableStateFlow(false)
    val isKeyGuardEnabled: StateFlow<Boolean> = _isKeyGuardEnabled.asStateFlow()

    private val _hasSecurityKey = MutableStateFlow(false)
    val hasSecurityKey: StateFlow<Boolean> = _hasSecurityKey.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isKeyGuardEnabled.value = prefs.getBoolean(KEY_GUARD_ENABLED, false)
        val hash = prefs.getString(KEY_SECRET_HASH, null)
        val repoLock = KeyRepository(context).getSystemSecurityKey(SYSTEM_KEY_ID)
        if (hash.isNullOrBlank() && repoLock != null) {
            prefs.edit()
                .putString(KEY_TYPE, repoLock.keyType.name)
                .putString(KEY_SECRET_HASH, repoLock.secretHash)
                .putString(KEY_QR_TOKEN, repoLock.qrToken)
                .apply()
        }
        _hasSecurityKey.value = !hash.isNullOrBlank() || repoLock != null || CommonKeyManager.isFollowingAndReady(context, SYSTEM_KEY_ID)
    }

    fun setKeyGuardEnabled(context: Context, enabled: Boolean) {
        _isKeyGuardEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_GUARD_ENABLED, enabled).apply()
    }

    fun setSecurityKey(context: Context, keyType: KeyType, secret: String) {
        val hash = QrUtils.sha256(secret)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TYPE, keyType.name)
            .putString(KEY_SECRET_HASH, hash)
            .putString(KEY_QR_TOKEN, if (keyType == KeyType.QR_CODE) secret else null)
            .apply()
        KeyRepository(context).setSystemSecurityKey(
            id = SYSTEM_KEY_ID,
            name = SYSTEM_KEY_NAME,
            keyType = keyType,
            secret = secret
        )
        _hasSecurityKey.value = true
    }

    fun clearSecurityKey(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_SECRET_HASH)
            .remove(KEY_QR_TOKEN)
            .remove(KEY_TYPE)
            .putBoolean(KEY_GUARD_ENABLED, false)
            .apply()
        KeyRepository(context).removeSystemSecurityKey(SYSTEM_KEY_ID)
        _isKeyGuardEnabled.value = false
        _hasSecurityKey.value = CommonKeyManager.isFollowingAndReady(context, SYSTEM_KEY_ID)
    }

    fun getKeyType(context: Context): KeyType {
        if (CommonKeyManager.isFollowingAndReady(context, SYSTEM_KEY_ID)) {
            return CommonKeyManager.getKeyType(context)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val typeStr = prefs.getString(KEY_TYPE, null)
        if (typeStr != null) {
            return runCatching { KeyType.valueOf(typeStr) }.getOrDefault(KeyType.PASSWORD)
        }
        return KeyRepository(context).getSystemSecurityKey(SYSTEM_KEY_ID)?.keyType ?: KeyType.PASSWORD
    }

    fun verifySecurityKey(context: Context, input: String): Boolean {
        if (CommonKeyManager.isFollowingAndReady(context, SYSTEM_KEY_ID)) {
            return CommonKeyManager.verifyCommonKey(context, input)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val expectedHash = prefs.getString(KEY_SECRET_HASH, null)
            ?: KeyRepository(context).getSystemSecurityKey(SYSTEM_KEY_ID)?.secretHash
            ?: return false
        return expectedHash.equals(QrUtils.sha256(input), ignoreCase = true)
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsageMinutesToday(context: Context, packageName: String): Int {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime) ?: return 0
        val totalMs = stats.filter { it.packageName == packageName }.sumOf { it.totalTimeInForeground }
        return (totalMs / 1000 / 60).toInt()
    }

    fun enforceAppRestriction(
        context: Context,
        packageName: String,
        blockMode: String,
        restrict: Boolean
    ): Result<Boolean> {
        return if (restrict) {
            if (blockMode == "hide") {
                PolicyBridgeManager.setApplicationHidden(context, packageName, true)
            } else {
                PolicyBridgeManager.suspendPackages(context, listOf(packageName), true)
            }
        } else {
            val unhideRes = PolicyBridgeManager.setApplicationHidden(context, packageName, false)
            val unsuspendRes = PolicyBridgeManager.suspendPackages(context, listOf(packageName), false)
            if (unsuspendRes.isSuccess) unsuspendRes else unhideRes
        }
    }

    fun checkAndEnforceLimits(context: Context) {
        val repo = AppLimitRepository(context)
        val limits = repo.limits.value
        if (limits.isEmpty()) return

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usm?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime).orEmpty()
        val usageMap = stats.groupBy { it.packageName }.mapValues { (_, list) ->
            (list.sumOf { it.totalTimeInForeground } / 1000 / 60).toInt()
        }

        for (limit in limits.values) {
            val usedMinutes = usageMap[limit.packageName] ?: 0
            if (limit.isRestricted || limit.dailyLimitMinutes == 0 || usedMinutes >= limit.dailyLimitMinutes) {
                enforceAppRestriction(context, limit.packageName, limit.blockMode, true)
            }
        }
    }
}
