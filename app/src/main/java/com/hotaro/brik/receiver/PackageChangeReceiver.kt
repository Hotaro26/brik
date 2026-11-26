package com.hotaro.brik.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hotaro.brik.data.repository.KeyRepository
import com.hotaro.brik.data.repository.TimeBlockRepository
import com.hotaro.brik.policy.PolicyBridgeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receiver that monitors package additions, reinstalls, and device boot.
 * Ensures that if a package belonging to an active locked Bag is reinstalled or the device reboots,
 * the package suspension and anti-uninstall protection are immediately re-applied.
 */
class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val appContext = context.applicationContext

        when (action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                val packageName = intent.data?.schemeSpecificPart ?: return

                val keyRepo = KeyRepository(appContext)
                val isLocked = keyRepo.activeLocks.value.any { lock ->
                    lock.packageNames.contains(packageName)
                }

                if (isLocked) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            PolicyBridgeManager.suspendPackages(appContext, listOf(packageName), true)
                            PolicyBridgeManager.setUninstallBlocked(appContext, listOf(packageName), true)
                            TimeBlockRepository(appContext).syncActiveBlocks(appContext)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val keyRepo = KeyRepository(appContext)
                        val allLocked = keyRepo.activeLocks.value.flatMap { it.packageNames }.distinct()
                        if (allLocked.isNotEmpty()) {
                            PolicyBridgeManager.suspendPackages(appContext, allLocked, true)
                            PolicyBridgeManager.setUninstallBlocked(appContext, allLocked, true)
                        }
                        TimeBlockRepository(appContext).syncActiveBlocks(appContext)
                        com.hotaro.brik.util.SideloadLockManager.init(appContext)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
