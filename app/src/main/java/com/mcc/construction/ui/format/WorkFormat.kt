package com.mcc.construction.ui.format

import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale

/** "8.5" — hours worked to one decimal place, as recorded in `ATTENDANCE.TotalHours`. */
fun formatHours(hours: Double): String = String.format(Locale.ROOT, "%.1f", hours)

/** Whole hours and minutes between two times, for "on shift for 2 h 15 min". */
fun elapsedHoursMinutes(from: LocalDateTime, to: LocalDateTime): Pair<Long, Long> {
    val minutes = Duration.between(from, to).toMinutes().coerceAtLeast(0)
    return minutes / 60 to minutes % 60
}
