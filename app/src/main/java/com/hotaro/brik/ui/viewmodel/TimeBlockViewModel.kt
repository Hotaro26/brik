package com.hotaro.brik.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotaro.brik.data.model.ActiveTimeBlockState
import com.hotaro.brik.data.model.AppBag
import com.hotaro.brik.data.model.AppInfo
import com.hotaro.brik.data.model.AutomatedRoutine
import com.hotaro.brik.data.model.FocusMode
import com.hotaro.brik.data.model.SavedKey
import com.hotaro.brik.data.repository.BagRepository
import com.hotaro.brik.data.repository.KeyRepository
import com.hotaro.brik.data.repository.TimeBlockRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimeBlockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimeBlockRepository(application)
    private val bagRepository = BagRepository(application)
    private val keyRepository = KeyRepository(application)

    val focusModes: StateFlow<List<FocusMode>> = repository.focusModes
    val routines: StateFlow<List<AutomatedRoutine>> = repository.routines
    val activeSession: StateFlow<ActiveTimeBlockState?> = repository.activeSession
    val bags: StateFlow<List<AppBag>> = bagRepository.bags
    val savedKeys: StateFlow<List<SavedKey>> = keyRepository.savedKeys

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                val seen = mutableSetOf<String>()
                val list = mutableListOf<AppInfo>()

                for (info in resolveInfos) {
                    val pkg = info.activityInfo.packageName
                    if (pkg != getApplication<Application>().packageName && seen.add(pkg)) {
                        val label = info.loadLabel(pm)?.toString() ?: pkg
                        val isSystem = (info.activityInfo.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                        val icon = try { info.loadIcon(pm) } catch (e: Exception) { null }
                        list.add(AppInfo(packageName = pkg, label = label, icon = icon, isSystemApp = isSystem))
                    }
                }
                list.sortedBy { it.label.lowercase() }
            }
            _installedApps.value = apps
        }
    }

    fun startFocusMode(id: String, durationMinutes: Int, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repository.startFocusMode(id, durationMinutes, onResult)
        }
    }

    fun stopFocusMode(id: String, secretProvided: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repository.stopFocusMode(id, secretProvided, onResult)
        }
    }

    fun saveFocusMode(mode: FocusMode): FocusMode {
        return repository.saveFocusMode(mode)
    }

    fun deleteFocusMode(id: String, secretProvided: String?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repository.deleteFocusMode(id, secretProvided, onResult)
        }
    }

    fun saveRoutine(routine: AutomatedRoutine): AutomatedRoutine {
        return repository.saveRoutine(routine)
    }

    fun deleteRoutine(id: String, secretProvided: String?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repository.deleteRoutine(id, secretProvided, onResult)
        }
    }

    fun toggleRoutine(id: String, secretProvided: String?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repository.toggleRoutine(id, secretProvided, onResult)
        }
    }

    fun syncActiveBlocks() {
        viewModelScope.launch {
            repository.syncActiveBlocks(getApplication())
        }
    }
}
