package com.mmm.bookingapp.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Unit tests for the free-times logic (8 tests). Window 480-600 is 08:00-10:00. */
public class AvailabilityUtilsTest {

    private static final Set<String> NONE = Collections.emptySet();
    private static final List<int[]> NO_BREAKS = new ArrayList<>();

    @Test public void emptyDay_thirtyMinutes_givesEveryQuarterHourThatFits() {
        assertEquals(Arrays.asList("08:00", "08:15", "08:30", "08:45", "09:00", "09:15", "09:30"),
                AvailabilityUtils.freeStartTimes(480, 600, 30, NONE, NO_BREAKS, 0));
    }

    @Test public void longerDuration_givesFewerStarts() {
        assertEquals(Arrays.asList("08:00", "08:15", "08:30", "08:45", "09:00"),
                AvailabilityUtils.freeStartTimes(480, 600, 60, NONE, NO_BREAKS, 0));
    }

    @Test public void takenBlocks_removeEveryStartThatTouchesThem() {
        Set<String> taken = new HashSet<>(Arrays.asList("08:15", "08:30"));
        assertEquals(Arrays.asList("08:45", "09:00", "09:15", "09:30"),
                AvailabilityUtils.freeStartTimes(480, 600, 30, taken, NO_BREAKS, 0));
    }

    @Test public void break_removesStartsThatOverlapIt_butAllowsTouching() {
        List<int[]> breaks = new ArrayList<>();
        breaks.add(new int[]{600, 660}); // 10:00 - 11:00
        assertEquals(Arrays.asList("08:00", "08:15", "08:30", "08:45", "09:00", "11:00"),
                AvailabilityUtils.freeStartTimes(480, 720, 60, NONE, breaks, 0));
    }

    @Test public void today_earliestStartIsRoundedUpToTheNextQuarterHour() {
        assertEquals(Arrays.asList("08:45", "09:00", "09:15", "09:30"),
                AvailabilityUtils.freeStartTimes(480, 600, 30, NONE, NO_BREAKS, 520)); // 08:40
    }

    @Test public void nothingFits_returnsEmptyList() {
        assertTrue(AvailabilityUtils.freeStartTimes(480, 525, 60, NONE, NO_BREAKS, 0).isEmpty());
    }

    @Test public void appointmentMustEndByClosingTime() {
        assertEquals(Collections.singletonList("08:00"),
                AvailabilityUtils.freeStartTimes(480, 540, 60, NONE, NO_BREAKS, 0));
    }

    @Test public void overlapsBreak_backToBackIsAllowed_realOverlapIsNot() {
        List<int[]> breaks = new ArrayList<>();
        breaks.add(new int[]{600, 660});
        assertFalse(AvailabilityUtils.overlapsBreak(540, 600, breaks));
        assertTrue(AvailabilityUtils.overlapsBreak(570, 630, breaks));
    }
}