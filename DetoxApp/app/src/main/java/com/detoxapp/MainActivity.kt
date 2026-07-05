package com.detoxapp

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import com.detoxapp.data.ScheduleRepository
import com.detoxapp.data.StatsRepository
import com.detoxapp.databinding.ActivityMainBinding
import com.detoxapp.service.ReelsShortsBlockerService
import com.google.android.material.chip.Chip
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var statsRepository: StatsRepository
    private lateinit var scheduleRepository: ScheduleRepository

    // Calendar.SUNDAY(1) .. Calendar.SATURDAY(7), short labels for the chips
    private val dayDefs = listOf(
        Calendar.SUNDAY to "Sun",
        Calendar.MONDAY to "Mon",
        Calendar.TUESDAY to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY to "Thu",
        Calendar.FRIDAY to "Fri",
        Calendar.SATURDAY to "Sat"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        statsRepository = StatsRepository(this)
        scheduleRepository = ScheduleRepository(this)

        binding.enableButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        setupScheduleControls()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        refreshStats()
        refreshScheduleUi()
    }

    private fun refreshStatus() {
        val enabled = isAccessibilityServiceEnabled()
        binding.statusText.text = if (enabled) {
            getString(R.string.status_on)
        } else {
            getString(R.string.status_off)
        }
        binding.statusText.setTextColor(
            resources.getColor(if (enabled) R.color.success else R.color.warning, theme)
        )
        binding.enableButton.text = if (enabled) "Open accessibility settings" else getString(R.string.enable_button)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        val target = ReelsShortsBlockerService::class.java.name
        return enabledServices.any { it.resolveInfo.serviceInfo.name == target }
    }

    private fun refreshStats() {
        val today = statsRepository.getTodayTotal()
        binding.todayStatsText.text = getString(R.string.stats_today, today)

        val totals = statsRepository.getLastDaysTotals(7)
        val labels = (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            listOf("S", "M", "T", "W", "T", "F", "S")[cal.get(Calendar.DAY_OF_WEEK) - 1]
        }
        binding.weeklyChart.setData(totals, labels)
    }

    private fun setupScheduleControls() {
        binding.scheduleSwitch.setOnCheckedChangeListener { _, isChecked ->
            scheduleRepository.scheduleModeEnabled = isChecked
            binding.startTimeButton.isEnabled = isChecked
            binding.endTimeButton.isEnabled = isChecked
            refreshScheduleUi()
        }

        binding.startTimeButton.setOnClickListener {
            showTimePicker(scheduleRepository.startMinutes) { minutes ->
                scheduleRepository.startMinutes = minutes
                refreshScheduleUi()
            }
        }
        binding.endTimeButton.setOnClickListener {
            showTimePicker(scheduleRepository.endMinutes) { minutes ->
                scheduleRepository.endMinutes = minutes
                refreshScheduleUi()
            }
        }

        // Build day-of-week chips once
        dayDefs.forEach { (calendarDay, label) ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = scheduleRepository.isDayEnabled(calendarDay)
                setOnCheckedChangeListener { _, checked ->
                    scheduleRepository.setDayEnabled(calendarDay, checked)
                }
            }
            binding.daysChipGroup.addView(chip)
        }
    }

    private fun refreshScheduleUi() {
        val enabled = scheduleRepository.scheduleModeEnabled
        binding.scheduleSwitch.isChecked = enabled
        binding.startTimeButton.isEnabled = enabled
        binding.endTimeButton.isEnabled = enabled
        binding.startTimeButton.text = "Start: ${formatMinutes(scheduleRepository.startMinutes)}"
        binding.endTimeButton.text = "End: ${formatMinutes(scheduleRepository.endMinutes)}"
    }

    private fun showTimePicker(currentMinutes: Int, onPicked: (Int) -> Unit) {
        val hour = currentMinutes / 60
        val minute = currentMinutes % 60
        TimePickerDialog(this, { _, h, m -> onPicked(h * 60 + m) }, hour, minute, false).show()
    }

    private fun formatMinutes(minutes: Int): String {
        val hour24 = minutes / 60
        val minute = minutes % 60
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format("%d:%02d %s", hour12, minute, amPm)
    }
}
