package com.hotaro.brik.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotaro.brik.data.model.AppBag
import com.hotaro.brik.data.model.BagLock
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.LockStats
import com.hotaro.brik.data.model.SavedKey
import com.hotaro.brik.data.repository.KeyRepository
import com.hotaro.brik.data.repository.TimeBlockRepository
import com.hotaro.brik.policy.DnsManager
import com.hotaro.brik.policy.PolicyBridgeManager
import com.hotaro.brik.util.GrayscaleManager
import com.hotaro.brik.util.SideloadLockManager
import com.hotaro.brik.util.ZenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KeyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KeyRepository(application)
    val activeLocks: StateFlow<List<BagLock>> = repository.activeLocks
    val stats: StateFlow<LockStats> = repository.stats
    val savedKeys: StateFlow<List<SavedKey>> = repository.savedKeys

    fun saveKey(name: String, description: String, token: String): SavedKey {
        return repository.saveKey(name, description, token)
    }

    fun deleteSavedKey(id: String) {
        repository.deleteSavedKey(id)
    }

    fun updateSavedKey(id: String, name: String, description: String) {
        repository.updateSavedKey(id, name, description)
    }

    fun isBagLocked(bagId: String): Boolean {
        return repository.isBagLocked(bagId)
    }

    fun lockBag(
        context: Context,
        bag: AppBag,
        keyType: KeyType,
        secret: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val pkgList = bag.packageNames.toList()
            val actionResult = withContext(Dispatchers.IO) {
                val res = if (bag.actionType == com.hotaro.brik.data.model.BagAction.HIDE) {
                    PolicyBridgeManager.setPackagesHidden(context, pkgList, true)
                } else {
                    PolicyBridgeManager.suspendPackages(context, pkgList, true)
                }
                if (res.isSuccess) {
                    PolicyBridgeManager.setUninstallBlocked(context, pkgList, true)
                }
                res
            }

            if (actionResult.isSuccess) {
                repository.lockBag(
                    bagId = bag.id,
                    bagName = bag.name,
                    packages = bag.packageNames,
                    keyType = keyType,
                    secret = secret,
                    actionType = bag.actionType
                )
                onResult(true, null)
            } else {
                onResult(false, actionResult.exceptionOrNull()?.message ?: "Failed to enforce bag policy")
            }
        }
    }

    fun lockBagWithCommonKey(
        context: Context,
        bag: AppBag,
        onResult: (Boolean, String?) -> Unit
    ) {
        val secretHash = com.hotaro.brik.util.CommonKeyManager.getSecretHash(context)
        if (secretHash.isNullOrBlank()) {
            onResult(false, "Common Key is not configured")
            return
        }
        val keyType = com.hotaro.brik.util.CommonKeyManager.getKeyType(context)
        val qrToken = com.hotaro.brik.util.CommonKeyManager.getQrToken(context)
        viewModelScope.launch {
            val pkgList = bag.packageNames.toList()
            val actionResult = withContext(Dispatchers.IO) {
                val res = if (bag.actionType == com.hotaro.brik.data.model.BagAction.HIDE) {
                    PolicyBridgeManager.setPackagesHidden(context, pkgList, true)
                } else {
                    PolicyBridgeManager.suspendPackages(context, pkgList, true)
                }
                if (res.isSuccess) {
                    PolicyBridgeManager.setUninstallBlocked(context, pkgList, true)
                }
                res
            }
            if (actionResult.isSuccess) {
                repository.lockBagWithHash(
                    bagId = bag.id,
                    bagName = bag.name,
                    packages = bag.packageNames,
                    keyType = keyType,
                    secretHash = secretHash,
                    qrToken = qrToken,
                    actionType = bag.actionType
                )
                onResult(true, null)
            } else {
                onResult(false, actionResult.exceptionOrNull()?.message ?: "Failed to enforce bag policy")
            }
        }
    }

    fun unlockBag(
        context: Context,
        lock: BagLock,
        inputSecret: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val valid = repository.verifyAndUnlock(lock.id, inputSecret)
            if (valid) {
                withContext(Dispatchers.IO) {
                    // Only release packages that are not locked in another active bag, Focus Mode, Routine, or App Manager restriction
                    val remainingLockedPackages = repository.activeLocks.value
                        .filter { it.id != lock.id }
                        .flatMap { it.packageNames }
                        .toSet()
                    val activeTimeBlockPackages = TimeBlockRepository.getInstance(context)
                        .activeSession.value?.packagesSuspended.orEmpty().toSet()
                    val appLimitRestricted = com.hotaro.brik.data.repository.AppLimitRepository(context)
                        .limits.value.values
                        .filter { it.isRestricted || it.dailyLimitMinutes == 0 }
                        .map { it.packageName }
                        .toSet()
                    val protectedPackages = remainingLockedPackages + activeTimeBlockPackages + appLimitRestricted
                    val packagesToRelease = lock.packageNames.filterNot { protectedPackages.contains(it) }

                    if (packagesToRelease.isNotEmpty()) {
                        if (lock.actionType == com.hotaro.brik.data.model.BagAction.HIDE) {
                            PolicyBridgeManager.setPackagesHidden(context, packagesToRelease, false)
                        } else {
                            PolicyBridgeManager.suspendPackages(context, packagesToRelease, false)
                        }
                        PolicyBridgeManager.setUninstallBlocked(context, packagesToRelease, false)
                    }
                }
                onResult(true, null)
            } else {
                onResult(false, "Incorrect password or invalid QR key")
            }
        }
    }

    fun emergencyUnlock(
        context: Context,
        lock: BagLock,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            repository.removeLockDirectly(lock.id)
            when {
                lock.id == "dpm_uninstall_lock" -> {
                    val prefs = context.getSharedPreferences("brik_admin_pref", Context.MODE_PRIVATE)
                    prefs.edit()
                        .remove("pref_dpm_uninstall_secret_hash")
                        .remove("pref_dpm_uninstall_key_type")
                        .remove("pref_dpm_uninstall_qr_token")
                        .apply()
                }
                lock.id == "sideload_adb_lock" -> {
                    SideloadLockManager.clearSecurityKey(context)
                }
                lock.id == "dns_lock" -> {
                    DnsManager.clearSecurityKey(context)
                }
                lock.id == "grayscale_security_key" -> {
                    GrayscaleManager.clearSecurityKey(context)
                }
                lock.id == "zen_security_key" -> {
                    ZenManager.clearSecurityKey(context)
                }
                lock.id == "app_manager_security_key" -> {
                    com.hotaro.brik.util.AppLimitManager.clearSecurityKey(context)
                }
                lock.id.startsWith("focus_mode_") -> {
                    val modeId = lock.id.removePrefix("focus_mode_")
                    TimeBlockRepository(context).clearFocusModeKey(modeId)
                }
                lock.id.startsWith("routine_") -> {
                    val routineId = lock.id.removePrefix("routine_")
                    TimeBlockRepository(context).clearRoutineKey(routineId)
                }
                lock.packageNames.isNotEmpty() -> {
                    withContext(Dispatchers.IO) {
                        val remainingLockedPackages = repository.activeLocks.value
                            .filter { it.id != lock.id }
                            .flatMap { it.packageNames }
                            .toSet()
                        val packagesToRelease = lock.packageNames.filterNot { remainingLockedPackages.contains(it) }

                        if (packagesToRelease.isNotEmpty()) {
                            PolicyBridgeManager.suspendPackages(context, packagesToRelease, false)
                            PolicyBridgeManager.setUninstallBlocked(context, packagesToRelease, false)
                        }
                    }
                }
            }
            onResult(true, null)
        }
    }

    fun setSystemSecurityKey(id: String, name: String, keyType: KeyType, secret: String): BagLock {
        return repository.setSystemSecurityKey(id, name, keyType, secret)
    }

    fun verifySystemSecurityKey(id: String, inputSecret: String): Boolean {
        return repository.verifySystemSecurityKey(id, inputSecret)
    }

    fun syncAllActiveLocks(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val allLocked = repository.activeLocks.value.flatMap { it.packageNames }.distinct()
            if (allLocked.isNotEmpty()) {
                PolicyBridgeManager.suspendPackages(context, allLocked, true)
                PolicyBridgeManager.setUninstallBlocked(context, allLocked, true)
            }
        }
    }

    fun setDpmUninstallLock(keyType: KeyType, secret: String): BagLock {
        return repository.setDpmUninstallLock(keyType, secret)
    }

    fun setSideloadLockKey(keyType: KeyType, secret: String): BagLock {
        return repository.setSideloadLockKey(keyType, secret)
    }

    fun getSideloadLock(): BagLock? {
        return repository.getSideloadLock()
    }

    fun getDpmUninstallLock(): BagLock? {
        return repository.getDpmUninstallLock()
    }

    fun verifyDpmUninstallKey(inputSecret: String): Boolean {
        return repository.verifyDpmUninstallKey(inputSecret)
    }

    fun verifyAndDeactivateDeviceOwner(
        context: Context,
        inputSecret: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val valid = repository.verifyDpmUninstallKey(inputSecret)
            if (valid) {
                val clearRes = withContext(Dispatchers.IO) {
                    PolicyBridgeManager.clearDeviceOwner(context)
                }
                if (clearRes.isSuccess) {
                    onResult(true, null)
                } else {
                    onResult(false, clearRes.exceptionOrNull()?.message ?: "Failed to clear Device Owner")
                }
            } else {
                onResult(false, "Incorrect password or invalid QR key")
            }
        }
    }

    fun removeDpmUninstallLock() {
        repository.removeDpmUninstallLock()
    }

    fun syncExistingSystemKeys() {
        repository.syncExistingSystemKeys(getApplication())
    }
}
