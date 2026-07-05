package com.detoxapp.data

import android.content.Context
import java.util.Calendar

/**
 * Stores the "focus schedule" - the window of time during which Reels/Shorts
 * blocking is enforced. If schedule mode is off, blocking is always active.
 * If it's on, blocking only happens on the selected days between start/end time.
 * Handles overnight ranges (e.g. 22:00 -> 06:00).
 */
class ScheduleRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var scheduleModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    /** Minutes since midnight, e.g. 9:30 AM = 570 */
    var startMinutes: Int
        get() = prefs.getInt(KEY_START, DEFAULT_START)
        set(value) = prefs.edit().putInt(KEY_START, value).apply()

    var endMinutes: Int
        get() = prefs.getInt(KEY_END, DEFAULT_END)
        set(value) = prefs.edit().putInt(KEY_END, value).apply()

    /** Bit i (0=Sunday .. 6=Saturday, matching Calendar.DAY_OF_WEEK - 1) set = active that day. */
    var daysMask: Int
        get() = prefs.getInt(KEY_DAYS, DEFAULT_DAYS_MASK)
        set(value) = prefs.edit().putInt(KEY_DAYS, value).apply()

    fun isDayEnabled(calendarDayOfWeek: Int): Boolean {
        val bit = calendarDayOfWeek - 1 // Calendar.SUNDAY == 1
        return (daysMask shr bit) and 1 == 1
    }

    fun setDayEnabled(calendarDayOfWeek: Int, enabled: Boolean) {
        val bit = calendarDayOfWeek - 1
        daysMask = if (enabled) daysMask or (1 shl bit) else daysMask and (1 shl bit).inv()
    }

    /** Should blocking be active right now, given the current schedule settings? */
    fun isBlockingActiveNow(): Boolean {
        if (!scheduleModeEnabled) return true // always-on mode

        val cal = Calendar.getInstance()
        if (!isDayEnabled(cal.get(Calendar.DAY_OF_WEEK))) return false

        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = startMinutes
        val end = endMinutes

        return if (start <= end) {
            nowMinutes in start until end
        } else {
            // overnight range, e.g. 22:00 -> 06:00
            nowMinutes >= start || nowMinutes < end
        }
    }

    companion object {
        private const val PREFS_NAME = "detox_schedule"
        private const val KEY_ENABLED = "schedule_enabled"
        private const val KEY_START = "start_minutes"
        private const val KEY_END = "end_minutes"
        private const val KEY_DAYS = "days_mask"

        private const val DEFAULT_START = 9 * 60   // 9:00 AM
        private const val DEFAULT_END = 18 * 60     // 6:00 PM
        // Mon-Fri by default (bits 1..5, since Sunday=bit0, Saturday=bit6)
        private const val DEFAULT_DAYS_MASK = 0b0111110
    }
}
