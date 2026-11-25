package com.hotaro.brik.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hotaro.brik.data.repository.TimeBlockRepository

class TimeBlockAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TIME_BLOCK_TRIGGER = "com.hotaro.brik.ACTION_TIME_BLOCK_TRIGGER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val repository = TimeBlockRepository(context)
        repository.syncActiveBlocks(context)
    }
}
