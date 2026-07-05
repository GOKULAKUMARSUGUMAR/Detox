package com.detoxapp.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Stores a running count of how many times each app's Reels/Shorts view
 * was auto-blocked, bucketed by calendar day, using plain SharedPreferences
 * (no database needed for simple counters).
 */
class StatsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    enum class App(val key: String) {
        INSTAGRAM("instagram"),
        YOUTUBE("youtube")
    }

    /** Call this every time the accessibility service auto-blocks a Reels/Shorts screen. */
    fun recordBlock(app: App, atMillis: Long = System.currentTimeMillis()) {
        val day = dayFormat.format(atMillis)
        val key = countKey(day, app)
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    fun getCountForDay(app: App, daysAgo: Int): Int {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        val day = dayFormat.format(cal.timeInMillis)
        return prefs.getInt(countKey(day, app), 0)
    }

    fun getTodayCount(app: App): Int = getCountForDay(app, 0)

    /** Returns the last [days] counts, oldest first, combined across both apps. */
    fun getLastDaysTotals(days: Int = 7): List<Int> {
        return (days - 1 downTo 0).map { daysAgo ->
            getCountForDay(App.INSTAGRAM, daysAgo) + getCountForDay(App.YOUTUBE, daysAgo)
        }
    }

    fun getTodayTotal(): Int = getTodayCount(App.INSTAGRAM) + getTodayCount(App.YOUTUBE)

    private fun countKey(day: String, app: App) = "count_${day}_${app.key}"

    companion object {
        private const val PREFS_NAME = "detox_stats"
    }
}
