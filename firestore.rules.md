# firestore.rules.md

rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isSignedIn() {
      return request.auth != null;
    }
    function isOwner(userId) {
      return isSignedIn() && request.auth.uid == userId;
    }
    function isAdmin() {
      return isSignedIn() &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "admin";
    }

    match /users/{userId} {
      allow read: if isOwner(userId) || isAdmin();

      // FIXED: previously only checked isOwner(userId), meaning a client
      // could create their own user document with role: "admin" and it
      // would be accepted. Now the role is forced to "client" at creation
      // time, server-side - admin accounts can ONLY be provisioned
      // manually via the Firebase Console, exactly as documented in the
      // Blueprint and never through any path in the app itself.
      allow create: if isOwner(userId) && request.resource.data.role == "client";

      allow update: if isOwner(userId) &&
        request.resource.data.role == resource.data.role;
      allow update: if isAdmin();
    }

    match /bookings/{bookingId} {
      allow read: if isSignedIn() &&
        (resource.data.clientId == request.auth.uid || isAdmin());

      // FIXED: now explicitly requires a new booking to belong to the
      // creating user and to start in the correct, honest state
      // (pending, unpaid) - a client can never create a booking that is
      // already confirmed or already marked paid.
      allow create: if isSignedIn() &&
        request.resource.data.clientId == request.auth.uid &&
        request.resource.data.status == "pending" &&
        request.resource.data.paid == false;

      // FIXED: previously a client could update ANY field on their own
      // booking (status, paid, even clientId), since the rule only
      // checked ownership. Now a client may ONLY change the status
      // field, and only to "cancelled" - every other field
      // (status to confirmed/completed, paid, clientId, service, date,
      // time, etc.) is locked to the client and can only be changed by
      // an admin. This matches the Blueprint's role-based access design:
      // confirm/decline/mark-paid/mark-completed are documented admin-only
      // actions.
      allow update: if isAdmin() || (
        isSignedIn() &&
        resource.data.clientId == request.auth.uid &&
        request.resource.data.diff(resource.data).affectedKeys().hasOnly(["status"]) &&
        request.resource.data.status == "cancelled"
      );

      // NEW: a true delete operation, admin-only. Clients cancel (an
      // update, above) rather than delete, so their own booking history
      // in My Bookings is preserved; an admin can permanently remove a
      // booking record via the new Delete Booking action.
      allow delete: if isAdmin();
    }

    match /bookedSlots/{slotId} {
      allow read: if isSignedIn();

      // FIXED: previously any signed-in user could write ANY slot
      // document, meaning a client could overwrite or claim a slot that
      // belonged to someone else. Now a slot can only be created by the
      // client it is being created for (clientId must match the
      // requester), matching the ownership model already used on
      // bookings.
      allow create: if isSignedIn() && request.resource.data.clientId == request.auth.uid;

      // FIXED: previously any signed-in user could delete ANY slot
      // document (e.g. another client's in-progress reservation). Now
      // only the owning client or an admin can free a slot - matches
      // exactly who is allowed to cancel/decline the booking that slot
      // belongs to.
      allow delete: if isAdmin() || resource.data.clientId == request.auth.uid;

      // NEW: slots are only ever created or deleted in this app, never
      // modified in place - explicitly denying update closes off a
      // tampering path that was never a legitimate use case to begin
      // with.
      allow update: if false;
    }

    match /practiceInfo/{docId} {
      allow read: if true;
      allow write: if isAdmin();
    }
  }
}
