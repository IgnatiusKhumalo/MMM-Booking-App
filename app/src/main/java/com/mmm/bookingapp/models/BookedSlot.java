package com.mmm.bookingapp.models;

/**
 * Mirrors the "bookedSlots" Firestore collection exactly, per the Blueprint's
 * Data Design (Section 4) and the loophole fix in Section 6: this collection
 * holds ONLY scheduling info, never personal client data, so it is safely
 * readable by any authenticated client for conflict-checking purposes
 * without exposing anyone's booking details to another client.
 */
public class BookedSlot {
    private String date;
    private String time;
    private String consultationType;
    private String location;
    private int duration;

    public BookedSlot() {
    }

    public BookedSlot(String date, String time, String consultationType, String location, int duration) {
        this.date = date;
        this.time = time;
        this.consultationType = consultationType;
        this.location = location;
        this.duration = duration;
    }

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