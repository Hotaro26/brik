package com.hotaro.brik.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hotaro.brik.R
import com.hotaro.brik.admin.BrikAdminReceiver
import com.hotaro.brik.data.repository.AppLimitRepository
import com.hotaro.brik.data.repository.BagRepository
import com.hotaro.brik.data.repository.KeyRepository
import com.hotaro.brik.data.repository.TimeBlockRepository
import com.hotaro.brik.policy.DnsManager
import com.hotaro.brik.policy.DnsMode
import com.hotaro.brik.policy.PolicyBridgeManager
import com.hotaro.brik.receiver.ResetAlarmReceiver
import com.hotaro.brik.service.TimeBlockService
import com.hotaro.brik.ui.theme.ThemePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

data class ResetState(
    val isScheduled: Boolean = false,
    val startedAt: Long = 0L,
    val targetAt: Long = 0L,
    val isResetting: Boolean = false,
    val isCompleted: Boolean = false
)

object ResetManager {
    const val RESET_DURATION_MS = 5L * 24L * 60L * 60L * 1000L // 5 days
    const val ACTION_EXECUTE_RESET = "com.hotaro.brik.ACTION_EXECUTE_RESET"

    private const val PREFS_NAME = "brik_reset_prefs"
    private const val KEY_SCHEDULED = "pref_reset_scheduled"
    private const val KEY_STARTED_AT = "pref_reset_started_at"
    private const val KEY_TARGET_AT = "pref_reset_target_at"
    private const val ALARM_REQUEST_CODE = 5505
    private const val NOTIF_CHANNEL_ID = "brik_reset_channel"
    private const val NOTIF_ID = 5506

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val executing = AtomicBoolean(false)

    private val _resetState = MutableStateFlow(ResetState())
    val resetState: StateFlow<ResetState> = _resetState.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scheduled = prefs.getBoolean(KEY_SCHEDULED, false)
        val startedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        val targetAt = prefs.getLong(KEY_TARGET_AT, 0L)

        _resetState.value = ResetState(
            isScheduled = scheduled,
            startedAt = startedAt,
            targetAt = targetAt
        )

