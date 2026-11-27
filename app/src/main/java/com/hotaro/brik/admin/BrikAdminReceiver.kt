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
                val defaultDialer = (context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager)?.defaultDialerPackage
                val allowedPackages = listOfNotNull(
                    context.packageName,
                    "com.android.settings",
                    defaultDialer,
                    "com.google.android.dialer",
                    "com.android.dialer",
                    "com.android.phone",
                    "com.android.server.telecom",
                    "com.android.incallui",
                    "com.samsung.android.dialer",
                    "com.samsung.android.incallui",
                    "com.samsung.android.app.telephonyui",
                    "com.miui.telephony",
                    "com.miui.phone",
                    "com.qti.phone"
                ).distinct().toTypedArray()
                dpm?.setLockTaskPackages(dar, allowedPackages)
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
