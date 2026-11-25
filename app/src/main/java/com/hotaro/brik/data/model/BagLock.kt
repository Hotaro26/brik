package com.hotaro.brik.data.model

import kotlinx.serialization.Serializable

enum class KeyType {
    PASSWORD,
    QR_CODE
}

@Serializable
data class BagLock(
    val id: String,
    val bagId: String,
    val bagName: String,
    val keyType: KeyType,
    val secretHash: String,
    val qrToken: String? = null,
    val lockedAt: Long = System.currentTimeMillis(),
    val packageNames: Set<String>
)

@Serializable
data class LockStats(
    val totalLocks: Int = 0,
    val totalUnlocks: Int = 0,
    val lastUnlockedAt: Long? = null
)
