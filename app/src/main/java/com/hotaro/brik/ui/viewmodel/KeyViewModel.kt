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
import com.hotaro.brik.policy.PolicyBridgeManager
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
            val suspendResult = withContext(Dispatchers.IO) {
                val res = PolicyBridgeManager.suspendPackages(context, pkgList, true)
                if (res.isSuccess) {
                    PolicyBridgeManager.setUninstallBlocked(context, pkgList, true)
                }
                res
            }

            if (suspendResult.isSuccess) {
                repository.lockBag(
                    bagId = bag.id,
                    bagName = bag.name,
                    packages = bag.packageNames,
                    keyType = keyType,
                    secret = secret
                )
                onResult(true, null)
            } else {
                onResult(false, suspendResult.exceptionOrNull()?.message ?: "Failed to suspend apps")
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
                    // Only release packages that are not locked in another active bag
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
            if (lock.packageNames.isNotEmpty()) {
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
            onResult(true, null)
        }
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

    fun getDpmUninstallLock(): BagLock? {
        return repository.getDpmUninstallLock()
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
}
