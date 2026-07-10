package com.fginc.weldo.data.remote

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Parsing/formatting for the backend's timestamps. The server serializes Java `Instant`
 * with microsecond precision (e.g. `2026-07-10T12:34:56.123456Z`) which most stock parsers
 * choke on, and `LocalDate` (`2026-07-10`) for calendar-day fields like `dueDate`.
 * All model date fields are raw Strings; convert here at the edges only.
 */
object WeldoTime {

    /** Epoch millis for sorting, or 0 if unparseable/null. Accepts instants and plain dates. */
    fun epochMillis(value: String?): Long {
        if (value.isNullOrBlank()) return 0
        return parseInstant(value)?.toEpochMilli() ?: 0
    }

    private fun parseInstant(value: String): Instant? = try {
        Instant.parse(value)
    } catch (_: Exception) {
        try {
            OffsetDateTime.parse(value).toInstant()
        } catch (_: Exception) {
            try {
                LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant()
            } catch (_: Exception) {
                null
            }
        }
    }

    /** "Jul 10, 2026" for a due-date/day field, or null. */
    fun formatDay(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val date = try {
            LocalDate.parse(value)
        } catch (_: Exception) {
            parseInstant(value)?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: return null
        }
        return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }

    /** "Jul 10, 2026, 2:34 PM" for an instant, or null. */
    fun formatDateTime(value: String?): String? {
        val instant = if (value.isNullOrBlank()) return null else parseInstant(value) ?: return null
        return instant.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }

    /** ISO instant for "now". */
    fun nowInstant(): String = Instant.now().toString()

    /** ISO date (yyyy-MM-dd) for a picked calendar day given epoch millis (UTC-based picker). */
    fun isoDate(epochMillisUtc: Long): String =
        Instant.ofEpochMilli(epochMillisUtc).atZone(ZoneId.of("UTC")).toLocalDate().toString()

    /** ISO instant from epoch millis. */
    fun isoInstant(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).toString()

    /** IANA timezone id for the X-Timezone header. */
    fun timezoneId(): String = ZoneId.systemDefault().id
}
