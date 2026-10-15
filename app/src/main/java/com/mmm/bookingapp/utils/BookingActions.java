package com.mmm.bookingapp.utils;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.mmm.bookingapp.models.BookedSlot;

import java.util.ArrayList;
import java.util.List;

/**
 * The single shared place where a booking is cancelled or permanently
 * deleted, used by My Bookings (client cancel) and Booking Detail (admin
 * decline and admin delete).
 *
 * Both operations run as ONE Firestore transaction, so the booking
 * change and the release of its slot blocks succeed together or fail
 * together - a failed slot delete can never leave a booking cancelled
 * while its time stays blocked.
 *
 * Slot blocks are released only when the booking is still active
 * (pending or confirmed) AND the block is owned by this booking's
 * client. This stops deleting an old cancelled booking from freeing a
 * block that has since been re-booked. Blocks created before the
 * clientId field existed (clientId == null) are treated as owned by the
 * booking so they can still be freed.
 */
public class BookingActions {

    public static final int REASON_OTHER = 0;
    public static final int REASON_NOT_CANCELLABLE = 1;
    public static final int REASON_MISSING = 2;

    private static final String MSG_NOT_CANCELLABLE = "NOT_CANCELLABLE";
    private static final String MSG_MISSING = "BOOKING_MISSING";

    public interface Callback {
        void onSuccess();
        void onError(int reason, String message);
    }

    /** Sets status to "cancelled" and frees the booking's slot blocks, atomically. */
    public static void cancelBooking(String bookingId, Callback callback) {
        run(bookingId, false, callback);
    }

    /** Permanently deletes the booking document (and frees its slot blocks if it still held them), atomically. */
    public static void deleteBooking(String bookingId, Callback callback) {
        run(bookingId, true, callback);
    }

    private static void run(String bookingId, boolean deletePermanently, Callback callback) {
        DocumentReference bookingRef = FirebaseRefs.bookingsCollection().document(bookingId);

        FirebaseRefs.getFirestore().runTransaction(transaction -> {
                    // Firestore transactions require every read to happen before any write.
                    DocumentSnapshot bookingSnap = transaction.get(bookingRef);
                    if (!bookingSnap.exists()) {
                        throw new FirebaseFirestoreException(MSG_MISSING,
                                FirebaseFirestoreException.Code.NOT_FOUND);
                    }

                    String status = bookingSnap.getString("status");
                    boolean holdsSlots = "pending".equals(status) || "confirmed".equals(status);

                    if (!deletePermanently && !holdsSlots) {
                        throw new FirebaseFirestoreException(MSG_NOT_CANCELLABLE,
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                    }

                    List<DocumentReference> slotRefs = new ArrayList<>();
                    List<DocumentSnapshot> slotSnaps = new ArrayList<>();
                    if (holdsSlots) {
                        Long durationValue = bookingSnap.getLong("duration");
                        int duration = durationValue != null ? durationValue.intValue() : BookedSlot.BLOCK_MINUTES;
                        List<String> blockIds = BookedSlot.buildBlockIds(
                                bookingSnap.getString("date"),
                                bookingSnap.getString("time"),
                                duration,
                                bookingSnap.getString("location"));
                        for (String blockId : blockIds) {
                            DocumentReference ref = FirebaseRefs.bookedSlotsCollection().document(blockId);
                            slotRefs.add(ref);
                            slotSnaps.add(transaction.get(ref));
                        }
                    }

                    if (deletePermanently) {
                        transaction.delete(bookingRef);
                    } else {
                        transaction.update(bookingRef, "status", "cancelled");
                    }

                    String bookingOwner = bookingSnap.getString("clientId");
                    for (int i = 0; i < slotRefs.size(); i++) {
                        DocumentSnapshot slotSnap = slotSnaps.get(i);
                        if (!slotSnap.exists()) continue;
                        String slotOwner = slotSnap.getString("clientId");
                        if (slotOwner == null || slotOwner.equals(bookingOwner)) {
                            transaction.delete(slotRefs.get(i));
                        }
                    }
                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    String message = e.getMessage() == null ? "" : e.getMessage();
                    if (message.contains(MSG_NOT_CANCELLABLE)) {
                        callback.onError(REASON_NOT_CANCELLABLE, message);
                    } else if (message.contains(MSG_MISSING)) {
                        callback.onError(REASON_MISSING, message);
                    } else {
                        callback.onError(REASON_OTHER, message);
                    }
                });
    }
}