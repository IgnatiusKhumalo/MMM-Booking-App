package com.mmm.bookingapp.utils;

import com.mmm.bookingapp.models.BookedSlot;
import com.mmm.bookingapp.models.PracticeInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Works out which appointment start times are free, so the booking screen
 * can show clients only times they can actually book instead of letting
 * them discover a clash after choosing.
 *
 * A start time (on the quarter hour) is free when the whole appointment
 *  - begins no earlier than opening time (and not in the past, for today),
 *  - ends no later than closing time,
 *  - does not overlap a break, and
 *  - does not touch any 15-minute block that is already reserved.
 *
 * Plain Java with no Android dependencies, so the rules are covered by
 * unit tests (AvailabilityUtilsTest). The booking transaction remains the
 * final safeguard if two people choose the same time at the same moment.
 */
public class AvailabilityUtils {

    /**
     * @param openingMinutes     opening time, minutes after midnight
     * @param closingMinutes     closing time, minutes after midnight
     * @param durationMinutes    length of the appointment
     * @param takenBlockTimes    start times ("HH:mm") of 15-minute blocks already reserved
     *                           for this date and location
     * @param breaks             [startMinutes, endMinutes] pairs the practice is on a break
     * @param earliestStartMinutes earliest allowed start (use 0 for a future date, or the
     *                           current time for today)
     * @return free start times as "HH:mm", in order
     */
    public static List<String> freeStartTimes(int openingMinutes, int closingMinutes, int durationMinutes,
                                              Set<String> takenBlockTimes, List<int[]> breaks,
                                              int earliestStartMinutes) {
        List<String> free = new ArrayList<>();
        int block = BookedSlot.BLOCK_MINUTES;

        int first = Math.max(openingMinutes, earliestStartMinutes);
        first = ((first + block - 1) / block) * block; // round up to the next quarter hour

        for (int start = first; start + durationMinutes <= closingMinutes; start += block) {
            int end = start + durationMinutes;
            if (overlapsBreak(start, end, breaks)) continue;
            if (touchesTakenBlock(start, end, takenBlockTimes)) continue;
            free.add(PracticeInfo.formatMinutes(start));
        }
        return free;
    }

    /** True if [start, end) overlaps any break. Touching end-to-start is not an overlap. */
    public static boolean overlapsBreak(int start, int end, List<int[]> breaks) {
        if (breaks == null) return false;
        for (int[] b : breaks) {
            if (start < b[1] && b[0] < end) return true;
        }
        return false;
    }

    private static boolean touchesTakenBlock(int start, int end, Set<String> takenBlockTimes) {
        if (takenBlockTimes == null || takenBlockTimes.isEmpty()) return false;
        for (int t = start; t < end; t += BookedSlot.BLOCK_MINUTES) {
            if (takenBlockTimes.contains(PracticeInfo.formatMinutes(t))) return true;
        }
        return false;
    }
}