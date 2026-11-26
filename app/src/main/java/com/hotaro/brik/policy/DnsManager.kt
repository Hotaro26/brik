package com.hotaro.brik.policy

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.hotaro.brik.admin.BrikAdminReceiver
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.util.QrUtils
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

        // 1. Try Device Owner (DPM)
        if (isDeviceOwner) {
            try {
                when (mode) {
                    DnsMode.OFF -> {
                        dpm.setGlobalSetting(dar, "private_dns_mode", "off")
                        return true to "Private DNS turned off"
                    }
                    DnsMode.OPPORTUNISTIC -> {
                        dpm.setGlobalSetting(dar, "private_dns_mode", "opportunistic")
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                dpm.setGlobalPrivateDnsModeOpportunistic(dar)
                            }
                        } catch (e: Throwable) { }
                        return true to "Automatic DNS enabled"
                    }
                    DnsMode.SPECIFIED_HOST -> {
                        val host = hostname?.trim() ?: ""
                        if (host.isEmpty()) return false to "Hostname cannot be empty"

                        dpm.setGlobalSetting(dar, "private_dns_specifier", host)
                        dpm.setGlobalSetting(dar, "private_dns_mode", "hostname")

                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                dpm.setGlobalPrivateDnsModeSpecifiedHost(dar, host)
                            }
                        } catch (e: Throwable) { }

                        return true to "Custom DNS set to $host"
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        // 2. Try Dhizuku
        try {
            val dhizukuInstalled = com.rosan.dhizuku.api.Dhizuku.init(context)
            if (dhizukuInstalled && com.rosan.dhizuku.api.Dhizuku.isPermissionGranted()) {
                val dhizukuDpm = PolicyBridgeManager.getDhizukuDpm(context)
                val owner = com.rosan.dhizuku.api.Dhizuku.getOwnerComponent()
                when (mode) {
                    DnsMode.OFF -> {
                        dhizukuDpm.setGlobalSetting(owner, "private_dns_mode", "off")
                        return true to "Private DNS turned off"
                    }
                    DnsMode.OPPORTUNISTIC -> {
                        dhizukuDpm.setGlobalSetting(owner, "private_dns_mode", "opportunistic")
                        return true to "Automatic DNS enabled"
                    }
                    DnsMode.SPECIFIED_HOST -> {
                        val host = hostname?.trim() ?: ""
                        if (host.isEmpty()) return false to "Hostname cannot be empty"
                        dhizukuDpm.setGlobalSetting(owner, "private_dns_specifier", host)
                        dhizukuDpm.setGlobalSetting(owner, "private_dns_mode", "hostname")
                        return true to "Custom DNS set to $host"
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // 3. Fallback via Shizuku ADB or Root shell
        val cmd = when (mode) {
            DnsMode.OFF -> "settings put global private_dns_mode off"
            DnsMode.OPPORTUNISTIC -> "settings put global private_dns_mode opportunistic"
            DnsMode.SPECIFIED_HOST -> {
                val host = hostname?.trim() ?: ""
                if (host.isEmpty()) return false to "Hostname cannot be empty"
                "settings put global private_dns_specifier '$host' && settings put global private_dns_mode hostname"
            }
        }

        val success = try {
            if (Shell.isAppGrantedRoot() == true) {
                Shell.cmd(cmd).exec().isSuccess
            } else if (rikka.shizuku.Shizuku.pingBinder() && rikka.shizuku.Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                try {
                    val method = rikka.shizuku.Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    method.isAccessible = true
                    val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
                    process.waitFor() == 0
                } catch (e: Throwable) {
                    false
                }
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }

        return if (success) {
            true to "Private DNS updated successfully"
        } else {
            false to "Cannot configure DNS: requires Device Owner, Dhizuku, Shizuku, or Root"
        }
    }

    private const val PREFS_DNS_LOCK = "brik_dns_lock_prefs"
    private const val KEY_DNS_LOCK_ENABLED = "dns_lock_enabled"
    private const val KEY_DNS_KEY_TYPE = "dns_key_type"
    private const val KEY_DNS_KEY_HASH = "dns_key_hash"

    fun hasKeyLock(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_DNS_LOCK, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DNS_LOCK_ENABLED, false) && !prefs.getString(KEY_DNS_KEY_HASH, null).isNullOrBlank()
    }

    fun getKeyType(context: Context): KeyType {
        val prefs = context.getSharedPreferences(PREFS_DNS_LOCK, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_DNS_KEY_TYPE, KeyType.PASSWORD.name) ?: KeyType.PASSWORD.name
        return try { KeyType.valueOf(name) } catch (e: Exception) { KeyType.PASSWORD }
    }

    fun setSecurityKey(context: Context, keyType: KeyType, secret: String) {
        val hash = QrUtils.sha256(secret)
        val prefs = context.getSharedPreferences(PREFS_DNS_LOCK, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_DNS_LOCK_ENABLED, true)
            .putString(KEY_DNS_KEY_TYPE, keyType.name)
            .putString(KEY_DNS_KEY_HASH, hash)
            .apply()
        com.hotaro.brik.data.repository.KeyRepository(context).setSystemSecurityKey(
            id = "dns_lock",
            name = "DNS Protection Key",
            keyType = keyType,
            secret = secret
        )
    }

    fun clearSecurityKey(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_DNS_LOCK, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_DNS_LOCK_ENABLED, false)
            .remove(KEY_DNS_KEY_HASH)
            .remove(KEY_DNS_KEY_TYPE)
            .apply()
        com.hotaro.brik.data.repository.KeyRepository(context).removeSystemSecurityKey("dns_lock")
    }

    fun verifySecurityKey(context: Context, input: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_DNS_LOCK, Context.MODE_PRIVATE)
        val storedHash = prefs.getString(KEY_DNS_KEY_HASH, null) ?: return true
        val inputHash = QrUtils.sha256(input)
        return storedHash.equals(inputHash, ignoreCase = true)
    }
}