        if (scheduled) {
            if (System.currentTimeMillis() >= targetAt) {
                executeFullReset(context)
            } else {
                scheduleAlarm(context, targetAt)
            }
        }
    }

    fun startCountdown(context: Context) {
        val now = System.currentTimeMillis()
        val targetAt = now + RESET_DURATION_MS
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SCHEDULED, true)
            .putLong(KEY_STARTED_AT, now)
            .putLong(KEY_TARGET_AT, targetAt)
            .commit()

        _resetState.value = ResetState(
            isScheduled = true,
            startedAt = now,
            targetAt = targetAt
        )
        scheduleAlarm(context, targetAt)
    }

    fun cancelCountdown(context: Context) {
        cancelAlarm(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_SCHEDULED, false)
            .remove(KEY_STARTED_AT)
            .remove(KEY_TARGET_AT)
            .commit()

        _resetState.value = ResetState()
    }

    fun checkAndExecuteIfDue(context: Context) {
        val state = _resetState.value
        if (state.isScheduled && state.targetAt > 0L && System.currentTimeMillis() >= state.targetAt) {
            executeFullReset(context)
        }
    }

    fun scheduleAlarm(context: Context, targetAt: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ResetAlarmReceiver::class.java).apply {
            action = ACTION_EXECUTE_RESET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetAt, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ResetAlarmReceiver::class.java).apply {
            action = ACTION_EXECUTE_RESET
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun executeFullReset(context: Context) {
        if (!executing.compareAndSet(false, true)) return
        val appCtx = context.applicationContext
        _resetState.value = _resetState.value.copy(isResetting = true)

        scope.launch {
            try {
                cancelAlarm(appCtx)

                // 1. Stop active Foreground services & TimeBlock enforcement
                try {
                    TimeBlockService.stop(appCtx)
                } catch (_: Throwable) {}

                // 2. Collect all installed + tracked packages to unhide, unsuspend, and unblock uninstall
                val dpm = appCtx.getSystemService(DevicePolicyManager::class.java)
                val dar = BrikAdminReceiver.getComponentName(appCtx)
                val isDpm = dpm != null && dpm.isDeviceOwnerApp(appCtx.packageName)

                val allPackages = mutableSetOf<String>()
                try {
                    KeyRepository(appCtx).activeLocks.value.forEach { allPackages.addAll(it.packageNames) }
                    BagRepository(appCtx).bags.value.forEach { allPackages.addAll(it.packageNames) }
                    TimeBlockRepository.getInstance(appCtx).activeSession.value?.packagesSuspended?.let {
                        allPackages.addAll(it)
                    }
                    AppLimitRepository(appCtx).limits.value.keys.forEach { allPackages.add(it) }
                } catch (_: Throwable) {}

                try {
                    val installed = appCtx.packageManager.getInstalledApplications(
                        PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES
                    )
                    installed.forEach { app ->
                        val pkg = app.packageName
                        if (pkg != appCtx.packageName) {
                            if (!isDpm || dpm!!.isPackageSuspended(dar, pkg) || dpm.isApplicationHidden(dar, pkg)) {
                                allPackages.add(pkg)
                            }
                        }
                    }
                } catch (_: Throwable) {}

                val pkgList = allPackages.filter { it != appCtx.packageName }
                if (pkgList.isNotEmpty()) {
                    PolicyBridgeManager.setPackagesHidden(appCtx, pkgList, false)
                    PolicyBridgeManager.suspendPackages(appCtx, pkgList, false)
                    PolicyBridgeManager.setUninstallBlocked(appCtx, pkgList, false)
                }

                // 3. Clear Kiosk lock task packages & restrictions
                if (isDpm) {
                    try {
                        dpm!!.setLockTaskPackages(dar, emptyArray())
                    } catch (_: Throwable) {}
                }

                // 4. Lift Private DNS, Grayscale, Sideload/ADB, Zen, WindDown, Emergency
                try { DnsManager.setDns(appCtx, DnsMode.OPPORTUNISTIC, null) } catch (_: Throwable) {}
                try {
                    GrayscaleManager.setSystemwideEnabled(appCtx, false)
                    GrayscaleManager.setAppSpecificEnabled(appCtx, false)
                    GrayscaleManager.applyGrayscale(appCtx, false)
                } catch (_: Throwable) {}
                try { SideloadLockManager.clearSecurityKey(appCtx) } catch (_: Throwable) {}
                try {
                    if (StealthUtils.isAppHidden(appCtx)) {
                        StealthUtils.setAppHidden(appCtx, false)
                    }
                } catch (_: Throwable) {}
                try {
                    AppIconManager.setAppIcon(appCtx, AppIconManager.icons.first())
                } catch (_: Throwable) {}

                // 5. Clear ALL SharedPreferences so Brik is like newly installed
                try {
                    val sharedPrefsDir = File(appCtx.applicationInfo.dataDir, "shared_prefs")
                    sharedPrefsDir.listFiles()?.forEach { file ->
                        val prefName = file.name.removeSuffix(".xml")
                        appCtx.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().commit()
                    }
                } catch (_: Throwable) {}

                // 6. Revoke Device Owner (DPM) permissions last
                try {
                    PolicyBridgeManager.clearDeviceOwner(appCtx)
                } catch (_: Throwable) {}

                // 7. Re-initialize core singletons to reflect fresh-install defaults
                try {
                    ThemePreferences.init(appCtx)
                    AssistivePreferences.init(appCtx)
                    KioskPreferences.init(appCtx)
                    EmergencyManager.init(appCtx)
                    SideloadLockManager.init(appCtx)
                    ZenManager.init(appCtx)
                    WindDownManager.init(appCtx)
                    GrayscaleManager.init(appCtx)
                    CommonKeyManager.init(appCtx)
                    AppLimitManager.init(appCtx)
                    PolicyBridgeManager.updateStatus(appCtx)
                } catch (_: Throwable) {}

                _resetState.value = ResetState(
                    isScheduled = false,
                    isResetting = false,
                    isCompleted = true
                )

                postCompletionNotification(appCtx)
            } finally {
                executing.set(false)
            }
        }
    }

    private fun postCompletionNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Device Reset",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when the 5-day reset countdown completes"
            }
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
            .setSmallIcon(R.drawable.shield_fill0)
            .setContentTitle("Brik Reset Complete")
            .setContentText("All distractions removed, app data cleared, and Device Owner revoked.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID, notification)
    }
}
