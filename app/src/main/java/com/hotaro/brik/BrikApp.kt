package com.hotaro.brik

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.hotaro.brik.ui.theme.ThemePreferences
import com.hotaro.brik.util.EmergencyManager
import com.hotaro.brik.util.WindDownManager
import com.hotaro.brik.util.ZenManager

class BrikApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePreferences.init(this)
        EmergencyManager.init(this)
        ZenManager.init(this)
        WindDownManager.init(this)

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
