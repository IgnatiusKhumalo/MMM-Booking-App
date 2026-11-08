package com.mmm.bookingapp.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

/** Unit tests for the 24-hour cancellation notice rule (7 tests). */
public class CancellationPolicyTest {

    private static final String DATE = "2026-11-10";
    private static final String TIME = "10:30";
    private static final long HOUR = 60L * 60L * 1000L;

    private final long start = CancellationPolicy.appointmentStartMillis(DATE, TIME);

    @Test public void startMillis_parsesDateAndTime() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(start);
        assertEquals(2026, c.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, c.get(Calendar.MONTH));
        assertEquals(10, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(10, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, c.get(Calendar.MINUTE));
    }

    @Test public void moreThan24HoursAway_cancellationIsFree() {
        assertFalse(CancellationPolicy.isWithinNoticePeriod(DATE, TIME, start - 48 * HOUR));
    }

    @Test public void exactly24HoursAway_isStillFree() {
        assertFalse(CancellationPolicy.isWithinNoticePeriod(DATE, TIME, start - 24 * HOUR));
    }

    @Test public void oneMinuteInsideTheNoticePeriod_isClosed() {
        assertTrue(CancellationPolicy.isWithinNoticePeriod(DATE, TIME, start - 24 * HOUR + 60_000L));
    }

    @Test public void fewHoursBefore_isClosed() {
        assertTrue(CancellationPolicy.isWithinNoticePeriod(DATE, TIME, start - 2 * HOUR));
    }

    @Test public void alreadyStarted_isClosed() {
        assertTrue(CancellationPolicy.isWithinNoticePeriod(DATE, TIME, start + HOUR));
    }

    @Test public void unreadableDateOrTime_neverBlocksTheClient() {
        assertEquals(-1, CancellationPolicy.appointmentStartMillis("bad", "x"));
        assertFalse(CancellationPolicy.isWithinNoticePeriod("bad", TIME, start));
        assertFalse(CancellationPolicy.isWithinNoticePeriod(null, null, start));
    }
}