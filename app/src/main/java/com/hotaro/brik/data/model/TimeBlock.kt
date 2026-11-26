package com.hotaro.brik.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FocusMode(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconName: String = "mode_fill0",
    val targetBagIds: List<String> = emptyList(),
    val targetPackageNames: List<String> = emptyList(),
    val enableDnsBlocking: Boolean = false,
    val dnsHost: String = "family.cloudflare-dns.com",
    val keyType: KeyType = KeyType.PASSWORD,
    val keySecret: String? = null,
    val savedKeyId: String? = null,
    val defaultDurationMinutes: Int = 45,
    val isRunning: Boolean = false,
    val sessionStartedAt: Long? = null,
    val sessionEndsAt: Long? = null,
    val enableKioskMode: Boolean = false,
    val kioskAllowedPackages: List<String> = emptyList(),
    val kioskDisableSystemGestures: Boolean = true,
    val kioskEnableFloatingControls: Boolean = true,
    val kioskFloatingAutoHideSeconds: Int = 3
)

@Serializable
data class AutomatedRoutine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isEnabled: Boolean = true,
    val daysOfWeek: List<Int> = listOf(2, 3, 4, 5, 6), // 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 17,
    val endMinute: Int = 0,
    val targetBagIds: List<String> = emptyList(),
    val targetPackageNames: List<String> = emptyList(),
    val enableDnsBlocking: Boolean = false,
    val dnsHost: String = "family.cloudflare-dns.com",
    val keyType: KeyType = KeyType.PASSWORD,
    val keySecret: String? = null,
    val savedKeyId: String? = null,
    val isActiveNow: Boolean = false
)

enum class TimeBlockType {
    FOCUS_MODE,
    ROUTINE
}

data class ActiveTimeBlockState(
    val type: TimeBlockType,
    val id: String,
    val title: String,
    val endsAt: Long?,
    val packagesSuspended: List<String>,
    val dnsBlocked: Boolean,
    val keyType: KeyType,
    val keySecret: String?,
    val enableKioskMode: Boolean = false,
    val kioskAllowedPackages: List<String> = emptyList(),
    val kioskDisableSystemGestures: Boolean = true,
    val kioskEnableFloatingControls: Boolean = true,
    val kioskFloatingAutoHideSeconds: Int = 3
)
