# firestore.rules

These are the **actual Firestore Security Rules deployed** to this project's Firebase Console. They are kept in version control here as the single source of truth; the README and TESTING documents refer to them. If a rule is ever changed in the Console, this file must be updated in the same commit.

**To deploy:** Firebase Console → Firestore Database → **Rules** → paste everything inside the code block below → **Publish**.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isSignedIn() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isSignedIn() && request.auth.uid == userId;
    }

    // Admin status is decided on the server by reading the requester's own
    // users/{uid} document. A client can never claim to be an admin from the app.
    function isAdmin() {
      return isSignedIn() &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "admin";
    }

    match /users/{userId} {
      allow read: if isOwner(userId) || isAdmin();

      // A profile can only be created as a client. Admin accounts are made
      // manually in the Firebase Console and never through the app.
      allow create: if isOwner(userId) && request.resource.data.role == "client";

      // A user can update their own profile but can never change their role.
      allow update: if isOwner(userId) &&
        request.resource.data.role == resource.data.role;
      allow update: if isAdmin();
    }

    match /bookings/{bookingId} {
      allow read: if isSignedIn() &&
        (resource.data.clientId == request.auth.uid || isAdmin());

      // A new booking must belong to the creator and start in the honest
      // state: pending and unpaid.
      allow create: if isSignedIn() &&
        request.resource.data.clientId == request.auth.uid &&
        request.resource.data.status == "pending" &&
        request.resource.data.paid == false;

      // Admins can change anything. A client may change ONLY the status field
      // of their own booking, and only to "cancelled" - so a client can never
      // confirm a booking, mark it paid or complete, or change who owns it.
      allow update: if isAdmin() || (
        isSignedIn() &&
        resource.data.clientId == request.auth.uid &&
        request.resource.data.diff(resource.data).affectedKeys().hasOnly(["status"]) &&
        request.resource.data.status == "cancelled"
      );

      // Permanent deletion of a booking record is admin-only. Clients cancel
      // (an update, above), which keeps their booking history.
      allow delete: if isAdmin();
    }

    // One document per 15-minute block of a booking. Holds no personal data
    // except clientId, which exists only to enforce ownership here.
    match /bookedSlots/{slotId} {
      allow read: if isSignedIn();

      // A block can only be created for the requesting client.
      allow create: if isSignedIn() && request.resource.data.clientId == request.auth.uid;

      // Only the owning client or an admin can free a block.
      allow delete: if isAdmin() || resource.data.clientId == request.auth.uid;

      // Blocks are only ever created or deleted, never modified in place.
      allow update: if false;
    }

    match /practiceInfo/{docId} {
      allow read: if true;
      allow write: if isAdmin();
    }
  }
}
```

## What each rule protects against

| Rule | Attack it prevents | How it was verified |
| --- | --- | --- |
| `users` create requires `role == "client"` | A user registering themselves as an admin | Registration works normally (role defaults to client); a write with `role: "admin"` is denied in the Rules Playground |
| `users` update keeps `role` unchanged | A user promoting themselves after registering | Rules Playground |
| `bookings` create requires own `clientId`, `pending`, `paid == false` | Creating a booking for someone else, or one that is already confirmed/paid | Normal bookings still succeed from the app |
| `bookings` update limited to `status → "cancelled"` for clients | A client confirming their own booking, marking it paid/completed, or changing its owner | Rules Playground; client cancellation still works from the app |
| `bookings` delete admin-only | A client permanently deleting booking records | There is no client delete action; admin Delete Booking works |
| `bookedSlots` create/delete tied to `clientId` | A client overwriting or deleting someone else's time reservation | Booking and cancelling still work end to end |
| `bookedSlots` update denied | Tampering with an existing reservation | By design — the app never updates a block |
| `practiceInfo` write admin-only | A client editing the practice's contact details or hours | Admin edits work; client has no edit screen |

## Notes

- Slot blocks created **before** these rules (older test data) have no `clientId`, so a *client* cannot delete them; an admin can. This affects test data only.
- The app also guards the same rules in code (for example it never offers a client a Delete button), but the rules above are what is actually enforced — a modified or malicious client still cannot break them.
