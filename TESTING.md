# MMM Booking App — Testing Documentation

## 1. Automated Tests

### Unit Tests — `ValidationUtilsTest.java`
**Location:** `app/src/test/java/com/mmm/bookingapp/utils/ValidationUtilsTest.java`
**Run via:** Android Studio → right-click the file → Run, or `./gradlew test`

27 tests covering every rule in `ValidationUtils`: full name (word count, word length, whitespace handling), email (presence of "@"), phone (exact 9-digit rule), password (all 4 composition rules + length), and password matching — including deliberate edge cases (null inputs, empty strings, boundary lengths).

### Instrumented (Espresso) Test — `WelcomeActivityTest.java`
**Location:** `app/src/androidTest/java/com/mmm/bookingapp/auth/WelcomeActivityTest.java`
**Run via:** Android Studio → right-click the file → Run (requires a connected device or running emulator), or `./gradlew connectedAndroidTest`

Confirms the Welcome Screen (the app's true entry point) renders both navigation buttons correctly.

## 2. Manual Test Plan

### Functional Testing

| # | Feature | Steps | Expected Result | Pass/Fail |
|---|---|---|---|---|
| F1 | Registration | Fill all fields correctly, tick consent, submit | Account created, verification email sent | |
| F2 | Registration blocked without consent | Fill all fields, leave consent unticked, submit | Red error shown, no account created | |
| F3 | Email verification gate | Log in before verifying email | Blocked, resend option shown | |
| F4 | Login (verified account) | Log in with correct credentials | Routed to Home (client) or Admin Dashboard (admin) | |
| F5 | Password reset | Request reset for a registered email | Reset email received | |
| F6 | Booking submission | Complete and submit a valid booking | Confirmation screen shown, booking appears in My Bookings | |
| F7 | Double-booking prevention | Two accounts attempt the same slot | Second attempt is rejected | |
| F8 | Public holiday block (NEW) | Attempt to pick a known SA public holiday date | Booking date rejected with a clear message | |
| F9 | Cancellation | Cancel a pending/confirmed booking | Confirmation dialog shown, status becomes Cancelled | |
| F10 | Admin confirm/decline | Admin confirms a pending booking | Status updates, reflected in client's My Bookings on reload | |
| F11 | Admin search (NEW) | Type a service name into the search box | List filters to matching bookings only | |
| F12 | Admin search + filters combined (NEW) | Apply a status filter, then search | Only bookings matching both are shown | |
| F13 | CSV export (NEW) | Tap Export to CSV on Admin Dashboard | Share sheet opens with a valid CSV file | |
| F14 | Practice Information edit | Admin edits phone number, saves | Client's Contact screen reflects the change | |

### Usability Testing

| # | Check | Expected Result | Pass/Fail |
|---|---|---|---|
| U1 | Dropdown fields are visually identifiable as tappable | Rounded border + visible arrow on all Spinners | |
| U2 | Password fields | Eye icon toggles visibility correctly on Login and Register | |
| U3 | Dark Mode | All screens switch palette correctly and remain readable | |
| U4 | Scroll indicators | Visible on all screens with content exceeding one view | |
| U5 | Touch target size (NEW) | Admin Dashboard's Logout and Settings controls are comfortably tappable, not just their small visual size | |

### Edge-Case Testing

| # | Scenario | Expected Result | Pass/Fail |
|---|---|---|---|
| E1 | Submit booking form with no fields selected | Specific inline error per missing field | |
| E2 | Attempt booking while offline | Clear offline message, no crash | |
| E3 | Attempt login while offline | Clear offline message, no crash | |
| E4 | Open My Bookings offline, never loaded before | Empty state shown, no crash | |
| E5 | Holiday API unreachable (NEW) | Booking still works using weekday/hours rules only; toast notes holidays couldn't be checked | |
| E6 | Search with no matching results (NEW) | Empty state shown, no crash | |
| E7 | Export CSV with zero bookings in filtered list (NEW) | Logged as a warning; export still runs with header row only | |

### Bug Testing and Fixing

| # | Bug Found | Root Cause | Resolution |
|---|---|---|---|
| B1 | Admin Dashboard XML failed to compile after adding `app:tint` | Missing `xmlns:app` namespace declaration on root layout tag | Added the namespace declaration |
| B2 | Logout unreachable on Settings screen | Logout buttons existed but were below the visible scroll area with no scroll indicator | Added a top-of-screen Logout button and a visible scroll thumb |
| B3 | Emulator "Unknown error" on run | Emulator process had hung, unrelated to app code | Cold-booted the emulator |
| B4 | `PackageInfo` import error | Wrong package path (`android.pm` instead of `android.content.pm`) | Corrected the import |

## 3. How Testing Was Performed

All manual tests were performed on an Android Studio emulator (Pixel 6 profile) and, where noted, with Airplane Mode toggled to simulate offline conditions. Firebase Console was used to independently verify that Firestore writes (bookings, cancellations, registrations) matched what the app displayed after each test.
