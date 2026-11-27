package com.hotaro.brik

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.hotaro.brik.ui.theme.ThemePreferences
import com.hotaro.brik.util.AssistivePreferences
import com.hotaro.brik.util.EmergencyManager
import com.hotaro.brik.util.GrayscaleManager
import com.hotaro.brik.util.KioskPreferences
import com.hotaro.brik.util.SideloadLockManager
import com.hotaro.brik.util.WindDownManager
import com.hotaro.brik.util.ZenManager

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
