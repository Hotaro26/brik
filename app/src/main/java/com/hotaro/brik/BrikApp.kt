package com.hotaro.brik

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.hotaro.brik.ui.theme.ThemePreferences
import com.hotaro.brik.util.AppLimitManager
import com.hotaro.brik.util.AssistivePreferences
import com.hotaro.brik.util.CommonKeyManager
import com.hotaro.brik.util.EmergencyManager
import com.hotaro.brik.util.GrayscaleManager
import com.hotaro.brik.util.KioskPreferences
import com.hotaro.brik.util.SideloadLockManager
import com.hotaro.brik.util.WindDownManager
import com.hotaro.brik.util.ZenManager
import kotlinx.coroutines.launch

class BrikApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePreferences.init(this)
        com.hotaro.brik.policy.PolicyBridgeManager.updateStatus(this)

        try {
            val dpm = getSystemService(android.app.admin.DevicePolicyManager::class.java)
            val dar = com.hotaro.brik.admin.BrikAdminReceiver.getComponentName(this)
            if (dpm != null && dpm.isDeviceOwnerApp(packageName)) {
                dpm.setPermissionGrantState(dar, packageName, android.Manifest.permission.READ_PHONE_STATE, android.app.admin.DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED)
            }
        } catch (_: Exception) {}

        AssistivePreferences.init(this)
        KioskPreferences.init(this)
        EmergencyManager.init(this)
        SideloadLockManager.init(this)
        ZenManager.init(this)
        WindDownManager.init(this)
        GrayscaleManager.init(this)
        CommonKeyManager.init(this)
        AppLimitManager.init(this)

        val reconcilePrefs = getSharedPreferences("brik_migration_prefs", Context.MODE_PRIVATE)
        if (!reconcilePrefs.getBoolean("reconcile_orphaned_packages_v215", false)) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    val keyRepo = com.hotaro.brik.data.repository.KeyRepository(this@BrikApp)
                    val timeBlockRepo = com.hotaro.brik.data.repository.TimeBlockRepository.getInstance(this@BrikApp)
                    val limitRepo = com.hotaro.brik.data.repository.AppLimitRepository(this@BrikApp)

                    val protectedPkgs = buildSet {
                        keyRepo.activeLocks.value.forEach { addAll(it.packageNames) }
                        timeBlockRepo.activeSession.value?.packagesSuspended?.let { addAll(it) }
                        limitRepo.limits.value.values
                            .filter { it.isRestricted || it.dailyLimitMinutes == 0 }
                            .forEach { add(it.packageName) }
                    }

                    val dpm = getSystemService(android.app.admin.DevicePolicyManager::class.java)
                    val dar = com.hotaro.brik.admin.BrikAdminReceiver.getComponentName(this@BrikApp)
                    val isDpm = dpm != null && dpm.isDeviceOwnerApp(packageName)

                    val allInstalled = packageManager.getInstalledApplications(
                        android.content.pm.PackageManager.GET_META_DATA or android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES
                    )
                    val orphaned = allInstalled
                        .map { it.packageName }
                        .filter { pkg ->
                            pkg != packageName && !protectedPkgs.contains(pkg) && (
                                !isDpm || dpm!!.isPackageSuspended(dar, pkg) || dpm.isApplicationHidden(dar, pkg)
                            )
                        }
                    if (orphaned.isNotEmpty()) {
                        com.hotaro.brik.policy.PolicyBridgeManager.setPackagesHidden(this@BrikApp, orphaned, false)
                        com.hotaro.brik.policy.PolicyBridgeManager.suspendPackages(this@BrikApp, orphaned, false)
                        com.hotaro.brik.policy.PolicyBridgeManager.setUninstallBlocked(this@BrikApp, orphaned, false)
                    }
                    reconcilePrefs.edit().putBoolean("reconcile_orphaned_packages_v215", true).apply()
                } catch (_: Throwable) {}
            }
        }

        val screenLockFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                    EmergencyManager.onScreenLocked(this@BrikApp)
                }
            }
        }, screenLockFilter)
    }
}
