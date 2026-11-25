package com.hotaro.brik.admin

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BrikAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Brik Device Admin enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "Brik Device Admin disabled", Toast.LENGTH_SHORT).show()
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Toast.makeText(context, "Brik Provisioning Complete", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, BrikAdminReceiver::class.java)
        }
    }
}
