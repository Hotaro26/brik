package com.hotaro.brik.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hotaro.brik.data.model.ActiveTimeBlockState
import com.hotaro.brik.data.model.AutomatedRoutine
import com.hotaro.brik.data.model.FocusMode
import com.hotaro.brik.data.model.KeyType
import com.hotaro.brik.data.model.TimeBlockType
import com.hotaro.brik.policy.DnsManager
import com.hotaro.brik.policy.DnsMode
import com.hotaro.brik.policy.PolicyBridgeManager
import com.hotaro.brik.receiver.TimeBlockAlarmReceiver
import com.hotaro.brik.service.TimeBlockService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.UUID

class TimeBlockRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("brik_time_block_pref", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val bagRepository = BagRepository(context)
    private val keyRepository = KeyRepository(context)

    private val _focusModes = MutableStateFlow<List<FocusMode>>(emptyList())
    val focusModes: StateFlow<List<FocusMode>> = _focusModes.asStateFlow()

    private val _routines = MutableStateFlow<List<AutomatedRoutine>>(emptyList())
    val routines: StateFlow<List<AutomatedRoutine>> = _routines.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveTimeBlockState?>(null)
    val activeSession: StateFlow<ActiveTimeBlockState?> = _activeSession.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Load Focus Modes
        val serializedModes = prefs.getString("saved_focus_modes", null)
        if (!serializedModes.isNullOrBlank()) {
            try {
                _focusModes.value = json.decodeFromString(serializedModes)
            } catch (e: Exception) {
                e.printStackTrace()
                _focusModes.value = getDefaultFocusModes()
            }
        } else {
            _focusModes.value = getDefaultFocusModes()
            persistFocusModes(_focusModes.value)
        }

        // Load Routines
        val serializedRoutines = prefs.getString("saved_routines", null)
        if (!serializedRoutines.isNullOrBlank()) {
            try {
                _routines.value = json.decodeFromString(serializedRoutines)
            } catch (e: Exception) {
                e.printStackTrace()
                _routines.value = emptyList()
            }
        } else {
            _routines.value = emptyList()
        }

        syncActiveBlocks(context)
    }

    private fun getDefaultFocusModes(): List<FocusMode> {
        return listOf(
            FocusMode(
                name = "Deep Work",
                iconName = "mode_fill0",
                defaultDurationMinutes = 60
            ),
            FocusMode(
                name = "Study",
                iconName = "schedule_fill0",
                defaultDurationMinutes = 45
            ),
            FocusMode(
                name = "Sleeping",
                iconName = "dark_mode_fill0",
                defaultDurationMinutes = 480
            ),
            FocusMode(
                name = "Family Time",
                iconName = "backpack_fill0",
                defaultDurationMinutes = 90
            )
        )
    }

    fun saveFocusMode(mode: FocusMode): FocusMode {
        val existingIndex = _focusModes.value.indexOfFirst { it.id == mode.id }
        val updated = if (existingIndex >= 0) {
            _focusModes.value.toMutableList().apply { set(existingIndex, mode) }
        } else {
            _focusModes.value + mode
        }
        persistFocusModes(updated)
        return mode
    }

    fun deleteFocusMode(id: String, secretProvided: String?, onResult: (Boolean, String?) -> Unit) {
        val mode = _focusModes.value.find { it.id == id } ?: return onResult(false, "Focus Mode not found")
        if (mode.isRunning) {
            // Require key to abort and delete
            if (!validateSecret(mode.keyType, mode.keySecret, secretProvided)) {
                return onResult(false, "Invalid key secret. Key required to delete active mode.")
            }
            stopFocusMode(id, secretProvided ?: "", onResult)
        }
        val updated = _focusModes.value.filterNot { it.id == id }
        persistFocusModes(updated)
        onResult(true, null)
    }

    fun startFocusMode(id: String, durationMinutes: Int, onResult: (Boolean, String?) -> Unit) {
        val mode = _focusModes.value.find { it.id == id } ?: return onResult(false, "Focus Mode not found")
        val now = System.currentTimeMillis()
        val endsAt = now + (durationMinutes * 60 * 1000L)

        val updatedMode = mode.copy(
            isRunning = true,
            sessionStartedAt = now,
            sessionEndsAt = endsAt
        )
        saveFocusMode(updatedMode)
        syncActiveBlocks(context)
        onResult(true, null)
    }

    fun stopFocusMode(id: String, secretProvided: String, onResult: (Boolean, String?) -> Unit) {
        val mode = _focusModes.value.find { it.id == id } ?: return onResult(false, "Focus Mode not found")
        if (!mode.isRunning) return onResult(true, null)

        if (!validateSecret(mode.keyType, mode.keySecret, secretProvided)) {
            return onResult(false, "Incorrect key. Valid key is required to stop session.")
        }

        val updatedMode = mode.copy(
            isRunning = false,
            sessionStartedAt = null,
            sessionEndsAt = null
        )
        saveFocusMode(updatedMode)
        syncActiveBlocks(context)
        onResult(true, null)
    }

    fun saveRoutine(routine: AutomatedRoutine): AutomatedRoutine {
        val existingIndex = _routines.value.indexOfFirst { it.id == routine.id }
        val updated = if (existingIndex >= 0) {
            _routines.value.toMutableList().apply { set(existingIndex, routine) }
        } else {
            _routines.value + routine
        }
        persistRoutines(updated)
        syncActiveBlocks(context)
        return routine
    }

    fun deleteRoutine(id: String, secretProvided: String?, onResult: (Boolean, String?) -> Unit) {
        val routine = _routines.value.find { it.id == id } ?: return onResult(false, "Routine not found")
        if (routine.isEnabled || routine.isActiveNow) {
            if (!validateSecret(routine.keyType, routine.keySecret, secretProvided)) {
                return onResult(false, "Key required to delete routine.")
            }
        }
        val updated = _routines.value.filterNot { it.id == id }
        persistRoutines(updated)
        syncActiveBlocks(context)
        onResult(true, null)
    }

    fun toggleRoutine(id: String, secretProvided: String?, onResult: (Boolean, String?) -> Unit) {
        val routine = _routines.value.find { it.id == id } ?: return onResult(false, "Routine not found")
        // "all routines must require their key to disable/activate"
        if (!validateSecret(routine.keyType, routine.keySecret, secretProvided)) {
            return onResult(false, "Key required to change routine status.")
        }

        val updated = routine.copy(isEnabled = !routine.isEnabled)
        saveRoutine(updated)
        onResult(true, null)
    }

    private fun validateSecret(keyType: KeyType, storedSecret: String?, providedSecret: String?): Boolean {
        if (storedSecret.isNullOrBlank()) return true // No key configured
        if (providedSecret == null) return false
        return storedSecret.trim() == providedSecret.trim()
    }

    /**
     * Synchronizes all active focus modes and scheduled routines:
     * - Evaluates time windows
     * - Suspends/unsuspends packages
     * - Applies or clears DNS
     * - Starts or stops Foreground Service notification
     * - Schedules upcoming AlarmManager triggers
     */
    fun syncActiveBlocks(context: Context) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun .. 7=Sat
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentDayMinutes = currentHour * 60 + currentMinute

        // 1. Evaluate Running Focus Modes
        var activeFocusMode: FocusMode? = null
        val updatedModes = _focusModes.value.map { mode ->
            if (mode.isRunning) {
                if (mode.sessionEndsAt != null && now >= mode.sessionEndsAt) {
                    // Session expired naturally
                    mode.copy(isRunning = false, sessionStartedAt = null, sessionEndsAt = null)
                } else {
                    activeFocusMode = mode
                    mode
                }
            } else {
                mode
            }
        }
        if (updatedModes != _focusModes.value) {
            persistFocusModes(updatedModes)
        }

        // 2. Evaluate Enabled Routines
        var activeRoutine: AutomatedRoutine? = null
        val updatedRoutines = _routines.value.map { routine ->
            if (routine.isEnabled && routine.daysOfWeek.contains(currentDay)) {
                val startMins = routine.startHour * 60 + routine.startMinute
                val endMins = routine.endHour * 60 + routine.endMinute
                val isCurrentlyActive = if (startMins <= endMins) {
                    currentDayMinutes in startMins until endMins
                } else {
                    // Overnights (e.g. 22:00 to 06:00)
                    currentDayMinutes >= startMins || currentDayMinutes < endMins
                }
                if (isCurrentlyActive && activeRoutine == null) {
                    activeRoutine = routine
                }
                routine.copy(isActiveNow = isCurrentlyActive)
            } else {
                routine.copy(isActiveNow = false)
            }
        }
        if (updatedRoutines != _routines.value) {
            persistRoutines(updatedRoutines)
        }

        // 3. Collect packages to suspend
        val packagesToSuspend = mutableSetOf<String>()
        var needDnsBlocking = false
        var dnsHostToUse: String? = null

        // From manual active locks
        keyRepository.activeLocks.value.forEach { lock ->
            packagesToSuspend.addAll(lock.packageNames)
        }

        // From active Focus Mode
        activeFocusMode?.let { mode ->
            packagesToSuspend.addAll(resolvePackages(mode.targetBagIds, mode.targetPackageNames))
            if (mode.enableDnsBlocking) {
                needDnsBlocking = true
                dnsHostToUse = mode.dnsHost
            }
        }

        // From active Routine
        activeRoutine?.let { routine ->
            packagesToSuspend.addAll(resolvePackages(routine.targetBagIds, routine.targetPackageNames))
            if (routine.enableDnsBlocking) {
                needDnsBlocking = true
                dnsHostToUse = routine.dnsHost
            }
        }

        // 4. Enforce app suspension via PolicyBridgeManager
        val allBags = bagRepository.bags.value
        val allKnownPackages = allBags.flatMap { it.packageNames }.toSet() + packagesToSuspend
        val packagesToUnsuspend = allKnownPackages - packagesToSuspend

        if (packagesToSuspend.isNotEmpty()) {
            PolicyBridgeManager.suspendPackages(context, packagesToSuspend.toList(), true)
            PolicyBridgeManager.setUninstallBlocked(context, packagesToSuspend.toList(), true)
        }
        if (packagesToUnsuspend.isNotEmpty()) {
            PolicyBridgeManager.suspendPackages(context, packagesToUnsuspend.toList(), false)
            PolicyBridgeManager.setUninstallBlocked(context, packagesToUnsuspend.toList(), false)
        }

        // 5. Enforce DNS Blocking
        if (needDnsBlocking && !dnsHostToUse.isNullOrBlank()) {
            DnsManager.setDns(context, DnsMode.SPECIFIED_HOST, dnsHostToUse)
        }

        // 6. Update Active Session State & Foreground Notification
        val currentActiveState: ActiveTimeBlockState? = when {
            activeFocusMode != null -> {
                val mode = activeFocusMode!!
                ActiveTimeBlockState(
                    type = TimeBlockType.FOCUS_MODE,
                    id = mode.id,
                    title = mode.name,
                    endsAt = mode.sessionEndsAt,
                    packagesSuspended = resolvePackages(mode.targetBagIds, mode.targetPackageNames),
                    dnsBlocked = mode.enableDnsBlocking,
                    keyType = mode.keyType,
                    keySecret = mode.keySecret
                )
            }
            activeRoutine != null -> {
                val routine = activeRoutine!!
                val endCalendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, routine.endHour)
                    set(Calendar.MINUTE, routine.endMinute)
                    set(Calendar.SECOND, 0)
                    if (timeInMillis <= now) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                ActiveTimeBlockState(
                    type = TimeBlockType.ROUTINE,
                    id = routine.id,
                    title = routine.name,
                    endsAt = endCalendar.timeInMillis,
                    packagesSuspended = resolvePackages(routine.targetBagIds, routine.targetPackageNames),
                    dnsBlocked = routine.enableDnsBlocking,
                    keyType = routine.keyType,
                    keySecret = routine.keySecret
                )
            }
            else -> null
        }
        _activeSession.value = currentActiveState

        // Update Foreground Service
        if (currentActiveState != null) {
            TimeBlockService.start(context, currentActiveState.title, currentActiveState.endsAt ?: (now + 3600000L))
        } else {
            TimeBlockService.stop(context)
        }

        // 7. Schedule Next Transition Alarms
        scheduleNextAlarm(context)
    }

    private fun resolvePackages(bagIds: List<String>, explicitPackages: List<String>): List<String> {
        val bagPackages = bagRepository.bags.value
            .filter { bagIds.contains(it.id) }
            .flatMap { it.packageNames }
        return (bagPackages + explicitPackages).distinct()
    }

    private fun scheduleNextAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        // Find nearest future event timestamp
        var nearestTime: Long? = null

        // 1. Running Focus Mode end
        _focusModes.value.filter { it.isRunning && it.sessionEndsAt != null && it.sessionEndsAt > now }.forEach {
            val end = it.sessionEndsAt!!
            if (nearestTime == null || end < nearestTime!!) nearestTime = end
        }

        // 2. Routine start / end transitions within next 24-48 hours
        _routines.value.filter { it.isEnabled }.forEach { routine ->
            val cal = Calendar.getInstance()
            for (dayOffset in 0..7) {
                val checkCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                }
                val checkDay = checkCal.get(Calendar.DAY_OF_WEEK)
                if (routine.daysOfWeek.contains(checkDay)) {
                    val start = checkCal.apply {
                        set(Calendar.HOUR_OF_DAY, routine.startHour)
                        set(Calendar.MINUTE, routine.startMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    if (start > now && (nearestTime == null || start < nearestTime!!)) {
                        nearestTime = start
                    }

                    val end = checkCal.apply {
                        set(Calendar.HOUR_OF_DAY, routine.endHour)
                        set(Calendar.MINUTE, routine.endMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    if (end > now && (nearestTime == null || end < nearestTime!!)) {
                        nearestTime = end
                    }
                }
            }
        }

        if (nearestTime != null) {
            val intent = Intent(context, TimeBlockAlarmReceiver::class.java).apply {
                action = TimeBlockAlarmReceiver.ACTION_TIME_BLOCK_TRIGGER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nearestTime!!, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, nearestTime!!, pendingIntent)
                }
            } catch (e: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nearestTime!!, pendingIntent)
            }
        }
    }

    private fun persistFocusModes(list: List<FocusMode>) {
        _focusModes.value = list
        prefs.edit().putString("saved_focus_modes", json.encodeToString(list)).apply()
    }

    private fun persistRoutines(list: List<AutomatedRoutine>) {
        _routines.value = list
        prefs.edit().putString("saved_routines", json.encodeToString(list)).apply()
    }
}
