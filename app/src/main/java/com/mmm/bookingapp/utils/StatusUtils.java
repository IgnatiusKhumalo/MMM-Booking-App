package com.mmm.bookingapp.utils;

import com.mmm.bookingapp.R;

/**
 * Single place that interprets a booking's status string, so every
 * screen treats it the same way. A booking with a missing (null) status
 * is treated as "pending" instead of crashing the screen that reads it.
 */
public class StatusUtils {

    /** The status to use for logic: "pending" when none is stored. */
    public static String normalize(String status) {
        return status == null ? "pending" : status;
    }

    /** True for bookings that still hold their time slot (pending or confirmed). */
    public static boolean isActive(String status) {
        String s = normalize(status);
        return "pending".equals(s) || "confirmed".equals(s);
    }

    /** Colour resource for the status badge. */
    public static int badgeColorRes(String status) {
        switch (normalize(status)) {
            case "confirmed": return R.color.color_status_confirmed;
            case "cancelled": return R.color.color_error;
            case "completed": return R.color.color_status_completed;
            default: return R.color.color_status_pending;
        }
    }

    /** Text resource for the status badge. */
    public static int badgeLabelRes(String status) {
        switch (normalize(status)) {
            case "confirmed": return R.string.status_confirmed;
            case "cancelled": return R.string.status_cancelled;
            case "completed": return R.string.status_completed;
            default: return R.string.status_pending;
        }
    }
}