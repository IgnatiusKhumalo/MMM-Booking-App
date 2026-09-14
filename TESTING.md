# TESTING.md

## 1. Automated Tests

### Unit Tests — `ValidationUtilsTest.java`

- **Location:** `app/src/test/java/com/mmm/bookingapp/utils/ValidationUtilsTest.java`
- **Run via:** right-click the file in Android Studio → Run, or `./gradlew test`

27 tests covering every rule in `ValidationUtils`:

- **Full name:** valid two/three-word names, single word rejected, null/empty rejected, single-letter second word rejected, extra whitespace tolerated, leading/trailing whitespace tolerated, whitespace-only rejected
- **Email:** contains "@" accepted, missing "@" rejected, null/empty rejected, edge case of bare "@" documented as technically valid per the intentionally simple rule
- **Phone:** exactly 9 digits accepted, 8 or 10 digits rejected, letters rejected, null/empty rejected
- **Password:** meets all rules accepted; missing uppercase, lowercase, number, special character, or minimum length each individually rejected; null rejected
- **Password matching:** identical strings accepted, different strings rejected, null first argument rejected

### Instrumented (Espresso) Test — `WelcomeActivityTest.java`

- **Location:** `app/src/androidTest/java/com/mmm/bookingapp/auth/WelcomeActivityTest.java`
- **Run via:** right-click → Run (requires a connected device/emulator), or `./gradlew connectedAndroidTest`

Confirms the Welcome Screen (true app entry point) renders both Register and Login buttons, visible and correctly labelled.

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
| F7 | Double-booking prevention | Two accounts attempt the same slot | Second attempt rejected | |
| F8 | Public holiday block | Attempt a known SA public holiday date | Booking date rejected with a clear message | |
| F9 | Cancellation | Cancel a pending/confirmed booking | Confirmation dialog shown, status becomes Cancelled | |
| F10 | Admin confirm/decline | Admin confirms a pending booking | Status updates, reflected in client's My Bookings on reload | |
| F11 | Admin search | Type a service name into the search box | List filters to matching bookings only | |
| F12 | Admin search + filters combined | Apply a status filter, then search | Only bookings matching both are shown | |
| F13 | CSV export | Tap Export CSV on Admin Dashboard | Share sheet opens with a valid CSV file | |
| F14 | Practice Information edit | Admin edits phone number, saves | Client's Contact screen reflects the change | |
| F15 | Admin Analytics | Open View Detailed Analytics | Time-bucketed counts and % change display correctly | |
| F16 | Settings — single logout | Open Settings, scroll to Support | Exactly one Logout button present | |

### Usability Testing

| # | Check | Expected Result | Pass/Fail |
|---|---|---|---|
| U1 | Dropdown fields | Rounded border + visible arrow on all Spinners | |
| U2 | Password fields | Eye icon toggles visibility correctly on Login and Register | |
| U3 | Dark Mode | All screens switch palette correctly, remain readable, status badges show bold white text with good contrast on all four status colours | |
| U4 | Scroll indicators | Visible on all screens with content exceeding one view | |
| U5 | Touch target size | Admin Dashboard's Logout and Settings icons comfortably tappable | |
| U6 | Register checkbox text | Both consent sentences fully visible, no clipping, "(optional)" has clear spacing below it | |
| U7 | Settings long labels | "About", "FAQ" etc. wrap to two lines cleanly, never cut off | |

### Edge-Case Testing

| # | Scenario | Expected Result | Pass/Fail |
|---|---|---|---|
| E1 | Submit booking form with fields missing | Specific inline error per missing field | |
| E2 | Attempt booking while offline | Clear offline message, no crash | |
| E3 | Attempt login while offline | Clear offline message, no crash | |
| E4 | Open My Bookings offline, never loaded before | Empty state shown, no crash | |
| E5 | Holiday API unreachable | Booking still works using weekday/hours rules only; toast notes holidays couldn't be checked | |
| E6 | Search with no matching results | Empty state shown, no crash | |
| E7 | Export CSV with zero bookings in filtered list | Export still runs with header row only | |

### Bug Testing and Fixing (real bugs found and resolved during this build)

| # | Bug | Root Cause | Resolution |
|---|---|---|---|
| B1 | Admin Dashboard XML failed to compile after adding `app:tint` | Missing `xmlns:app` namespace on root layout tag | Added the namespace declaration |
| B2 | Logout unreachable on Settings | Buttons existed but sat below the visible scroll area, no scroll indicator | Added a scroll thumb and repositioned logout |
| B3 | Emulator "Unknown error" on run | Emulator process had hung | Cold-booted the emulator |
| B4 | PackageInfo import error | Wrong package path (`android.pm` vs `android.content.pm`) | Corrected the import |
| B5 | Test files failed to resolve JUnit/Espresso imports | Files placed in `src/main/java` instead of `src/test`/`src/androidTest` | Moved to correct source sets |
| B6 | `strings.xml` failed to parse | Stray leftover `</parameter>` tag accidentally included | Removed the invalid tag |
| B7 | Admin login crashed after date-filter redesign | `tvFilterDate` changed from TextView to LinearLayout in XML but Java field type wasn't updated | Changed field type to View |
| B8 | Register screen: consent checkbox text clipped, "Policy" appeared cut off | Checkbox row height/text-wrap not properly configured | Set rows to genuine `wrap_content`, added `maxLines`/`ellipsize="none"`, added explicit bottom margins |
| B9 | Status badges hard to read in light mode | Black text on dark-coloured badge backgrounds (Confirmed/Completed/Cancelled) | Changed badge text to bold white, consistent contrast across all four statuses |

## 3. How Testing Was Performed

All manual tests performed on an Android Studio emulator, with Airplane Mode toggled where noted to simulate offline conditions. Firebase Console used independently to verify that Firestore writes (bookings, cancellations, registrations) matched what the app displayed after each test.
