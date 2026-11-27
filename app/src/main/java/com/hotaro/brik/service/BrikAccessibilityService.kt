package com.hotaro.brik.service

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import com.hotaro.brik.MainActivity
import com.hotaro.brik.util.GrayscaleManager
import com.hotaro.brik.util.KioskPreferences
import com.topjohnwu.superuser.Shell

class BrikAccessibilityService : AccessibilityService() {

    private var isCallInProgress = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            handleNotificationEvent(event)
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            // Ignore system UI overlays to prevent flickering during quick settings / notification shade interactions
            if (packageName == "com.android.systemui" || packageName == "android") {
                return
            }
            GrayscaleManager.onForegroundAppChanged(applicationContext, packageName)
            com.hotaro.brik.util.AppLimitManager.checkAndEnforceLimits(applicationContext)
        }
    }

    private fun handleNotificationEvent(event: AccessibilityEvent) {
        if (!KioskPreferences.allowIncomingCallsWindow.value) return
        val parcelable = event.parcelableData
        if (parcelable is Notification) {
            val isCallCategory = parcelable.category == Notification.CATEGORY_CALL
            val pkg = event.packageName?.toString() ?: ""
            val isDialerPackage = pkg.contains("dialer", ignoreCase = true) ||
                    pkg.contains("telecom", ignoreCase = true) ||
                    pkg.contains("phone", ignoreCase = true) ||
                    pkg.contains("incall", ignoreCase = true)

            if (isCallCategory || isDialerPackage) {
                isCallInProgress = true
                try {
                    if (parcelable.fullScreenIntent != null) {
                        parcelable.fullScreenIntent?.send()
                    } else {
                        parcelable.contentIntent?.send()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    val telecom = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                    telecom?.showInCallScreen(false)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        GrayscaleManager.evaluateAndApplyGrayscale(applicationContext)
        registerCallStateListener()
    }

    private fun registerCallStateListener() {
        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager.registerTelephonyCallback(
                    mainExecutor,
                    object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            handleCallState(state)
                        }
                    }
                )
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.listen(
                    object : PhoneStateListener() {
                        @Deprecated("Deprecated in Java")
                        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                            handleCallState(state)
                        }
                    },
                    PhoneStateListener.LISTEN_CALL_STATE
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isCallInProgress = true
                if (KioskPreferences.allowIncomingCallsWindow.value) {
                    try {
                        val telecom = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                        telecom?.showInCallScreen(false)
                    } catch (_: Exception) {}

                    try {
                        val telecom = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                        val dialerPkg = telecom?.defaultDialerPackage ?: "com.android.dialer"
                        val launchIntent = packageManager.getLaunchIntentForPackage(dialerPkg)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            startActivity(launchIntent)
                        }
                    } catch (_: Exception) {}
                }
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                isCallInProgress = true
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (isCallInProgress) {
                    isCallInProgress = false
                    if (KioskPreferences.autoResumeAfterCall.value) {
                        try {
                            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    companion object {
        fun isEnabled(context: Context): Boolean {
            val expectedComponent = ComponentName(context, BrikAccessibilityService::class.java).flattenToString()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val component = colonSplitter.next()
                if (component.equals(expectedComponent, ignoreCase = true) ||
                    component.equals("${context.packageName}/${BrikAccessibilityService::class.java.name}", ignoreCase = true)
                ) {
                    val accessibilityEnabled = runCatching {
                        Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
                    }.getOrDefault(false)
                    return accessibilityEnabled
                }
            }
            return false
        }

        fun tryEnableSilently(context: Context): Boolean {
            val expectedComponent = ComponentName(context, BrikAccessibilityService::class.java).flattenToString()
            val directSuccess = runCatching {
                val current = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""
                val set = current.split(":").filter { it.isNotBlank() }.toMutableSet()
                set.add(expectedComponent)
                val newSetting = set.joinToString(":")
                Settings.Secure.putString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    newSetting
                )
                Settings.Secure.putInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    1
                )
                true
            }.getOrDefault(false)

            if (directSuccess && isEnabled(context)) return true

            if (Shell.isAppGrantedRoot() == true) {
                val res = Shell.cmd(
                    "settings put secure enabled_accessibility_services $expectedComponent",
                    "settings put secure accessibility_enabled 1"
                ).exec()
                if (res.isSuccess) return true
            }

            return false
        }
    }
}
