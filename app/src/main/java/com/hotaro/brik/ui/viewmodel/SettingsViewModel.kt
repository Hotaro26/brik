package com.hotaro.brik.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotaro.brik.data.model.AppBag
import com.hotaro.brik.policy.BridgeStatus
import com.hotaro.brik.policy.BridgeType
import com.hotaro.brik.policy.PolicyBridgeManager
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val bridgeStatus: StateFlow<BridgeStatus> = PolicyBridgeManager.bridgeStatus

    private val _suspendedPackages = MutableStateFlow<Set<String>>(emptySet())
    val suspendedPackages: StateFlow<Set<String>> = _suspendedPackages.asStateFlow()

    private val _hiddenPackages = MutableStateFlow<Set<String>>(emptySet())
    val hiddenPackages: StateFlow<Set<String>> = _hiddenPackages.asStateFlow()

    private val _kioskPackages = MutableStateFlow<List<String>>(emptyList())
    val kioskPackages: StateFlow<List<String>> = _kioskPackages.asStateFlow()

    private val _isKioskActive = MutableStateFlow(false)
    val isKioskActive: StateFlow<Boolean> = _isKioskActive.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PolicyBridgeManager.updateStatus(getApplication())
            }

            // Load kiosk allowed packages
            val allowed = withContext(Dispatchers.IO) {
                PolicyBridgeManager.getLockTaskPackages(getApplication())
            }
            _kioskPackages.value = allowed
        }
    }

    fun clearDeviceOwner(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                PolicyBridgeManager.clearDeviceOwner(getApplication())
            }
            refreshStatus()
            if (res.isSuccess) {
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun requestDhizukuPermission() {
        try {
            if (Dhizuku.init(getApplication())) {
                Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                    override fun onRequestPermission(grantResult: Int) {
                        refreshStatus()
                    }
                })
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun requestShizukuPermission() {
        try {
            if (Shizuku.pingBinder()) {
                Shizuku.requestPermission(1001)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun setDeviceOwnerViaShizuku(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val cmd = "dpm set-device-owner com.hotaro.brik/.admin.BrikAdminReceiver"
                    val method = Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
                    val exit = process.waitFor()
                    val output = process.inputStream.bufferedReader().readText()
                    val error = process.errorStream.bufferedReader().readText()
                    if (exit == 0) {
                        true to (output.ifBlank { "Device Owner granted successfully!" })
                    } else {
                        false to (error.ifBlank { output.ifBlank { "Exit code $exit" } })
                    }
                }
            }
            refreshStatus()
            if (res.isSuccess) {
                val (success, msg) = res.getOrThrow()
                onResult(success, msg)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun suspendPackages(packages: List<String>, suspend: Boolean, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                val sRes = PolicyBridgeManager.suspendPackages(getApplication(), packages, suspend)
                if (sRes.isSuccess) {
                    PolicyBridgeManager.setUninstallBlocked(getApplication(), packages, suspend)
                }
                sRes
            }
            if (res.isSuccess) {
                if (suspend) {
                    _suspendedPackages.value = _suspendedPackages.value + packages
                } else {
                    _suspendedPackages.value = _suspendedPackages.value - packages.toSet()
                }
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun toggleAppHidden(packageName: String, hide: Boolean, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                PolicyBridgeManager.setApplicationHidden(getApplication(), packageName, hide)
            }
            if (res.isSuccess) {
                if (hide) {
                    _hiddenPackages.value = _hiddenPackages.value + packageName
                } else {
                    _hiddenPackages.value = _hiddenPackages.value - packageName
                }
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun saveKioskPackages(packages: List<String>, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                PolicyBridgeManager.setLockTaskPackages(getApplication(), packages)
            }
            if (res.isSuccess) {
                _kioskPackages.value = packages
                onResult(true, null)
            } else {
                onResult(false, res.exceptionOrNull()?.message)
            }
        }
    }

    fun startKioskMode(activity: Activity): Boolean {
        return try {
            activity.startLockTask()
            _isKioskActive.value = true
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    fun stopKioskMode(activity: Activity): Boolean {
        return try {
            activity.stopLockTask()
            _isKioskActive.value = false
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }
}
