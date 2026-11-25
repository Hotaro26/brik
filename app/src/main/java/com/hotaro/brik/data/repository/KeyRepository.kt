package com.hotaro.brik.data.repository

import android.content.Context
import com.hotaro.brik.data.model.BagLock
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.LockStats
import com.hotaro.brik.data.model.SavedKey
import com.hotaro.brik.util.QrUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class KeyRepository(context: Context) {
    private val prefs = context.getSharedPreferences("brik_keys_pref", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _activeLocks = MutableStateFlow<List<BagLock>>(emptyList())
    val activeLocks: StateFlow<List<BagLock>> = _activeLocks.asStateFlow()

    private val _savedKeys = MutableStateFlow<List<SavedKey>>(emptyList())
    val savedKeys: StateFlow<List<SavedKey>> = _savedKeys.asStateFlow()

    private val _stats = MutableStateFlow(LockStats())
    val stats: StateFlow<LockStats> = _stats.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val serializedLocks = prefs.getString("active_locks", null)
        if (!serializedLocks.isNullOrBlank()) {
            try {
                _activeLocks.value = json.decodeFromString(serializedLocks)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val serializedSavedKeys = prefs.getString("saved_keys", null)
        if (!serializedSavedKeys.isNullOrBlank()) {
            try {
                _savedKeys.value = json.decodeFromString(serializedSavedKeys)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val serializedStats = prefs.getString("lock_stats", null)
        if (!serializedStats.isNullOrBlank()) {
            try {
                _stats.value = json.decodeFromString(serializedStats)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun lockBag(
        bagId: String,
        bagName: String,
        packages: Set<String>,
        keyType: KeyType,
        secret: String,
        actionType: com.hotaro.brik.data.model.BagAction = com.hotaro.brik.data.model.BagAction.SUSPEND
    ): BagLock {
        val hash = QrUtils.sha256(secret)
        val lock = BagLock(
            id = UUID.randomUUID().toString(),
            bagId = bagId,
            bagName = bagName,
            keyType = keyType,
            secretHash = hash,
            qrToken = if (keyType == KeyType.QR_CODE) secret else null,
            lockedAt = System.currentTimeMillis(),
            packageNames = packages,
            actionType = actionType
        )

        // Remove any existing lock for this bagId
        val updated = _activeLocks.value.filterNot { it.bagId == bagId } + lock
        persistLocks(updated)

        // Increment stats
        val currentStats = _stats.value
        val updatedStats = currentStats.copy(totalLocks = currentStats.totalLocks + 1)
        persistStats(updatedStats)

        return lock
    }

    fun verifyAndUnlock(lockId: String, inputSecret: String): Boolean {
        val lock = _activeLocks.value.find { it.id == lockId } ?: return false
        val inputHash = QrUtils.sha256(inputSecret)
        if (inputHash == lock.secretHash) {
            val updated = _activeLocks.value.filterNot { it.id == lockId }
            persistLocks(updated)

            val currentStats = _stats.value
            val updatedStats = currentStats.copy(
                totalUnlocks = currentStats.totalUnlocks + 1,
                lastUnlockedAt = System.currentTimeMillis()
            )
            persistStats(updatedStats)
            return true
        }
        return false
    }

    fun removeLockDirectly(lockId: String) {
        val updated = _activeLocks.value.filterNot { it.id == lockId }
        persistLocks(updated)

        val currentStats = _stats.value
        val updatedStats = currentStats.copy(
            totalUnlocks = currentStats.totalUnlocks + 1,
            lastUnlockedAt = System.currentTimeMillis()
        )
        persistStats(updatedStats)
    }

    fun isBagLocked(bagId: String): Boolean {
        return _activeLocks.value.any { it.bagId == bagId }
    }

    fun getLockForBag(bagId: String): BagLock? {
        return _activeLocks.value.find { it.bagId == bagId }
    }

    fun setDpmUninstallLock(keyType: KeyType, secret: String): BagLock {
        val hash = QrUtils.sha256(secret)
        val lock = BagLock(
            id = "dpm_uninstall_lock",
            bagId = "dpm_uninstall",
            bagName = "Device Owner Uninstall Key",
            keyType = keyType,
            secretHash = hash,
            qrToken = if (keyType == KeyType.QR_CODE) secret else null,
            lockedAt = System.currentTimeMillis(),
            packageNames = emptySet(),
            isSystemKey = true
        )
        val updated = _activeLocks.value.filterNot { it.id == "dpm_uninstall_lock" } + lock
        persistLocks(updated)

        val currentStats = _stats.value
        val updatedStats = currentStats.copy(totalLocks = currentStats.totalLocks + 1)
        persistStats(updatedStats)

        return lock
    }

    fun getDpmUninstallLock(): BagLock? {
        return _activeLocks.value.find { it.id == "dpm_uninstall_lock" }
    }

    fun verifyDpmUninstallKey(inputSecret: String): Boolean {
        val lock = getDpmUninstallLock() ?: return true
        val inputHash = QrUtils.sha256(inputSecret)
        if (inputHash == lock.secretHash) {
            val updated = _activeLocks.value.filterNot { it.id == "dpm_uninstall_lock" }
            persistLocks(updated)

            val currentStats = _stats.value
            val updatedStats = currentStats.copy(
                totalUnlocks = currentStats.totalUnlocks + 1,
                lastUnlockedAt = System.currentTimeMillis()
            )
            persistStats(updatedStats)
            return true
        }
        return false
    }

    fun saveKey(name: String, description: String, token: String): SavedKey {
        val key = SavedKey(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            description = description.trim(),
            token = token.trim(),
            createdAt = System.currentTimeMillis()
        )
        val updated = _savedKeys.value + key
        persistSavedKeys(updated)
        return key
    }

    fun updateSavedKey(id: String, name: String, description: String) {
        val updated = _savedKeys.value.map {
            if (it.id == id) it.copy(name = name.trim(), description = description.trim()) else it
        }
        persistSavedKeys(updated)
    }

    fun deleteSavedKey(id: String) {
        val updated = _savedKeys.value.filterNot { it.id == id }
        persistSavedKeys(updated)
    }

    private fun persistSavedKeys(keys: List<SavedKey>) {
        _savedKeys.value = keys
        prefs.edit().putString("saved_keys", json.encodeToString(keys)).apply()
    }

    private fun persistLocks(list: List<BagLock>) {
        _activeLocks.value = list
        prefs.edit().putString("active_locks", json.encodeToString(list)).apply()
    }

    private fun persistStats(stats: LockStats) {
        _stats.value = stats
        prefs.edit().putString("lock_stats", json.encodeToString(stats)).apply()
    }
}
