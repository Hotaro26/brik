package com.hotaro.brik.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotaro.brik.data.model.AppBag
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.data.repository.BagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    private val bagRepository = BagRepository(application)
    val bags: StateFlow<List<AppBag>> = bagRepository.bags

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    val searchQuery = MutableStateFlow("")

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(_installedApps, searchQuery) { apps, query ->
        if (query.isBlank()) {
            apps
        } else {
            val q = query.trim().lowercase()
            apps.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                fetchInstalledApps()
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    private fun fetchInstalledApps(): List<AppInfo> {
        val pm = getApplication<Application>().packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val seenPackages = mutableSetOf<String>()
        val appList = mutableListOf<AppInfo>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg != getApplication<Application>().packageName && seenPackages.add(pkg)) {
                val label = info.loadLabel(pm)?.toString() ?: pkg
                val icon = try {
                    info.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                val isSystem = (info.activityInfo.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                appList.add(
                    AppInfo(
                        packageName = pkg,
                        label = label,
                        icon = icon,
                        isSystemApp = isSystem
                    )
                )
            }
        }

        // Also query all applications to catch background or other installed packages
        try {
            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in allApps) {
                val pkg = app.packageName
                if (pkg != getApplication<Application>().packageName && seenPackages.add(pkg)) {
                    val label = pm.getApplicationLabel(app).toString()
                    val icon = try {
                        pm.getApplicationIcon(app)
                    } catch (e: Exception) {
                        null
                    }
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    appList.add(
                        AppInfo(
                            packageName = pkg,
                            label = label,
                            icon = icon,
                            isSystemApp = isSystem
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return appList.sortedWith(compareBy({ it.isSystemApp }, { it.label.lowercase() }))
    }

    fun toggleAppSelection(packageName: String) {
        val current = _selectedPackages.value
        _selectedPackages.value = if (current.contains(packageName)) {
            current - packageName
        } else {
            current + packageName
        }
    }

    fun selectAll(packages: Collection<String>) {
        _selectedPackages.value = _selectedPackages.value + packages
    }

    fun clearSelection() {
        _selectedPackages.value = emptySet()
    }

    fun createBag(name: String): Boolean {
        val trimmed = name.trim()
        val pkgs = _selectedPackages.value
        if (trimmed.isEmpty() || pkgs.isEmpty()) return false
        bagRepository.saveBag(trimmed, pkgs)
        _selectedPackages.value = emptySet()
        return true
    }

    fun deleteBag(bagId: String) {
        bagRepository.deleteBag(bagId)
    }

    fun getAppInfo(packageName: String): AppInfo? {
        return _installedApps.value.find { it.packageName == packageName }
    }
}
