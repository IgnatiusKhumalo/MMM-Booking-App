package com.mmm.bookingapp.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Mirrors the single "practiceInfo/main" Firestore document, per the
 * Blueprint's Data Design. Read by every client-facing screen that
 * displays or enforces practice information.
 *
 * Open days are stored two ways on purpose:
 *  - openDays: structured list of day keys (e.g. "MONDAY"). This is what
 *    the booking screen actually checks.
 *  - operatingDays: a human-readable text (e.g. "Monday - Friday"),
 *    generated from openDays when the admin saves. This is what the
 *    Contact screen displays.
 */
public class PracticeInfo {

    public static final String[] DAY_KEYS = {
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
    private static final String[] DAY_NAMES = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

    private static final int DEFAULT_OPEN_MINUTES = 8 * 60;
    private static final int DEFAULT_CLOSE_MINUTES = 18 * 60;

    private String phone;
    private String email;
    private String whatsappLink;
    private List<String> locations;
    private String operatingDays;
    private List<String> openDays;
    private OperatingHours operatingHours;
    private String crisisLineNote;

    // Required no-argument constructor for Firestore's automatic deserialization.
    public PracticeInfo() {
    }

    public static class OperatingHours {
        private String start;
        private String end;

        public OperatingHours() {
        }

        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }

        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWhatsappLink() { return whatsappLink; }
    public void setWhatsappLink(String whatsappLink) { this.whatsappLink = whatsappLink; }

    public List<String> getLocations() { return locations; }
    public void setLocations(List<String> locations) { this.locations = locations; }

    public String getOperatingDays() { return operatingDays; }
    public void setOperatingDays(String operatingDays) { this.operatingDays = operatingDays; }

    public List<String> getOpenDays() { return openDays; }
    public void setOpenDays(List<String> openDays) { this.openDays = openDays; }

    public OperatingHours getOperatingHours() { return operatingHours; }
    public void setOperatingHours(OperatingHours operatingHours) { this.operatingHours = operatingHours; }

    public String getCrisisLineNote() { return crisisLineNote; }
    public void setCrisisLineNote(String crisisLineNote) { this.crisisLineNote = crisisLineNote; }

    // ---------- Booking rules derived from the saved settings ----------

    /** The open days actually in force: the saved list, or Monday-Friday if none is saved yet. */
    public List<String> effectiveOpenDays() {
        if (openDays != null && !openDays.isEmpty()) return openDays;
        return Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
    }

    /** @param calendarDayOfWeek a java.util.Calendar.DAY_OF_WEEK value */
    public boolean isOpenOn(int calendarDayOfWeek) {
        return effectiveOpenDays().contains(dayKeyFor(calendarDayOfWeek));
    }

    /** Human-readable open days, e.g. "Monday - Friday". */
    public String describeOpenDays() {
        return buildDisplayDays(effectiveOpenDays());
    }

    /** Opening time in minutes after midnight; 08:00 if missing or invalid. */
    public int computeOpeningMinutes() {
        return parseMinutes(operatingHours != null ? operatingHours.getStart() : null, DEFAULT_OPEN_MINUTES);
    }

    /** Closing time in minutes after midnight; 18:00 if missing or invalid. */
    public int computeClosingMinutes() {
        return parseMinutes(operatingHours != null ? operatingHours.getEnd() : null, DEFAULT_CLOSE_MINUTES);
    }

    // ---------- Static helpers (no Android dependencies, unit-testable) ----------

    public static String dayKeyFor(int calendarDayOfWeek) {
        switch (calendarDayOfWeek) {
            case Calendar.MONDAY: return "MONDAY";
            case Calendar.TUESDAY: return "TUESDAY";
            case Calendar.WEDNESDAY: return "WEDNESDAY";
            case Calendar.THURSDAY: return "THURSDAY";
            case Calendar.FRIDAY: return "FRIDAY";
            case Calendar.SATURDAY: return "SATURDAY";
            default: return "SUNDAY";
        }
    }

    /** Parses "HH:mm" into minutes after midnight; returns fallback if null or invalid. */
    public static int parseMinutes(String hhmm, int fallback) {
        if (hhmm == null) return fallback;
        String[] parts = hhmm.trim().split(":");
        if (parts.length != 2) return fallback;
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) return fallback;
            return hours * 60 + minutes;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static String formatMinutes(int minutesAfterMidnight) {
        return String.format(Locale.US, "%02d:%02d", minutesAfterMidnight / 60, minutesAfterMidnight % 60);
    }

    /**
     * Turns day keys into display text: consecutive days become a range
     * ("Monday - Friday"), separate groups are comma-separated.
     */
    public static String buildDisplayDays(List<String> dayKeys) {
        boolean[] selected = new boolean[7];
        boolean any = false;
        for (String key : dayKeys) {
            for (int i = 0; i < DAY_KEYS.length; i++) {
                if (DAY_KEYS[i].equals(key)) {
                    selected[i] = true;
                    any = true;
                }
            }
        }
        if (!any) return "Closed";

        List<String> parts = new ArrayList<>();
        int i = 0;
        while (i < 7) {
            if (!selected[i]) {
                i++;
                continue;
            }
            int j = i;
            while (j + 1 < 7 && selected[j + 1]) j++;
            if (j == i) {
                parts.add(DAY_NAMES[i]);
            } else if (j == i + 1) {
                parts.add(DAY_NAMES[i] + ", " + DAY_NAMES[j]);
            } else {
                parts.add(DAY_NAMES[i] + " - " + DAY_NAMES[j]);
            }
            i = j + 1;
        }
        return String.join(", ", parts);
    }

    /**
     * Safe hardcoded fallback, used only if the practiceInfo document is
     * missing or fails to load. In normal operation this is never used,
     * since the real document already exists.
     */
    public static PracticeInfo getFallback() {
        PracticeInfo fallback = new PracticeInfo();
        fallback.setPhone("072 319 1836");
        fallback.setEmail("info@mmmsocialworkersandassociates.co.za");
        fallback.setWhatsappLink("https://wa.me/27723191836");
        fallback.setOperatingDays("Monday - Friday");
        fallback.setOpenDays(Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"));
        OperatingHours hours = new OperatingHours();
        hours.setStart("08:00");
        hours.setEnd("18:00");
        fallback.setOperatingHours(hours);
        fallback.setCrisisLineNote("In a crisis outside these hours, please contact SADAG on 0800 567 567.");
        return fallback;
    }
}