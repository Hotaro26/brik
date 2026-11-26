package com.hotaro.brik.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.hotaro.brik.data.repository.AppLimitRepository
import com.hotaro.brik.policy.PolicyBridgeManager
import java.util.Calendar

object AppLimitManager {

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
        val pkgStat = stats.find { it.packageName == packageName } ?: return 0
        return (pkgStat.totalTimeInForeground / 1000 / 60).toInt()
    }

    fun checkAndEnforceLimits(context: Context) {
        val repo = AppLimitRepository(context)
        val limits = repo.limits.value
        if (limits.isEmpty()) return

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime) ?: return
        for (usage in stats) {
            val limit = limits[usage.packageName] ?: continue
            val usedMinutes = (usage.totalTimeInForeground / 1000 / 60).toInt()
            if (usedMinutes >= limit.dailyLimitMinutes) {
                if (limit.blockMode == "hide") {
                    PolicyBridgeManager.setApplicationHidden(context, usage.packageName, true)
                } else {
                    PolicyBridgeManager.suspendPackages(context, listOf(usage.packageName), true)
                }
            }
        }
    }
}
