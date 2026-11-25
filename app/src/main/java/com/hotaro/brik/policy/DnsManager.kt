package com.hotaro.brik.policy

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.hotaro.brik.admin.BrikAdminReceiver
import com.topjohnwu.superuser.Shell

enum class DnsMode(val title: String, val modeKey: String) {
    OFF("Off", "off"),
    OPPORTUNISTIC("Automatic", "opportunistic"),
    SPECIFIED_HOST("Custom DNS (DoT)", "hostname")
}

data class DnsPreset(
    val name: String,
    val host: String,
    val description: String
)

data class DnsStatus(
    val mode: DnsMode,
    val host: String?
)

object DnsManager {

    val PRESETS = listOf(
        DnsPreset("AdGuard DNS", "dns.adguard-dns.com", "Blocks ads, trackers, and malicious domains"),
        DnsPreset("AdGuard Family", "family.adguard-dns.com", "Blocks adult content, safe search, and ads"),
        DnsPreset("Cloudflare 1.1.1.1", "one.one.one.one", "Fast, private, unfiltered DNS resolver"),
        DnsPreset("Cloudflare Family", "family.cloudflare-dns.com", "Blocks malware and adult websites"),
        DnsPreset("Google Public DNS", "dns.google", "High-performance, reliable Google resolver"),
        DnsPreset("Quad9", "dns.quad9.net", "Blocks malicious hostnames & phishing threats"),
        DnsPreset("Mullvad AdBlock", "adblock.doh.mullvad.net", "High-privacy ad & tracker blocking")
    )

    fun getCurrentDns(context: Context): DnsStatus {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val isDeviceOwner = dpm?.isDeviceOwnerApp(context.packageName) == true
        val dar = BrikAdminReceiver.getComponentName(context)

        if (isDeviceOwner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val dpmMode = dpm.getGlobalPrivateDnsMode(dar)
                val dpmHost = dpm.getGlobalPrivateDnsHost(dar)
                val mode = when (dpmMode) {
                    DevicePolicyManager.PRIVATE_DNS_MODE_OFF -> DnsMode.OFF
                    DevicePolicyManager.PRIVATE_DNS_MODE_OPPORTUNISTIC -> DnsMode.OPPORTUNISTIC
                    DevicePolicyManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME -> DnsMode.SPECIFIED_HOST
                    else -> DnsMode.OPPORTUNISTIC
                }
                return DnsStatus(mode, dpmHost)
            } catch (e: Throwable) {
                // fallback to Settings.Global
            }
        }

        // Fallback to reading system settings directly
        return try {
            val modeStr = Settings.Global.getString(context.contentResolver, "private_dns_mode") ?: "opportunistic"
            val hostStr = Settings.Global.getString(context.contentResolver, "private_dns_specifier")
            val mode = when (modeStr) {
                "off" -> DnsMode.OFF
                "hostname" -> DnsMode.SPECIFIED_HOST
                else -> DnsMode.OPPORTUNISTIC
            }
            DnsStatus(mode, hostStr)
        } catch (e: Throwable) {
            DnsStatus(DnsMode.OPPORTUNISTIC, null)
        }
    }

    fun setDns(context: Context, mode: DnsMode, hostname: String?): Pair<Boolean, String> {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val isDeviceOwner = dpm?.isDeviceOwnerApp(context.packageName) == true
        val dar = BrikAdminReceiver.getComponentName(context)

        // Try Device Owner (DPM) first
        if (isDeviceOwner) {
            try {
                return when (mode) {
                    DnsMode.OFF -> {
                        dpm.setGlobalSetting(dar, "private_dns_mode", "off")
                        true to "Private DNS turned off"
                    }
                    DnsMode.OPPORTUNISTIC -> {
                        val result = dpm.setGlobalPrivateDnsModeOpportunistic(dar)
                        if (result == DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR) {
                            true to "Automatic DNS enabled"
                        } else {
                            false to "Failed to set Automatic DNS"
                        }
                    }
                    DnsMode.SPECIFIED_HOST -> {
                        val host = hostname?.trim() ?: ""
                        if (host.isEmpty()) return false to "Hostname cannot be empty"
                        val result = dpm.setGlobalPrivateDnsModeSpecifiedHost(dar, host)
                        when (result) {
                            DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR -> true to "Custom DNS set to $host"
                            DevicePolicyManager.PRIVATE_DNS_SET_ERROR_HOST_NOT_SERVING -> false to "DNS host does not support DNS-over-TLS (DoT)"
                            DevicePolicyManager.PRIVATE_DNS_SET_ERROR_FAILURE_SETTING -> false to "Failed to set Private DNS"
                            else -> true to "DNS mode updated"
                        }
                    }
                }
            } catch (e: Throwable) {
                // Continue to shell fallback
            }
        }

        // Fallback via Root shell
        val cmd = when (mode) {
            DnsMode.OFF -> "settings put global private_dns_mode off"
            DnsMode.OPPORTUNISTIC -> "settings put global private_dns_mode opportunistic"
            DnsMode.SPECIFIED_HOST -> {
                val host = hostname?.trim() ?: ""
                if (host.isEmpty()) return false to "Hostname cannot be empty"
                "settings put global private_dns_mode hostname && settings put global private_dns_specifier '$host'"
            }
        }

        val success = try {
            if (Shell.isAppGrantedRoot() == true) {
                Shell.cmd(cmd).exec().isSuccess
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }

        return if (success) {
            true to "Private DNS updated successfully"
        } else {
            false to "Cannot configure DNS: requires Device Owner or Root"
        }
    }
}
