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

class KeyRepository(private val context: Context) {
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
        syncExistingSystemKeys(context)
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

    fun registerSystemKeyIfMissing(
        id: String,
        name: String,
        keyType: KeyType,
        secretHash: String,
        qrToken: String? = null
    ) {
        if (_activeLocks.value.none { it.id == id }) {
            val lock = BagLock(
                id = id,
                bagId = id,
                bagName = name,
                keyType = keyType,
                secretHash = secretHash,
                qrToken = qrToken,
                lockedAt = System.currentTimeMillis(),
                packageNames = emptySet(),
                isSystemKey = true
            )
            persistLocks(_activeLocks.value + lock)
        }
    }

    fun syncExistingSystemKeys(context: Context) {
        // 1. Device Owner uninstall key
        val dpmPrefs = context.getSharedPreferences("brik_admin_pref", Context.MODE_PRIVATE)
        val dpmHash = dpmPrefs.getString("pref_dpm_uninstall_secret_hash", null)
        val dpmTypeStr = dpmPrefs.getString("pref_dpm_uninstall_key_type", null)
        val dpmQr = dpmPrefs.getString("pref_dpm_uninstall_qr_token", null)
        if (!dpmHash.isNullOrBlank()) {
            val keyType = if (dpmTypeStr == KeyType.QR_CODE.name) KeyType.QR_CODE else KeyType.PASSWORD
            registerSystemKeyIfMissing("dpm_uninstall_lock", "Device Owner Uninstall Key", keyType, dpmHash, dpmQr)
        }

        // 2. Sideload & ADB
        val sideloadPrefs = context.getSharedPreferences("brik_sideload_lock_pref", Context.MODE_PRIVATE)
        val sideloadHash = sideloadPrefs.getString("pref_sideload_lock_secret_hash", null)
        val sideloadTypeStr = sideloadPrefs.getString("pref_sideload_lock_key_type", null)
        val sideloadQr = sideloadPrefs.getString("pref_sideload_lock_qr_token", null)
        if (!sideloadHash.isNullOrBlank()) {
            val keyType = if (sideloadTypeStr == KeyType.QR_CODE.name) KeyType.QR_CODE else KeyType.PASSWORD
            registerSystemKeyIfMissing("sideload_adb_lock", "Sideload & ADB Security Key", keyType, sideloadHash, sideloadQr)
        }

        // 3. DNS
        val dnsPrefs = context.getSharedPreferences("brik_dns_lock_pref", Context.MODE_PRIVATE)
        val dnsEnabled = dnsPrefs.getBoolean("dns_lock_enabled", false)
        val dnsHash = dnsPrefs.getString("dns_key_hash", null)
        val dnsTypeStr = dnsPrefs.getString("dns_key_type", null)
        if (dnsEnabled && !dnsHash.isNullOrBlank()) {
            val keyType = if (dnsTypeStr == KeyType.QR_CODE.name) KeyType.QR_CODE else KeyType.PASSWORD
            registerSystemKeyIfMissing("dns_lock", "DNS Protection Key", keyType, dnsHash, null)
        }

        // 4. Grayscale
        val grayPrefs = context.getSharedPreferences("brik_grayscale_pref", Context.MODE_PRIVATE)
        val grayHash = grayPrefs.getString("pref_grayscale_secret_hash", null)
        val grayTypeStr = grayPrefs.getString("pref_grayscale_key_type", null)
        val grayQr = grayPrefs.getString("pref_grayscale_qr_token", null)
        if (!grayHash.isNullOrBlank()) {
            val keyType = if (grayTypeStr == KeyType.QR_CODE.name) KeyType.QR_CODE else KeyType.PASSWORD
            registerSystemKeyIfMissing("grayscale_security_key", "Grayscale Security Key", keyType, grayHash, grayQr)
        }

        // 5. Zen
        val zenPrefs = context.getSharedPreferences("brik_zen_pref", Context.MODE_PRIVATE)
        val zenHash = zenPrefs.getString("pref_zen_secret_hash", null)
        val zenTypeStr = zenPrefs.getString("pref_zen_key_type", null)
        val zenQr = zenPrefs.getString("pref_zen_qr_token", null)
        if (!zenHash.isNullOrBlank()) {
            val keyType = if (zenTypeStr == KeyType.QR_CODE.name) KeyType.QR_CODE else KeyType.PASSWORD
            registerSystemKeyIfMissing("zen_security_key", "Zen Mode Security Key", keyType, zenHash, zenQr)
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

    fun setSideloadLockKey(keyType: KeyType, secret: String): BagLock {
        val hash = QrUtils.sha256(secret)
        val lock = BagLock(
            id = "sideload_adb_lock",
            bagId = "sideload_adb",
            bagName = "Sideload & ADB Security Key",
            keyType = keyType,
            secretHash = hash,
            qrToken = if (keyType == KeyType.QR_CODE) secret else null,
            lockedAt = System.currentTimeMillis(),
            packageNames = emptySet(),
            isSystemKey = true
        )
        val updated = _activeLocks.value.filterNot { it.id == "sideload_adb_lock" } + lock
        persistLocks(updated)

        val currentStats = _stats.value
        val updatedStats = currentStats.copy(totalLocks = currentStats.totalLocks + 1)
        persistStats(updatedStats)

        return lock
    }

    fun setSystemSecurityKey(id: String, name: String, keyType: KeyType, secret: String): BagLock {
        val hash = QrUtils.sha256(secret)
        val lock = BagLock(
            id = id,
            bagId = id,
            bagName = name,
            keyType = keyType,
            secretHash = hash,
            qrToken = if (keyType == KeyType.QR_CODE) secret else null,
            lockedAt = System.currentTimeMillis(),
            packageNames = emptySet(),
            isSystemKey = true
        )
        val updated = _activeLocks.value.filterNot { it.id == id } + lock
        persistLocks(updated)

        val currentStats = _stats.value
        val updatedStats = currentStats.copy(totalLocks = currentStats.totalLocks + 1)
        persistStats(updatedStats)

        return lock
    }

    fun getSystemSecurityKey(id: String): BagLock? {
        return _activeLocks.value.find { it.id == id }
    }

    fun removeSystemSecurityKey(id: String) {
        val updated = _activeLocks.value.filterNot { it.id == id }
        persistLocks(updated)
    }

    fun verifySystemSecurityKey(id: String, inputSecret: String): Boolean {
        val lock = getSystemSecurityKey(id) ?: return true
        val inputHash = QrUtils.sha256(inputSecret)
        return inputHash.equals(lock.secretHash, ignoreCase = true)
    }

    fun getSideloadLock(): BagLock? {
        return _activeLocks.value.find { it.id == "sideload_adb_lock" }
    }

    fun removeSideloadLock() {
        val updated = _activeLocks.value.filterNot { it.id == "sideload_adb_lock" }
        persistLocks(updated)
    }

    fun getDpmUninstallLock(): BagLock? {
        return _activeLocks.value.find { it.id == "dpm_uninstall_lock" }
    }

    fun removeDpmUninstallLock() {
        val updated = _activeLocks.value.filterNot { it.id == "dpm_uninstall_lock" }
        persistLocks(updated)
    }

    fun verifyDpmUninstallKey(inputSecret: String): Boolean {
        val lock = getDpmUninstallLock() ?: return true
        val inputHash = QrUtils.sha256(inputSecret)
        return inputHash.equals(lock.secretHash, ignoreCase = true)
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
