package com.hotaro.brik.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.hotaro.brik.MainActivity
import com.hotaro.brik.util.StealthUtils

class DialerSecretCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "android.provider.Telephony.SECRET_CODE" ||
            action == "android.telephony.action.SECRET_CODE"
        ) {
            val uri = intent.data
            val dialedCode = uri?.host ?: intent.getStringExtra("code")
            val savedCode = StealthUtils.getDialerCode(context)

            // Match dialed code with user's saved code or default "2745"
            if (dialedCode == null || dialedCode == savedCode || dialedCode == StealthUtils.DEFAULT_CODE) {
                // Launch MainActivity
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(launchIntent)
                Toast.makeText(context, "Brik opened via secret code!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
