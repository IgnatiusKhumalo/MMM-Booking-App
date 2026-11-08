package com.mmm.bookingapp.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Unit tests for the practice's breaks and blocked dates/leave (5 tests). */
public class PracticeInfoAvailabilityTest {

    private static Map<String, String> map(String... keyValues) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) m.put(keyValues[i], keyValues[i + 1]);
        return m;
    }

    @Test public void breakRanges_parsesValidBreaksAndSkipsInvalidOnes() {
        PracticeInfo info = new PracticeInfo();
        List<Map<String, String>> breaks = new ArrayList<>();
        breaks.add(map("start", "13:00", "end", "14:00"));
        breaks.add(map("start", "15:00", "end", "14:00")); // ends before it starts
        breaks.add(map("start", "x", "end", "10:00"));      // unreadable
        info.setBreaks(breaks);

        List<int[]> ranges = info.breakRanges();
        assertEquals(1, ranges.size());
        assertEquals(780, ranges.get(0)[0]);
        assertEquals(840, ranges.get(0)[1]);
    }

    @Test public void noBreaks_givesEmptyList() {
        assertTrue(new PracticeInfo().breakRanges().isEmpty());
    }

    @Test public void blockedPeriod_coversEveryDayInclusive_andReturnsTheReason() {
        PracticeInfo info = new PracticeInfo();
        List<Map<String, String>> periods = new ArrayList<>();
        periods.add(map("from", "2026-12-20", "to", "2026-12-31", "reason", "Annual leave"));
        info.setBlockedPeriods(periods);

        assertEquals("Annual leave", info.blockedReasonFor("2026-12-20"));
        assertEquals("Annual leave", info.blockedReasonFor("2026-12-25"));
        assertEquals("Annual leave", info.blockedReasonFor("2026-12-31"));
        assertNull(info.blockedReasonFor("2026-12-19"));
        assertNull(info.blockedReasonFor("2027-01-01"));
    }

    @Test public void blockedPeriodWithoutEndDate_blocksOnlyThatDay() {
        PracticeInfo info = new PracticeInfo();
        List<Map<String, String>> periods = new ArrayList<>();
        periods.add(map("from", "2026-12-24"));
        info.setBlockedPeriods(periods);

        assertTrue(info.isDateBlocked("2026-12-24"));
        assertFalse(info.isDateBlocked("2026-12-25"));
        assertEquals("", info.blockedReasonFor("2026-12-24"));
    }

    @Test public void noBlockedPeriods_neverBlocks() {
        assertFalse(new PracticeInfo().isDateBlocked("2026-12-25"));
    }
}