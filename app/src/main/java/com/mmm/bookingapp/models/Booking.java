package com.mmm.bookingapp.models;

/**
 * Mirrors the "bookings" Firestore collection exactly, per the Blueprint's
 * Data Design (Section 4). location is null when consultationType is
 * "online" - per the Book Appointment screen's conditional dropdown logic.
 */
public class Booking {
    private String bookingId;
    private String clientId;
    private String service;
    private String consultationType;   // "online" | "in-person"
    private String location;           // null if online
    private int duration;              // 30 | 45 | 60
    private String date;               // "yyyy-MM-dd"
    private String time;               // "HH:mm"
    private String additionalInfo;
    private String status;             // "pending" | "confirmed" | "cancelled" | "completed"
    private boolean paid;
    private long createdAt;

    public Booking() {
    }

    public Booking(String clientId, String service, String consultationType, String location,
                   int duration, String date, String time, String additionalInfo) {
        this.clientId = clientId;
        this.service = service;
        this.consultationType = consultationType;
        this.location = location;
        this.duration = duration;
        this.date = date;
        this.time = time;
        this.additionalInfo = additionalInfo;
        this.status = "pending";        // always defaults to pending, per the Blueprint
        this.paid = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getConsultationType() { return consultationType; }
    public void setConsultationType(String consultationType) { this.consultationType = consultationType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}