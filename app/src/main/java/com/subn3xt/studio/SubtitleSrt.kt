package com.subn3xt.studio

import java.util.Locale

data class Cue(
    val start: String = "00:00:00,000",
    val end: String = "00:00:03,000",
    val text: String = ""
)

/** Converts non-negative milliseconds to the SRT timestamp format HH:MM:SS,mmm. */
internal fun msToSrt(ms: Long): String {
    require(ms >= 0) { "Timestamp cannot be negative" }
    val hours = ms / 3_600_000
    val minutes = (ms % 3_600_000) / 60_000
    val seconds = (ms % 60_000) / 1_000
    val millis = ms % 1_000
    return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
}

/** Parses SRT timestamps with either comma or dot milliseconds. */
internal fun parseSrtTime(value: String): Long? {
    val match = Regex("^(\\d{2,}):(\\d{2}):(\\d{2})[,.](\\d{3})$").matchEntire(value.trim())
        ?: return null
    val (hoursText, minutesText, secondsText, millisText) = match.destructured
    val hours = hoursText.toLongOrNull() ?: return null
    val minutes = minutesText.toLongOrNull() ?: return null
    val seconds = secondsText.toLongOrNull() ?: return null
    val millis = millisText.toLongOrNull() ?: return null
    if (minutes > 59 || seconds > 59) return null

    return try {
        Math.addExact(
            Math.multiplyExact(
                Math.addExact(
                    Math.addExact(Math.multiplyExact(hours, 3_600L), minutes * 60L),
                    seconds
                ),
                1_000L
            ),
            millis
        )
    } catch (_: ArithmeticException) {
        null
    }
}

/**
 * Builds standards-compatible UTF-8 SRT text using real CRLF line breaks.
 * Cues are kept in the supplied order; invalid or out-of-order cues are rejected.
 */
internal fun buildSrt(cues: List<Cue>): String {
    require(cues.isNotEmpty()) { "No subtitle lines to export" }
    var previousStart = -1L

    val blocks = cues.mapIndexed { index, cue ->
        val line = index + 1
        val start = parseSrtTime(cue.start)
            ?: throw IllegalArgumentException("Line $line: invalid start timestamp")
        val end = parseSrtTime(cue.end)
            ?: throw IllegalArgumentException("Line $line: invalid end timestamp")
        require(end > start) { "Line $line: end time must be after start time" }
        require(start >= previousStart) { "Line $line: cues must be ordered by start time" }
        require(cue.text.isNotBlank()) { "Line $line: subtitle text is empty" }
        previousStart = start

        listOf(
            line.toString(),
            "${msToSrt(start)} --> ${msToSrt(end)}",
            cue.text.trim()
        ).joinToString("\r\n")
    }

    return blocks.joinToString("\r\n\r\n") + "\r\n"
}
