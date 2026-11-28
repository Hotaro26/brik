package com.hotaro.brik.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hotaro.brik.util.ResetManager

class ResetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == ResetManager.ACTION_EXECUTE_RESET ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            val pendingResult = goAsync()
            try {
                ResetManager.init(context.applicationContext)
                ResetManager.checkAndExecuteIfDue(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
