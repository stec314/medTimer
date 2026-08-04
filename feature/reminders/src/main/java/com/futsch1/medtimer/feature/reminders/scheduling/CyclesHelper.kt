package com.futsch1.medtimer.feature.reminders.scheduling

import android.annotation.SuppressLint
import com.futsch1.medtimer.core.domain.model.Reminder
import java.time.LocalDate

object CyclesHelper {
    @SuppressLint("DefaultLocale")
    fun getCycleCountString(reminder: Reminder): String {
        if (reminder.pauseDays == 0 || reminder.consecutiveDays == 1) {
            return ""
        }

        val cycleStartDay = reminder.cycleStartDay
        val dayInCycle = LocalDate.now().toEpochDay() - cycleStartDay.toEpochDay()
        val cycleLength = reminder.consecutiveDays + reminder.pauseDays

        // Kotlin's % can return a negative remainder for a negative dividend (possible when
        // cycleStartDay is in the future); normalize into [0, cycleLength) before displaying.
        val dayWithinCycle = ((dayInCycle % cycleLength) + cycleLength) % cycleLength + 1
        return " ($dayWithinCycle/${reminder.consecutiveDays})"
    }
}
