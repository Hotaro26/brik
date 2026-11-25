package com.hotaro.brik.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import com.hotaro.brik.util.GrayscaleManager
import com.topjohnwu.superuser.Shell

class BrikAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            // Ignore system UI overlays to prevent flickering during quick settings / notification shade interactions
            if (packageName == "com.android.systemui" || packageName == "android") {
                return
            }
            GrayscaleManager.onForegroundAppChanged(applicationContext, packageName)
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        GrayscaleManager.evaluateAndApplyGrayscale(applicationContext)
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
