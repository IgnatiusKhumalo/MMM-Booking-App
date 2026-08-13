package com.mmm.bookingapp.models;

/**
 * Mirrors the "users" Firestore collection exactly, per the Blueprint's
 * Data Design (Section 4). The "role" field is write-protected at the
 * Firestore Security Rules level (see Section 5) - this class does not
 * enforce that itself, the server-side rules do.
 *
 * marketingOptIn: tracks the OPTIONAL subscribe checkbox on Register.
 * createdAt (NEW): epoch milliseconds at registration, used by the Admin
 * Analytics screen's date-bucketed registration counts. Accounts created
 * before this field existed will not have it, and are correctly excluded
 * from date-bucketed counts by Firestore's own inequality-query behaviour.
 */
public class User {
    private String uid;
    private String name;
    private String email;
    private String phone;
    private String role;               // "client" or "admin"
    private boolean emailVerified;
    private boolean notificationsEnabled;
    private boolean marketingOptIn;
    private long createdAt;

    // Required no-argument constructor for Firestore's automatic deserialization.
    public User() {
    }

    public User(String uid, String name, String email, String phone, boolean marketingOptIn) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = "client";           // always defaults to client - matches
        // the Blueprint's "no exceptions" rule
        this.emailVerified = false;
        this.notificationsEnabled = true;
        this.marketingOptIn = marketingOptIn;
        this.createdAt = System.currentTimeMillis();
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public boolean isMarketingOptIn() { return marketingOptIn; }
    public void setMarketingOptIn(boolean marketingOptIn) { this.marketingOptIn = marketingOptIn; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}