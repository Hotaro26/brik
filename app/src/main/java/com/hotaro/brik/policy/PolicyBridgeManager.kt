package com.hotaro.brik.policy

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.app.admin.IDevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.hotaro.brik.admin.BrikAdminReceiver
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuBinderWrapper
import com.topjohnwu.superuser.Shell
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

enum class BridgeType {
    DEVICE_OWNER,
    DHIZUKU,
    SHIZUKU,
    ROOT,
    NONE
}

data class BridgeStatus(
    val isDeviceOwner: Boolean = false,
    val isDhizukuInstalled: Boolean = false,
    val isDhizukuGranted: Boolean = false,
    val isShizukuRunning: Boolean = false,
    val isShizukuGranted: Boolean = false,
    val isRootGranted: Boolean = false,
    val activeBridge: BridgeType = BridgeType.NONE
)

object PolicyBridgeManager {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("")
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    fun getBridgeStatus(context: Context): BridgeStatus {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        val isDeviceOwner = dpm?.isDeviceOwnerApp(context.packageName) == true

        var dhizukuInstalled = false
        var dhizukuGranted = false
        try {
            dhizukuInstalled = Dhizuku.init(context)
            dhizukuGranted = dhizukuInstalled && Dhizuku.isPermissionGranted()
        } catch (e: Throwable) {
            // Ignored
        }

        var shizukuRunning = false
        var shizukuGranted = false
        try {
            shizukuRunning = Shizuku.pingBinder()
            shizukuGranted = shizukuRunning && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            // Ignored
        }

        var rootGranted = false
        try {
            rootGranted = Shell.isAppGrantedRoot() == true
        } catch (e: Throwable) {
            // Ignored
        }

        val active = when {
            isDeviceOwner -> BridgeType.DEVICE_OWNER
            dhizukuGranted -> BridgeType.DHIZUKU
            shizukuGranted -> BridgeType.SHIZUKU
            rootGranted -> BridgeType.ROOT
            else -> BridgeType.NONE
        }

        return BridgeStatus(
            isDeviceOwner = isDeviceOwner,
            isDhizukuInstalled = dhizukuInstalled,
            isDhizukuGranted = dhizukuGranted,
            isShizukuRunning = shizukuRunning,
            isShizukuGranted = shizukuGranted,
            isRootGranted = rootGranted,
            activeBridge = active
        )
    }

