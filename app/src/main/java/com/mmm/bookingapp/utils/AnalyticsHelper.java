package com.mmm.bookingapp.utils;

import java.util.Calendar;

/**
 * Centralises date-boundary calculations for the Admin Analytics screen,
 * per the Project Structure document's principle that shared logic used
 * by multiple methods belongs in utils/, calculated once correctly rather
 * than duplicated. All boundaries are in device-local time.
 */
public class AnalyticsHelper {

    public static long startOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long startOfYesterday() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startOfToday());
        cal.add(Calendar.DAY_OF_YEAR, -1);
        return cal.getTimeInMillis();
    }

    public static long startOfWeek() {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // If "today" is before Monday in the calendar's own week numbering
        // (shouldn't normally happen once MONDAY is set above, but guarded
        // defensively), roll back one week.
        if (cal.getTimeInMillis() > System.currentTimeMillis()) {
            cal.add(Calendar.WEEK_OF_YEAR, -1);
        }
        return cal.getTimeInMillis();
    }

    public static long startOfWeekOffset(int weeksAgo) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startOfWeek());
        cal.add(Calendar.WEEK_OF_YEAR, -weeksAgo);
        return cal.getTimeInMillis();
    }

    public static long startOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long startOfPreviousMonth() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startOfMonth());
        cal.add(Calendar.MONTH, -1);
        return cal.getTimeInMillis();
    }

    public static long startOfYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_YEAR, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long startOfPreviousYear() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startOfYear());
        cal.add(Calendar.YEAR, -1);
        return cal.getTimeInMillis();
    }

    /**
     * Formats a percentage change from previous to current as a display
     * string (e.g. "+12%", "-8%", "0%"). Handles the zero-baseline case
     * explicitly rather than dividing by zero.
     */
    public static String formatPercentChange(long previous, long current) {
        if (previous == 0) {
            return current == 0 ? "0%" : "New activity";
        }
        double change = ((double) (current - previous) / previous) * 100.0;
        String sign = change >= 0 ? "+" : "";
        return sign + Math.round(change) + "%";
    }
}