package com.mmm.bookingapp.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors the "bookedSlots" Firestore collection - an anonymous,
 * conflict-checking-only record per the Blueprint's Data Design. Kept
 * free of personal data (no name, phone, service or notes) so any
 * signed-in user can read it to check availability.
 *
 * A booking reserves one slot document per 15-minute block it covers
 * (a 60-minute booking reserves 4). Two bookings overlap exactly when
 * they want the same block, so a transaction that reads every block
 * first and fails if any exists prevents overlaps of any kind.
 *
 * clientId exists purely for server-side ownership enforcement (the
 * Security Rules let only the owning client, or an admin, create or
 * delete a slot). It is never displayed.
 */
public class BookedSlot {

    /** Length of one reservation block. All booking durations are multiples of this. */
    public static final int BLOCK_MINUTES = 15;

    private String slotId;
    private String clientId;
    private String date;
    private String time;
    private String consultationType;
    private String location;
    private int duration;

    public BookedSlot() {
    }

    public BookedSlot(String clientId, String date, String time, String consultationType,
                      String location, int duration) {
        this.clientId = clientId;
        this.date = date;
        this.time = time;
        this.consultationType = consultationType;
        this.location = location;
        this.duration = duration;
    }

    /** Slot document ID for one block: date_time_location, or "online" when location is null. */
    public static String buildId(String date, String time, String location) {
        return date + "_" + time + "_" + (location != null ? location : "online");
    }

    /** The start time ("HH:mm") of every 15-minute block covered by an appointment. */
    public static List<String> buildBlockTimes(String startTime, int durationMinutes) {
        List<String> times = new ArrayList<>();
        int start = PracticeInfo.parseMinutes(startTime, -1);
        if (start < 0) {
            times.add(startTime);
            return times;
        }
        int blocks = (durationMinutes + BLOCK_MINUTES - 1) / BLOCK_MINUTES;
        for (int i = 0; i < blocks; i++) {
            times.add(PracticeInfo.formatMinutes(start + i * BLOCK_MINUTES));
        }
        return times;
    }

    /** The slot document ID of every block covered by an appointment. */
    public static List<String> buildBlockIds(String date, String startTime, int durationMinutes, String location) {
        List<String> ids = new ArrayList<>();
        for (String blockTime : buildBlockTimes(startTime, durationMinutes)) {
            ids.add(buildId(date, blockTime, location));
        }
        return ids;
    }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getConsultationType() { return consultationType; }
    public void setConsultationType(String consultationType) { this.consultationType = consultationType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
}