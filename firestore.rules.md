# firestore.rules

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
    function isAdmin() {
      return isSignedIn() &&
        get(/databases/$(database)/documents/users/$(request.auth.uid)).data.role == "admin";
    }
    match /users/{userId} {
      allow read: if isOwner(userId) || isAdmin();
      allow create: if isOwner(userId);
      allow update: if isOwner(userId) &&
        request.resource.data.role == resource.data.role;
      allow update: if isAdmin();
    }
    match /bookings/{bookingId} {
      allow read: if isSignedIn() &&
        (resource.data.clientId == request.auth.uid || isAdmin());
      allow create: if isSignedIn() &&
        request.resource.data.clientId == request.auth.uid;
      allow update: if isSignedIn() &&
        (resource.data.clientId == request.auth.uid || isAdmin());
    }
    match /bookedSlots/{slotId} {
      allow read: if isSignedIn();
      allow write: if isSignedIn();
    }
    match /practiceInfo/{docId} {
      allow read: if true;
      allow write: if isAdmin();
    }
  }
}
```
