package com.walletconnect.pos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class DateRangesTest {

    private val localZone = ZoneId.systemDefault()

    @Test
    fun `today - startTime should be local midnight`() {
        val range = Pos.DateRanges.today()

        val expectedMidnight = LocalDate.now(localZone)
            .atStartOfDay(localZone)
            .toInstant()

        assertEquals(expectedMidnight, range.startTime)
    }

    @Test
    fun `today - endTime should include clock skew buffer`() {
        val beforeCall = Instant.now()
        val range = Pos.DateRanges.today()
        val afterCall = Instant.now()

        // endTime should be approximately 2 minutes (120 seconds) after now
        val bufferSeconds = 120L
        val expectedMin = beforeCall.plusSeconds(bufferSeconds)
        val expectedMax = afterCall.plusSeconds(bufferSeconds)

        assertTrue(
            "endTime should be ~2 minutes after now (between $expectedMin and $expectedMax, got ${range.endTime})",
            !range.endTime.isBefore(expectedMin) && !range.endTime.isAfter(expectedMax)
        )
    }

    @Test
    fun `today - range should span from midnight to now`() {
        val range = Pos.DateRanges.today()

        assertTrue(
            "startTime should be before or equal to endTime",
            !range.startTime.isAfter(range.endTime)
        )
    }

    @Test
    fun `lastDays - with 1 day should equal today`() {
        val todayRange = Pos.DateRanges.today()
        val lastOneDayRange = Pos.DateRanges.lastDays(1)

        assertEquals(todayRange.startTime, lastOneDayRange.startTime)
    }

    @Test
    fun `lastDays - with 7 days should start 6 days before today midnight`() {
        val range = Pos.DateRanges.lastDays(7)

        val expectedStart = LocalDate.now(localZone)
            .minusDays(6)
            .atStartOfDay(localZone)
            .toInstant()

        assertEquals(expectedStart, range.startTime)
    }

    @Test
    fun `lastDays - should throw on zero days`() {
        try {
            Pos.DateRanges.lastDays(0)
            assertTrue("Should have thrown IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("positive") == true)
        }
    }

    @Test
    fun `lastDays - should throw on negative days`() {
        try {
            Pos.DateRanges.lastDays(-1)
            assertTrue("Should have thrown IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("positive") == true)
        }
    }

    @Test
    fun `thisWeek - startTime should be Monday midnight`() {
        val range = Pos.DateRanges.thisWeek()

        val today = LocalDate.now(localZone)
        val daysFromMonday = (today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()
        val expectedMonday = today.minusDays(daysFromMonday)
            .atStartOfDay(localZone)
            .toInstant()

        assertEquals(expectedMonday, range.startTime)
    }

    @Test
    fun `thisWeek - should include at least today`() {
        val now = Instant.now()
        val range = Pos.DateRanges.thisWeek()

        assertTrue(
            "startTime should be before or at now",
            !range.startTime.isAfter(now)
        )
    }

    @Test
    fun `thisMonth - startTime should be first of month midnight`() {
        val range = Pos.DateRanges.thisMonth()

        val expectedFirstOfMonth = LocalDate.now(localZone)
            .withDayOfMonth(1)
            .atStartOfDay(localZone)
            .toInstant()

        assertEquals(expectedFirstOfMonth, range.startTime)
    }

    @Test
    fun `thisMonth - should include at least today`() {
        val now = Instant.now()
        val range = Pos.DateRanges.thisMonth()

        assertTrue(
            "startTime should be before or at now",
            !range.startTime.isAfter(now)
        )
    }

    @Test
    fun `all ranges should have startTime before endTime`() {
        val ranges = listOf(
            Pos.DateRanges.today(),
            Pos.DateRanges.lastDays(7),
            Pos.DateRanges.thisWeek(),
            Pos.DateRanges.thisMonth()
        )

        ranges.forEach { range ->
            assertTrue(
                "startTime should be before or equal to endTime",
                !range.startTime.isAfter(range.endTime)
            )
        }
    }

    @Test
    fun `ranges should use local timezone not UTC`() {
        val localMidnight = LocalDate.now(localZone)
            .atStartOfDay(localZone)
            .toInstant()
        val utcMidnight = LocalDate.now(ZoneId.of("UTC"))
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()

        val todayRange = Pos.DateRanges.today()

        // If timezone differs from UTC, these should be different
        // This test verifies local timezone is used
        assertEquals(
            "today() should use local timezone midnight",
            localMidnight,
            todayRange.startTime
        )
    }
}
