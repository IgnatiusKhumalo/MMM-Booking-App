package com.mmm.bookingapp.utils;

import java.util.Calendar;

/**
 * The practice's cancellation notice rule: a client may cancel in the app
 * free of charge up to NOTICE_HOURS (24) before the appointment starts.
 * Inside that window - or once the appointment has started - cancelling in
 * the app is closed and the client is asked to contact the practice, who
 * may charge a late-cancellation fee under the Terms and Conditions.
 *
 * Plain Java with no Android dependencies, so the rule is covered by unit
 * tests (CancellationPolicyTest). The rule is applied in the app; an admin
 * can still decline any booking.
 */
public class CancellationPolicy {

    public static final int NOTICE_HOURS = 24;
    private static final long NOTICE_MILLIS = NOTICE_HOURS * 60L * 60L * 1000L;

    /**
     * Start of the appointment in epoch milliseconds (device time zone), or
     * -1 if the date ("yyyy-MM-dd") or time ("HH:mm") cannot be read.
     */
    public static long appointmentStartMillis(String date, String time) {
        if (date == null || time == null) return -1;
        String[] d = date.trim().split("-");
        String[] t = time.trim().split(":");
        if (d.length != 3 || t.length != 2) return -1;
        try {
            int year = Integer.parseInt(d[0]);
            int month = Integer.parseInt(d[1]);
            int day = Integer.parseInt(d[2]);
            int hour = Integer.parseInt(t[0]);
            int minute = Integer.parseInt(t[1]);
            Calendar calendar = Calendar.getInstance();
            calendar.clear();
            calendar.set(year, month - 1, day, hour, minute, 0);
            return calendar.getTimeInMillis();
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * True when less than NOTICE_HOURS remain before the appointment (or it
     * has already started), meaning in-app cancellation is closed. An
     * unreadable date/time never blocks the client (returns false).
     */
    public static boolean isWithinNoticePeriod(String date, String time, long nowMillis) {
        long start = appointmentStartMillis(date, time);
        if (start < 0) return false;
        return nowMillis > start - NOTICE_MILLIS;
    }

    /** Same check against the current time. */
    public static boolean isWithinNoticePeriod(String date, String time) {
        return isWithinNoticePeriod(date, time, System.currentTimeMillis());
    }
}