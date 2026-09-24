package com.mcc.construction.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mcc.construction.R
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/*
 * Shared formatting, so a date or a rand amount reads the same on every screen.
 *
 * Currency is always South African rand, whatever the device locale — the
 * figures come from South African projects.
 */

private val SouthAfrica: Locale = Locale.Builder().setLanguage("en").setRegion("ZA").build()

private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
private val dayMonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "R 48 200 000,00" — rand, with South African grouping. */
fun BigDecimal.formatRand(): String {
    val format = NumberFormat.getCurrencyInstance(SouthAfrica).apply {
        currency = Currency.getInstance("ZAR")
    }
    return format.format(this)
}

/** "24 Sep 2026". */
fun LocalDate.formatDate(): String = format(dateFormat)

/** "24 Sep 2026". */
fun LocalDateTime.formatDate(): String = format(dateFormat)

/** "07:42". */
fun LocalDateTime.formatTime(): String = format(timeFormat)

/**
 * A timestamp the way people say it: "Just now", "12 min ago", "3 h ago",
 * "Yesterday, 16:40", then "21 Sep, 08:15".
 *
 * @param now pass a fixed value in previews and tests.
 */
@Composable
fun relativeTime(time: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
    val elapsed = Duration.between(time, now)
    val minutes = elapsed.toMinutes()
    val hours = elapsed.toHours()
    return when {
        minutes < 1 -> stringResource(R.string.time_just_now)
        minutes < 60 -> pluralStringResource(
            R.plurals.time_minutes_ago, minutes.toInt(), minutes.toInt()
        )
        time.toLocalDate() == now.toLocalDate() && hours < 24 -> pluralStringResource(
            R.plurals.time_hours_ago, hours.toInt(), hours.toInt()
        )
        time.toLocalDate() == now.toLocalDate().minusDays(1) ->
            stringResource(R.string.time_yesterday_at, time.format(timeFormat))
        else -> stringResource(
            R.string.time_date_at,
            time.format(dayMonthFormat),
            time.format(timeFormat)
        )
    }
}
