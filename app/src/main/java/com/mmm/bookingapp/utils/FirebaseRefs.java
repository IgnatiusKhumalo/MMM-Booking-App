package com.mmm.bookingapp.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Central access point for Firebase Authentication and every Firestore
 * collection/document used in this app. Collection names are defined
 * ONCE here, matching the Data Design in the Blueprint (Section 4) -
 * no other class should reference a Firestore collection name directly.
 */
public class FirebaseRefs {

    public static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore getFirestore() {
        return FirebaseFirestore.getInstance();
    }

    public static CollectionReference usersCollection() {
        return getFirestore().collection("users");
    }

    public static CollectionReference bookingsCollection() {
        return getFirestore().collection("bookings");
    }

    public static CollectionReference bookedSlotsCollection() {
        return getFirestore().collection("bookedSlots");
    }

    // practiceInfo is a single shared document, not a collection of many -
    // matches the Data Design, which specifies one document (id: "main").
    public static DocumentReference practiceInfoDoc() {
        return getFirestore().collection("practiceInfo").document("main");
    }
}