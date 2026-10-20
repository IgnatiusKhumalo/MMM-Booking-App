package com.mmm.bookingapp.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.mmm.bookingapp.utils.StatusUtils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for the booking rules that live in plain Java: the 15-minute
 * slot blocks that prevent overlapping appointments (BookedSlot), the
 * practice's open days and hours (PracticeInfo), and the null-safe status
 * handling (StatusUtils). 24 tests.
 */
public class BookingRulesTest {

    // ---------- BookedSlot: reservation blocks ----------

    @Test public void blockTimes_sixtyMinutes_givesFourBlocks() {
        assertEquals(Arrays.asList("10:00", "10:15", "10:30", "10:45"),
                BookedSlot.buildBlockTimes("10:00", 60));
    }
    @Test public void blockTimes_fortyFiveMinutes_givesThreeBlocks() {
        assertEquals(Arrays.asList("10:15", "10:30", "10:45"),
                BookedSlot.buildBlockTimes("10:15", 45));
    }
    @Test public void blockTimes_thirtyMinutes_givesTwoBlocks() {
        assertEquals(Arrays.asList("17:30", "17:45"),
                BookedSlot.buildBlockTimes("17:30", 30));
    }
    @Test public void blockTimes_invalidStart_returnsStartUnchanged() {
        assertEquals(Collections.singletonList("bad"), BookedSlot.buildBlockTimes("bad", 30));
    }
    @Test public void blockIds_online_useOnlineSuffix() {
        assertEquals(Arrays.asList("2026-09-01_10:00_online", "2026-09-01_10:15_online"),
                BookedSlot.buildBlockIds("2026-09-01", "10:00", 30, null));
    }
    @Test public void blockIds_inPerson_useLocation() {
        assertEquals(Arrays.asList("2026-09-01_10:00_Midrand", "2026-09-01_10:15_Midrand"),
                BookedSlot.buildBlockIds("2026-09-01", "10:00", 30, "Midrand"));
    }
    @Test public void overlappingBookings_shareAtLeastOneBlock() {
        List<String> first = BookedSlot.buildBlockIds("2026-09-01", "10:00", 60, "Midrand");
        List<String> second = BookedSlot.buildBlockIds("2026-09-01", "10:30", 30, "Midrand");
        assertFalse(Collections.disjoint(first, second));
    }
    @Test public void backToBackBookings_shareNoBlock() {
        List<String> first = BookedSlot.buildBlockIds("2026-09-01", "10:00", 60, "Midrand");
        List<String> second = BookedSlot.buildBlockIds("2026-09-01", "11:00", 30, "Midrand");
        assertTrue(Collections.disjoint(first, second));
    }
    @Test public void sameTimeDifferentLocation_shareNoBlock() {
        List<String> midrand = BookedSlot.buildBlockIds("2026-09-01", "10:00", 60, "Midrand");
        List<String> centurion = BookedSlot.buildBlockIds("2026-09-01", "10:00", 60, "Centurion");
        assertTrue(Collections.disjoint(midrand, centurion));
    }

    // ---------- PracticeInfo: time helpers ----------

    @Test public void parseMinutes_validTime_returnsMinutesAfterMidnight() {
        assertEquals(510, PracticeInfo.parseMinutes("08:30", -1));
    }
    @Test public void parseMinutes_invalidInput_returnsFallback() {
        assertEquals(99, PracticeInfo.parseMinutes("25:00", 99));
        assertEquals(99, PracticeInfo.parseMinutes("8", 99));
        assertEquals(99, PracticeInfo.parseMinutes("ab:cd", 99));
        assertEquals(99, PracticeInfo.parseMinutes(null, 99));
    }
    @Test public void formatMinutes_padsHoursAndMinutes() {
        assertEquals("09:05", PracticeInfo.formatMinutes(545));
        assertEquals("17:45", PracticeInfo.formatMinutes(1065));
    }

    // ---------- PracticeInfo: open-day text ----------

    @Test public void displayDays_weekdays_isRange() {
        assertEquals("Monday - Friday", PracticeInfo.buildDisplayDays(
                Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")));
    }
    @Test public void displayDays_separateDays_areCommaSeparated() {
        assertEquals("Monday, Wednesday",
                PracticeInfo.buildDisplayDays(Arrays.asList("MONDAY", "WEDNESDAY")));
    }
    @Test public void displayDays_twoConsecutiveDays_useComma() {
        assertEquals("Monday, Tuesday",
                PracticeInfo.buildDisplayDays(Arrays.asList("MONDAY", "TUESDAY")));
    }
    @Test public void displayDays_rangeAndSingleDay_areCombined() {
        assertEquals("Monday - Wednesday, Friday", PracticeInfo.buildDisplayDays(
                Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "FRIDAY")));
    }
    @Test public void displayDays_noDays_isClosed() {
        assertEquals("Closed", PracticeInfo.buildDisplayDays(Collections.<String>emptyList()));
    }

    // ---------- PracticeInfo: open days and hours in force ----------

    @Test public void isOpenOn_noSavedDays_defaultsToWeekdays() {
        PracticeInfo info = new PracticeInfo();
        assertTrue(info.isOpenOn(Calendar.MONDAY));
        assertTrue(info.isOpenOn(Calendar.FRIDAY));
        assertFalse(info.isOpenOn(Calendar.SATURDAY));
        assertFalse(info.isOpenOn(Calendar.SUNDAY));
    }
    @Test public void isOpenOn_usesSavedDays() {
        PracticeInfo info = new PracticeInfo();
        info.setOpenDays(Arrays.asList("TUESDAY", "SATURDAY"));
        assertTrue(info.isOpenOn(Calendar.TUESDAY));
        assertTrue(info.isOpenOn(Calendar.SATURDAY));
        assertFalse(info.isOpenOn(Calendar.MONDAY));
    }
    @Test public void operatingHours_missing_defaultTo0800And1800() {
        PracticeInfo info = new PracticeInfo();
        assertEquals(480, info.computeOpeningMinutes());
        assertEquals(1080, info.computeClosingMinutes());
    }
    @Test public void operatingHours_saved_areUsed() {
        PracticeInfo info = new PracticeInfo();
        PracticeInfo.OperatingHours hours = new PracticeInfo.OperatingHours();
        hours.setStart("09:00");
        hours.setEnd("16:30");
        info.setOperatingHours(hours);
        assertEquals(540, info.computeOpeningMinutes());
        assertEquals(990, info.computeClosingMinutes());
    }

    // ---------- StatusUtils ----------

    @Test public void status_missing_isTreatedAsPending() {
        assertEquals("pending", StatusUtils.normalize(null));
        assertEquals("confirmed", StatusUtils.normalize("confirmed"));
    }
    @Test public void status_pendingAndConfirmed_areActive() {
        assertTrue(StatusUtils.isActive("pending"));
        assertTrue(StatusUtils.isActive("confirmed"));
        assertTrue(StatusUtils.isActive(null));
    }
    @Test public void status_cancelledAndCompleted_areNotActive() {
        assertFalse(StatusUtils.isActive("cancelled"));
        assertFalse(StatusUtils.isActive("completed"));
    }
}