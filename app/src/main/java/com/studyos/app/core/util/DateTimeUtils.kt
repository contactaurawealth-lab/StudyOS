package com.studyos.app.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    private val headerDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val dayOfWeekShortFormatter = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

    fun getStartOfDay(
        date: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun getEndOfDay(
        date: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        return date.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()
    }

    fun getStartOfWeek(date: LocalDate = LocalDate.now()): LocalDate {
        val dayOfWeek = date.dayOfWeek.value // 1 = Monday, 7 = Sunday
        return date.minusDays((dayOfWeek - 1).toLong())
    }

    fun getEndOfWeek(date: LocalDate = LocalDate.now()): LocalDate {
        val start = getStartOfWeek(date)
        return start.plusDays(6)
    }

    fun formatMonthYear(date: LocalDate = LocalDate.now()): String {
        return date.format(monthYearFormatter)
    }

    fun formatDayOfWeekShort(date: LocalDate): String {
        return date.format(dayOfWeekShortFormatter)
    }

    fun formatDayNumber(date: LocalDate): String {
        return date.dayOfMonth.toString()
    }

    fun formatDateHeader(date: LocalDate = LocalDate.now()): String {
        return date.format(headerDateFormatter)
    }

    fun formatDateShort(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val zonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return zonedDateTime.format(shortDateFormatter)
    }

    fun formatTime(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val zonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return zonedDateTime.format(timeFormatter)
    }

    fun getGreeting(localTime: LocalTime = LocalTime.now()): String {
        val hour = localTime.hour
        return when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