    fun clearDeviceOwner(context: Context): Result<Boolean> {
        return runCatching {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)!!
            dpm.clearDeviceOwnerApp(context.packageName)
            true
        }
    }

    @SuppressLint("PrivateApi")
    fun getDhizukuDpm(context: Context): DevicePolicyManager {
        val ownerComponent = Dhizuku.getOwnerComponent()
        val pkgContext = context.createPackageContext(ownerComponent.packageName, Context.CONTEXT_IGNORE_SECURITY)
        val manager = pkgContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val field = manager.javaClass.getDeclaredField("mService")
        field.isAccessible = true
        val oldInterface = field[manager] as IDevicePolicyManager
        if (oldInterface is DhizukuBinderWrapper) return manager
        val oldBinder = oldInterface.asBinder()
        val newBinder = Dhizuku.binderWrapper(oldBinder)
        val newInterface = IDevicePolicyManager.Stub.asInterface(newBinder)
        field[manager] = newInterface
        return manager
    }

    /**
     * App Blocking: suspend or unsuspend packages.
     */
    fun suspendPackages(context: Context, packages: List<String>, suspend: Boolean): Result<Boolean> {
        val status = getBridgeStatus(context)
        return runCatching {
            when (status.activeBridge) {
                BridgeType.DEVICE_OWNER -> {
                    val dpm = context.getSystemService(DevicePolicyManager::class.java)!!
                    val dar = BrikAdminReceiver.getComponentName(context)
                    val failed = dpm.setPackagesSuspended(dar, packages.toTypedArray(), suspend)
                    failed.isEmpty()
                }
                BridgeType.DHIZUKU -> {
                    val dpm = getDhizukuDpm(context)
                    val dar = Dhizuku.getOwnerComponent()
                    val failed = dpm.setPackagesSuspended(dar, packages.toTypedArray(), suspend)
                    failed.isEmpty()
                }
                BridgeType.SHIZUKU -> {
                    // Shizuku bridge active: root fallback or inform user to activate Device Owner via Shizuku
                    for (pkg in packages) {
                        val cmd = if (suspend) "pm suspend $pkg" else "pm unsuspend $pkg"
                        executeShellCommand(cmd)
                    }
                    true
                }
                BridgeType.ROOT -> {
                    for (pkg in packages) {
                        val action = if (suspend) "suspend" else "unsuspend"
                        Shell.cmd("pm $action $pkg").exec()
                    }
                    true
                }
                BridgeType.NONE -> {
                    throw IllegalStateException("No privileged bridge active. Enable Device Owner, Dhizuku, Shizuku, or Root.")
                }
            }
        }
    }

    /**
     * App Anti-Uninstall: Block or allow uninstallation of packages.
     * Prevents bypassing suspension/blocking by uninstalling and reinstalling apps.
     */
    fun setUninstallBlocked(context: Context, packages: List<String>, blocked: Boolean): Result<Boolean> {
        val status = getBridgeStatus(context)
        return runCatching {
            when (status.activeBridge) {
                BridgeType.DEVICE_OWNER -> {
                    val dpm = context.getSystemService(DevicePolicyManager::class.java)!!
                    val dar = BrikAdminReceiver.getComponentName(context)
                    for (pkg in packages) {
                        try {
                            dpm.setUninstallBlocked(dar, pkg, blocked)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                    true
                }
                BridgeType.DHIZUKU -> {
                    val dpm = getDhizukuDpm(context)
                    val dar = Dhizuku.getOwnerComponent()
                    for (pkg in packages) {
                        try {
                            dpm.setUninstallBlocked(dar, pkg, blocked)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    /**
     * Check if package uninstallation is blocked.
     */
    fun isUninstallBlocked(context: Context, packageName: String): Boolean {
        val status = getBridgeStatus(context)
        return try {
            when (status.activeBridge) {
                BridgeType.DEVICE_OWNER -> {
                    val dpm = context.getSystemService(DevicePolicyManager::class.java)!!
                    val dar = BrikAdminReceiver.getComponentName(context)
                    dpm.isUninstallBlocked(dar, packageName)
                }
                BridgeType.DHIZUKU -> {
                    val dpm = getDhizukuDpm(context)
                    val dar = Dhizuku.getOwnerComponent()
                    dpm.isUninstallBlocked(dar, packageName)
                }
                else -> false
            }
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * App Hiding: hide or unhide package completely.
     */
    fun setApplicationHidden(context: Context, packageName: String, hidden: Boolean): Result<Boolean> {
        val status = getBridgeStatus(context)
        return runCatching {
            when (status.activeBridge) {
                BridgeType.DEVICE_OWNER -> {
                    val dpm = context.getSystemService(DevicePolicyManager::class.java)!!
                    val dar = BrikAdminReceiver.getComponentName(context)
                    dpm.setApplicationHidden(dar, packageName, hidden)
                }
                BridgeType.DHIZUKU -> {
                    val dpm = getDhizukuDpm(context)
                    val dar = Dhizuku.getOwnerComponent()
                    dpm.setApplicationHidden(dar, packageName, hidden)
                }
                BridgeType.SHIZUKU -> {
                    val action = if (hidden) "hide" else "unhide"
                    executeShellCommand("pm $action $packageName")
                }
                BridgeType.ROOT -> {
                    val action = if (hidden) "hide" else "unhide"
                    Shell.cmd("pm $action $packageName").exec().isSuccess
                }
                BridgeType.NONE -> {
                    throw IllegalStateException("No privileged bridge active. Enable Device Owner, Dhizuku, Shizuku, or Root.")
                }
            }
        }
    }

    /**
     * Check if package is hidden
     */
    fun isApplicationHidden(context: Context, packageName: String): Boolean {
        val status = getBridgeStatus(context)
        return try {
            when (status.activeBridge) {
                BridgeType.DEVICE_OWNER -> {
                    val dpm = context.getSystemService(DevicePolicyManager::class.java)!!
                    val dar = BrikAdminReceiver.getComponentName(context)
                    dpm.isApplicationHidden(dar, packageName)
                }
                BridgeType.DHIZUKU -> {
                    val dpm = getDhizukuDpm(context)
                    val dar = Dhizuku.getOwnerComponent()
                    dpm.isApplicationHidden(dar, packageName)
                }
                else -> false
            }
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Lock / Kiosk Mode: Set packages permitted to run in Lock Task Mode.
     */
    fun setLockTaskPackages(context: Context, packages: List<String>): Result<Boolean> {
        val status = getBridgeStatus(context)
        return runCatching {
            when (status.activeBridge) {
                BridgeType.DEVICE_OWNER -> {
                    val dpm = context.getSystemService(DevicePolicyManager::class.java)!!
                    val dar = BrikAdminReceiver.getComponentName(context)
                    dpm.setLockTaskPackages(dar, packages.toTypedArray())
                    true
                }
                BridgeType.DHIZUKU -> {
                    val dpm = getDhizukuDpm(context)
                    val dar = Dhizuku.getOwnerComponent()
                    dpm.setLockTaskPackages(dar, packages.toTypedArray())
                    true
                }
                else -> {
                    throw IllegalStateException("Lock Task / Kiosk configuration requires Device Owner or Dhizuku.")
                }
            }
        }
    }

    fun getLockTaskPackages(context: Context): List<String> {
        val status = getBridgeStatus(context)
        return try {
            when (status.activeBridge) {
                BridgeType.DEVICE_OWNER -> {
                    val dpm = context.getSystemService(DevicePolicyManager::class.java)!!
                    val dar = BrikAdminReceiver.getComponentName(context)
                    dpm.getLockTaskPackages(dar).toList()
                }
                BridgeType.DHIZUKU -> {
                    val dpm = getDhizukuDpm(context)
                    val dar = Dhizuku.getOwnerComponent()
                    dpm.getLockTaskPackages(dar).toList()
                }
                else -> emptyList()
            }
        } catch (e: Throwable) {
            emptyList()
        }
    }

    fun isLockTaskPermitted(context: Context, packageName: String): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        return dpm?.isLockTaskPermitted(packageName) == true
    }

    private fun executeShellCommand(command: String): Boolean {
        return try {
            if (Shell.isAppGrantedRoot() == true) {
                Shell.cmd(command).exec().isSuccess
            } else {
                false
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }
}
