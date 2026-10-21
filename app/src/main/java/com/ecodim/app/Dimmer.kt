package com.ecodim.app

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.service.quicksettings.TileService
import android.text.TextUtils
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * Single source of truth for dimming state. The activity, tile and notification write here;
 * [DimService] observes it and draws the overlay. Everything lives in one process.
 */
object Dimmer {
    const val MIN_LEVEL = 0.10f
    // Capped so the screen stays readable enough to find the off switch.
    const val MAX_LEVEL = 0.90f
    const val STEP = 0.10f

    private const val KEY_ACTIVE = "active"
    private const val KEY_LEVEL = "level"
    private const val KEY_WARMTH = "warmth"
    private const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
    private const val KEY_SCHEDULE_START = "schedule_start"
    private const val KEY_SCHEDULE_END = "schedule_end"

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _level = MutableStateFlow(0.5f)
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _warmth = MutableStateFlow(0f)
    val warmth: StateFlow<Float> = _warmth.asStateFlow()

    private val _schedule = MutableStateFlow(Schedule())
    val schedule: StateFlow<Schedule> = _schedule.asStateFlow()

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences("ecodim", Context.MODE_PRIVATE)
        _active.value = prefs.getBoolean(KEY_ACTIVE, false)
        _level.value = prefs.getFloat(KEY_LEVEL, 0.5f).coerceIn(MIN_LEVEL, MAX_LEVEL)
        _warmth.value = prefs.getFloat(KEY_WARMTH, 0f).coerceIn(0f, 1f)
        val defaults = Schedule()
        _schedule.value = Schedule(
            enabled = prefs.getBoolean(KEY_SCHEDULE_ENABLED, defaults.enabled),
            startMinute = prefs.getInt(KEY_SCHEDULE_START, defaults.startMinute),
            endMinute = prefs.getInt(KEY_SCHEDULE_END, defaults.endMinute),
        )
    }

    val levelPercent: Int get() = (_level.value * 100).roundToInt()

    fun setActive(on: Boolean) {
        if (_active.value == on) return
        _active.value = on
        prefs.edit { putBoolean(KEY_ACTIVE, on) }
        refreshTile()
    }

    fun toggle() = setActive(!_active.value)

    fun setLevel(value: Float) {
        val clamped = value.coerceIn(MIN_LEVEL, MAX_LEVEL)
        if (_level.value == clamped) return
        _level.value = clamped
        prefs.edit { putFloat(KEY_LEVEL, clamped) }
        refreshTile()
    }

    fun nudge(delta: Float) = setLevel(((_level.value + delta) * 100).roundToInt() / 100f)

    fun setWarmth(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _warmth.value = clamped
        prefs.edit { putFloat(KEY_WARMTH, clamped) }
    }

    fun setSchedule(schedule: Schedule) {
        _schedule.value = schedule
        prefs.edit {
            putBoolean(KEY_SCHEDULE_ENABLED, schedule.enabled)
            putInt(KEY_SCHEDULE_START, schedule.startMinute)
            putInt(KEY_SCHEDULE_END, schedule.endMinute)
        }
    }

    internal fun setServiceRunning(running: Boolean) {
        _serviceRunning.value = running
        refreshTile()
    }

    /**
     * True when the accessibility service is switched on. The setting can be on a moment before
     * the service connects, so check both.
     */
    fun isServiceEnabled(context: Context): Boolean {
        if (_serviceRunning.value) return true
        val expected = ComponentName(context, DimService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
        return splitter.any { ComponentName.unflattenFromString(it) == expected }
    }

    private fun refreshTile() {
        // Best effort: some launchers throw if the tile was never added.
        runCatching {
            TileService.requestListeningState(appContext, ComponentName(appContext, DimTileService::class.java))
        }
    }
}
