package com.hotaro.brik.admin

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BrikAdminReceiver : DeviceAdminReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.hotaro.brik.SYNC_LOCK_PACKAGES") {
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                val dar = getComponentName(context)
                dpm?.setLockTaskPackages(dar, arrayOf(context.packageName, "com.android.settings", "com.google.android.dialer", "com.android.dialer"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        com.hotaro.brik.policy.PolicyBridgeManager.updateStatus(context)
        Toast.makeText(context, "Brik Device Admin enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        com.hotaro.brik.policy.PolicyBridgeManager.updateStatus(context)
        Toast.makeText(context, "Brik Device Admin disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        com.hotaro.brik.policy.PolicyBridgeManager.updateStatus(context)
        Toast.makeText(context, "Brik Provisioning Complete", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, BrikAdminReceiver::class.java)
        }
    }
}
