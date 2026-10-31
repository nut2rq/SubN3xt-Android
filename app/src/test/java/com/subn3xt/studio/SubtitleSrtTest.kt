package com.subn3xt.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleSrtTest {
    @Test
    fun formatsMillisecondsAsSrtTimestamp() {
        assertEquals("00:00:02,880", msToSrt(2_880))
        assertEquals("01:02:03,004", msToSrt(3_723_004))
        assertEquals("100:00:00,000", msToSrt(360_000_000))
    }

    @Test
    fun parsesCommaAndDotMillisecondSeparators() {
        assertEquals(2_880L, parseSrtTime("00:00:02,880"))
        assertEquals(2_880L, parseSrtTime("00:00:02.880"))
        assertEquals(null, parseSrtTime("00:60:02,000"))
        assertEquals(null, parseSrtTime("00:00:02,88"))
        assertEquals(null, parseSrtTime("not-a-timestamp"))
    }

    @Test
    fun exportsRealLineBreaksAndPreservesUnicodeText() {
        val output = buildSrt(
            listOf(
                Cue("00:00:00,000", "00:00:02,880", "好 的 大家都到齐了吧"),
                Cue("00:00:30,000", "00:00:31,800", "ภาษาไทย และ English")
            )
        )

        val expected = listOf(
            "1",
            "00:00:00,000 --> 00:00:02,880",
            "好 的 大家都到齐了吧",
            "",
            "2",
            "00:00:30,000 --> 00:00:31,800",
            "ภาษาไทย และ English",
            ""
        ).joinToString("\r\n")

        assertEquals(expected, output)
        assertFalse(output.contains("\\n"))
        assertTrue(output.contains("\r\n"))
    }

    @Test
    fun rejectsInvalidOrEmptyCues() {
        assertTrue(runCatching { buildSrt(emptyList()) }.isFailure)
        assertTrue(runCatching {
            buildSrt(listOf(Cue("00:00:02,000", "00:00:01,000", "bad timing")))
        }.isFailure)
        assertTrue(runCatching {
            buildSrt(listOf(Cue("bad", "00:00:01,000", "bad start")))
        }.isFailure)
        assertTrue(runCatching {
            buildSrt(listOf(Cue("00:00:00,000", "00:00:01,000", "  ")))
        }.isFailure)
    }

    @Test
    fun rejectsCuesOutOfChronologicalOrder() {
        assertTrue(runCatching {
            buildSrt(
                listOf(
                    Cue("00:00:10,000", "00:00:11,000", "second"),
                    Cue("00:00:01,000", "00:00:02,000", "first")
                )
            )
        }.isFailure)
    }
}
