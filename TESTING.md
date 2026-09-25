# 🧪 Testing Documentation

This document covers the automated test suite and the full manual test plan used to validate the booking app, including functional, usability, edge-case, and bug-fix records.

---

## Table of Contents

- [1. Automated Tests](#1-automated-tests)
  - [Unit Tests — ValidationUtilsTest.java](#unit-tests--validationutilstestjava)
  - [Instrumented (Espresso) Test — WelcomeActivityTest.java](#instrumented-espresso-test--welcomeactivitytestjava)
- [2. Manual Test Plan](#2-manual-test-plan)
  - [Functional Testing](#functional-testing)
  - [Usability Testing](#usability-testing)
  - [Edge-Case Testing](#edge-case-testing)
  - [Bug Testing and Fixing](#bug-testing-and-fixing)
- [3. How Testing Was Performed](#3-how-testing-was-performed)

---

## 1. Automated Tests

### Unit Tests — `ValidationUtilsTest.java`

| | |
|---|---|
| **Location** | `app/src/test/java/com/mmm/bookingapp/utils/ValidationUtilsTest.java` |
| **Run via** | Right-click the file in Android Studio → **Run**, or `./gradlew test` |
| **Coverage** | 30 tests |

Covers every rule in `ValidationUtils`:

- **Full name** — valid two/three-word names; single word rejected; null/empty rejected; single-letter second word rejected; extra whitespace tolerated; leading/trailing whitespace tolerated; whitespace-only rejected
- **Email** — contains `"@"` accepted; missing `"@"` rejected; null/empty rejected; bare `"@"` documented as technically valid per the intentionally simple rule
- **Phone** — exactly 9 digits accepted; 8 or 10 digits rejected; letters rejected; null/empty rejected
- **Password** — meets all rules accepted; missing uppercase, lowercase, number, special character, or minimum length each individually rejected; null rejected
- **Password matching** — identical strings accepted; different strings rejected; null first argument rejected

### Instrumented (Espresso) Test — `WelcomeActivityTest.java`

| | |
|---|---|
| **Location** | `app/src/androidTest/java/com/mmm/bookingapp/auth/WelcomeActivityTest.java` |
| **Run via** | Right-click → **Run** *(requires a connected device/emulator)*, or `./gradlew connectedAndroidTest` |

Confirms the Welcome Screen *(true app entry point)* renders both **Register** and **Login** buttons, visible and correctly labelled.

---

## 2. Manual Test Plan

### Functional Testing

| # | Feature | Steps | Expected Result | Pass/Fail |
|---|---|---|---|---|
| F1 | Registration | Fill all fields correctly, tick consent, submit | Account created, verification email sent | |
| F2 | Registration blocked without consent | Fill all fields, leave consent unticked, submit | Red error shown, no account created | |
| F3 | Email verification gate | Log in before verifying email | Blocked, resend option shown | |
| F4 | Login (verified account) | Log in with correct credentials | Routed to Home (client) or Admin Dashboard (admin), with a brief "Logged in successfully" toast on arrival | |
| F5 | Password reset | Request reset for a registered email | Reset email received | |
| F6 | Booking submission | Complete and submit a valid booking | Confirmation screen shown, booking appears in My Bookings, immediate notification received | |
| F7 | Double-booking prevention | Two accounts attempt the same slot | Second attempt rejected | |
| F8 | Public holiday block | Attempt a known SA public holiday date | Booking date rejected with a clear message | |
| F9 | Cancellation | Cancel a pending/confirmed booking | Confirmation dialog shown, status becomes Cancelled | |
| F10 | Admin confirm/decline | Admin confirms a pending booking | Status updates automatically on the client's My Bookings screen in real time — no manual reload needed | |
| F11 | Admin search | Type a service name into the search box | List filters to matching bookings only | |
| F12 | Admin search + filters combined | Apply a status filter, then search | Only bookings matching both are shown | |
| F13 | CSV export | Tap Export CSV on Admin Dashboard | Share sheet opens with a valid CSV file | |
| F14 | Practice Information edit | Admin edits phone number, saves | Client's Contact screen reflects the change | |
| F15 | Admin Analytics | Open View Detailed Analytics | Time-bucketed counts, % change, and bar-chart comparisons display correctly for both Bookings Created and New Registrations | |
| F16 | Settings — single logout | Open Settings, scroll to Support | Exactly one Logout button present | |
| F17 | Dark Mode toggle confirmation | Toggle Dark Mode on Settings | Toast confirms "Dark Mode enabled"/"disabled"; theme changes across the whole app | |
| F18 | Booking Reminders toggle confirmation | Toggle Booking Reminders on Settings | Toast confirms "Booking reminders enabled"/"disabled" | |
| F19 | Logout confirmation dialog | Tap Logout from Home, Settings, and Admin Dashboard | Each shows a Cancel/Log Out dialog; Cancel dismisses with no action; Log Out signs out and returns to Welcome | |
| F20 | Delete Account explainer | Tap Delete My Account on Settings | A dialog explains the process first; Continue opens the pre-addressed email app; Cancel dismisses with no action | |
| F21 | Back button on Home/Admin Dashboard | Press back once, then again within 2 seconds | First press shows "Press back again to exit" toast and does nothing else; second press exits the app; user remains logged in | |
| F22 | Session persistence | Log in, fully close the app (not just background it), reopen | App opens directly to Home/Admin Dashboard without requiring login again | |
| F23 | Real-time admin list update | With Admin Dashboard open, have a client submit a new booking from another device/account | New booking and updated summary counts appear on the Admin Dashboard automatically | |

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
| U8 | Logout dialog wording | Dialog clearly states logging out requires logging in again; button order is Cancel (left) then Log Out (right) | |
| U9 | Tablet layout — centred content | On a 7"/10" tablet, form and card content is centred with generous margins, not stretched edge-to-edge | |
| U10 | Tablet layout — no dead space | Home, Book Appointment, Settings, and other fixed-content screens balance vertically on a tall tablet screen rather than leaving empty space at the bottom | |
| U11 | Tablet font scaling | Text across all screens is visibly larger and more readable on tablet than on phone, without any layout breaking | |
| U12 | Landscape orientation | Key screens (Home, Book Appointment, Register) remain usable and scrollable in landscape on both phone and tablet | |

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
| E8 | Rapid double back-press on a non-root screen (e.g. Book Appointment) | Normal back navigation occurs — the "press again to exit" behaviour is specific to Home/Admin Dashboard only, not app-wide | |

### Bug Testing and Fixing

Real bugs found and resolved during this build:

| # | Bug | Root Cause | Resolution |
|---|---|---|---|
| B1 | Admin Dashboard XML failed to compile after adding `app:tint` | Missing `xmlns:app` namespace on root layout tag | Added the namespace declaration |
| B2 | Logout unreachable on Settings | Buttons existed but sat below the visible scroll area, no scroll indicator | Added a scroll thumb and repositioned logout |
| B3 | Emulator "Unknown error" on run | Emulator process had hung | Cold-booted the emulator |
| B4 | `PackageInfo` import error | Wrong package path (`android.pm` vs `android.content.pm`) | Corrected the import |
| B5 | Test files failed to resolve JUnit/Espresso imports | Files placed in `src/main/java` instead of `src/test`/`src/androidTest` | Moved to correct source sets |
| B6 | `strings.xml` failed to parse | Stray leftover `</parameter>` tag accidentally included | Removed the invalid tag |
| B7 | Admin login crashed after date-filter redesign | `tvFilterDate` changed from TextView to LinearLayout in XML but Java field type wasn't updated | Changed field type to View |
| B8 | Register screen: consent checkbox text clipped, "Policy" appeared cut off | Checkbox row height/text-wrap not properly configured | Set rows to genuine `wrap_content`, added `maxLines`/`ellipsize="none"`, added explicit bottom margins |
| B9 | Status badges hard to read in light mode | Black text on dark-coloured badge backgrounds (Confirmed/Completed/Cancelled) | Changed badge text to bold white, consistent contrast across all four statuses |
| B10 | Pressing back on Home logged the user out / returned to Welcome unexpectedly | `WelcomeActivity`'s Register/Login buttons never called `finish()`, so Welcome stayed on the back stack beneath Home; `FLAG_ACTIVITY_CLEAR_TOP` did not clear it | Implemented double-press-to-exit `onBackPressed()` on Home/Admin Dashboard; changed navigation flags to `FLAG_ACTIVITY_NEW_TASK \| FLAG_ACTIVITY_CLEAR_TASK` in `LoginActivity` and `MainActivity` to properly clear the pre-login stack |
| B11 | Register screen failed to centre content vertically on tablet (content stuck at top, dead space below) | `NestedScrollView` combined with `fillViewport="true"` and a `layout_weight`-based centering child does not reliably stretch to full viewport height, unlike plain `ScrollView` | Switched Register (and proactively, Settings) from `NestedScrollView` to `ScrollView` |
| B12 | Register screen: back button and title centred vertically along with the form on tablet, instead of staying pinned at top | Header row and form content were placed inside the same centred container, so the centering behaviour dragged the header down too | Split the header into its own fixed top block, separate from the centred content column below it, matching the working pattern already used on Login |

---

## 3. How Testing Was Performed

All manual tests were performed on an **Android Studio emulator**, with Airplane Mode toggled where noted to simulate offline conditions. The **Firebase Console** was used independently to verify that Firestore writes *(bookings, cancellations, registrations)* matched what the app displayed after each test.

Tablet-specific tests **(U9–U12)** were performed using Android Studio's **Pixel Tablet** and **Nexus 10** emulator profiles, in addition to the standard phone profile, in both portrait and landscape orientation.
