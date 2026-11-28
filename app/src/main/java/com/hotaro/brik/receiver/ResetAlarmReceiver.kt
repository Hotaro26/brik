package com.hotaro.brik.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hotaro.brik.util.ResetManager

class ResetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        val appCtx = context.applicationContext
        when (action) {
            ResetManager.ACTION_START_RESET -> {
                val pendingResult = goAsync()
                try {
                    ResetManager.init(appCtx)
                    if (!ResetManager.resetState.value.isScheduled) {
                        ResetManager.startCountdown(appCtx)
                        ResetManager.postStartedNotification(appCtx)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            ResetManager.ACTION_EXECUTE_RESET,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                val pendingResult = goAsync()
                try {
                    ResetManager.init(appCtx)
                    ResetManager.checkAndExecuteIfDue(appCtx)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
