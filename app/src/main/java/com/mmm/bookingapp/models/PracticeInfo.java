package com.mmm.bookingapp.models;

import java.util.List;

/**
 * Mirrors the single "practiceInfo/main" Firestore document exactly,
 * per the Blueprint's Data Design (Section 4). This is read by every
 * client-facing screen that displays or enforces practice information
 * (Contact screen now; Book Appointment screen's date/time rules later).
 */
public class PracticeInfo {
    private String phone;
    private String email;
    private String whatsappLink;
    private List<String> locations;
    private String operatingDays;
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

    public OperatingHours getOperatingHours() { return operatingHours; }
    public void setOperatingHours(OperatingHours operatingHours) { this.operatingHours = operatingHours; }

    public String getCrisisLineNote() { return crisisLineNote; }
    public void setCrisisLineNote(String crisisLineNote) { this.crisisLineNote = crisisLineNote; }

    /**
     * Safe hardcoded fallback, used only if the practiceInfo document is
     * ever missing or fails to load - per the Blueprint's documented gap
     * fix ("safe fallback defaults if not yet populated"). In normal
     * operation this is never used, since the real document already exists.
     */
    public static PracticeInfo getFallback() {
        PracticeInfo fallback = new PracticeInfo();
        fallback.setPhone("072 319 1836");
        fallback.setEmail("info@mmmsocialworkersandassociates.co.za");
        fallback.setWhatsappLink("https://wa.me/27723191836");
        fallback.setOperatingDays("Monday - Friday");
        OperatingHours hours = new OperatingHours();
        hours.setStart("08:00");
        hours.setEnd("18:00");
        fallback.setOperatingHours(hours);
        fallback.setCrisisLineNote("In a crisis outside these hours, please contact SADAG on 0800 567 567.");
        return fallback;
    }
}