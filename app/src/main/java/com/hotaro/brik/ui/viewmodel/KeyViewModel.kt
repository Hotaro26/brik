package com.hotaro.brik.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotaro.brik.data.model.AppBag
import com.hotaro.brik.data.model.BagLock
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.LockStats
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
            val suspendResult = withContext(Dispatchers.IO) {
                PolicyBridgeManager.suspendPackages(context, bag.packageNames.toList(), true)
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
                    PolicyBridgeManager.suspendPackages(context, lock.packageNames.toList(), false)
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
            withContext(Dispatchers.IO) {
                PolicyBridgeManager.suspendPackages(context, lock.packageNames.toList(), false)
            }
            onResult(true, null)
        }
    }
}
