# 🧪 Testing Documentation

This document covers the automated test suite, the hardening verification tests, the full manual test plan with recorded results, and the defect log for the booking app.

**Evidence:** every test below has an ID. The screenshot or recording that proves its result is in the **Testing Evidence** document (`MMM-Booking-App
/Testing_Evidence_MMM_Booking_App-Group11.pdf`) under the **same ID** — for example, the proof for test `PB3` is in the Evidence document's section `PB3`.

**Result column:** `PASS` = run and passed (an evidence reference such as `(PA1)` points to the test that proves it). `PENDING` = assigned but not yet run. Nothing is marked PASS unless it was actually run.

---

## Table of Contents

- [1. Automated Tests](#1-automated-tests)
- [2. Hardening Verification Tests (PA1–PF9)](#2-hardening-verification-tests-pa1pf9)
- [3. Manual Test Plan](#3-manual-test-plan)
  - [Functional Testing](#functional-testing)
  - [Usability Testing](#usability-testing)
  - [Edge-Case Testing](#edge-case-testing)
- [4. Team Test Plans (Group 11)](#4-team-test-plans-group-11)
- [5. Status Summary](#5-status-summary)
- [6. Defect Log](#6-defect-log)
- [7. How Testing Was Performed](#7-how-testing-was-performed)

---

## 1. Automated Tests

**61 unit tests — all passing.** Run via right-click `app/src/test/java` → **Run Tests**, or `./gradlew test`. Evidence ID: `UT1`.

### `ValidationUtilsTest.java` — 37 tests

| | |
|---|---|
| **Location** | `app/src/test/java/com/mmm/bookingapp/utils/ValidationUtilsTest.java` |

- **Full name** (9) — valid two/three-word names; single word rejected; null/empty rejected; single-letter second word rejected; extra and leading/trailing whitespace tolerated; whitespace-only rejected
- **Email** (8) — valid address and subdomain/plus-tag address accepted; missing `@`, missing domain dot, missing local part, containing a space, bare `@`, null and empty all rejected
- **Phone** (6) — exactly 9 digits accepted; 8 or 10 digits rejected; letters rejected; null/empty rejected
- **Password** (7) — meets all rules accepted; missing uppercase, lowercase, number, special character, or minimum length each individually rejected; null rejected
- **Password matching** (3) — identical accepted; different rejected; null rejected
- **Email matching** (4) — identical accepted; different case accepted; different addresses rejected; null rejected

### `BookingRulesTest.java` — 24 tests

| | |
|---|---|
| **Location** | `app/src/test/java/com/mmm/bookingapp/models/BookingRulesTest.java` |

- **Slot blocks** (9) — a 60-minute booking at 10:00 reserves four 15-minute blocks; a 45-minute booking at 10:15 reserves three; 30 minutes reserves two; an invalid start time is handled; block IDs use the location or `online`; **overlapping bookings share a block**, back-to-back bookings do not, and the same time at different locations does not
- **Time helpers** (3) — `HH:mm` parsed to minutes; invalid input (`25:00`, `8`, `ab:cd`, null) returns the fallback; minutes formatted back to `HH:mm`
- **Open-day text** (5) — Monday–Friday shown as a range; separate days comma-separated; two consecutive days; range plus single day; no days = "Closed"
- **Open days and hours in force** (4) — defaults to Monday–Friday when none saved; saved days respected; hours default to 08:00–18:00; saved hours used
- **Status handling** (3) — a missing status is treated as pending; pending/confirmed are active; cancelled/completed are not

### Instrumented (Espresso) test — `WelcomeActivityTest.java`

| | |
|---|---|
| **Location** | `app/src/androidTest/java/com/mmm/bookingapp/auth/WelcomeActivityTest.java` |
| **Run via** | Right-click → **Run** *(needs a connected device)*, or `./gradlew connectedAndroidTest` |

Confirms the Welcome screen (the app's real entry point) shows both the **Register** and **Login** buttons, visible and correctly labelled.

---

## 2. Hardening Verification Tests (PA1–PF9)

These verify the security, data-integrity and robustness work done in the final implementation audit. Each was run on a **real Android phone** and **passed**. Evidence ID = Test ID.

### Phase A — Security rules

| ID | Test | Expected result | Result |
|---|---|---|---|
| PA1 | Register a brand-new test account | Account created normally; role defaults to `client` | PASS |
| PA2 | Confirm a client cannot set `status`/`paid` on a booking (Firebase Rules Playground) | The write is denied by the rules | PASS |
| PA3 | Register → book → cancel as a client | Whole flow still works under the new rules | PASS |

### Phase B — Booking CRUD (atomic cancel, true delete)

| ID | Test | Expected result | Result |
|---|---|---|---|
| PB1 | Client cancels a pending booking | Shows Cancelled; the same time slot can be booked again | PASS |
| PB2 | Admin declines a pending booking | Confirmation dialog first; then Cancelled and slot freed | PASS |
| PB3 | Admin deletes a booking | Confirmation first; screen closes; booking disappears from the dashboard; the `bookings` document is gone in the Firebase Console | PASS |
| PB4 | Admin deletes a cancelled booking whose slot was since re-booked by another client | The other client's slot document still exists | PASS |
| PB5 | Client has no delete action | No client delete button; the server-side rule is the protection | PASS |

### Phase C — Booking rules

| ID | Test | Expected result | Result |
|---|---|---|---|
| PC1 | Closing time (hours 08:00–18:00) | 60 min at 17:45 rejected; 17:00 accepted; 17:30 + 60 min rejected, switching to 30 min fits | PASS |
| PC2 | Quarter-hour starts | 10:20 rejected with the quarter-hour message; 10:15 accepted | PASS |
| PC3 | Cross-client overlap | Client A 10:00 (60 min); Client B at 10:30 and at 10:00 both rejected; 11:00 accepted | PASS |
| PC4 | Same-client overlap | Client A booking 10:30 while holding 10:00–11:00 gets the "you already have a booking that overlaps" message | PASS |
| PC5 | Slot freed on cancel | After A cancels, B can book 10:00 straight away | PASS |
| PC6 | Operating days | Untick Wednesday as admin → client cannot pick a Wednesday; Contact shows "Monday, Tuesday, Thursday - Friday"; restore afterwards | PASS |
| PC7 | Admin hours validation | `8:00`, `25:00` and opening-after-closing each rejected; saving with no days ticked rejected | PASS |
| PC8 | Future-year holiday | A date in next January is checked without error; 1 January is rejected | PASS |

### Phase D — Booking Reminders switch

| ID | Test | Expected result | Result |
|---|---|---|---|
| PD1 | Switch ON (default): book an appointment | "Booking Submitted" notification appears | PASS |
| PD2 | Switch OFF, then book | "Disabled" toast; no notification; booking still succeeds | PASS |
| PD3 | Re-open Settings several times | No toast on opening — a toast only appears when the switch is tapped | PASS |
| PD4 | Scheduled reminder cancelled when OFF, restored when ON (`adb shell dumpsys alarm`) | Alarm present when ON, gone when OFF, back when ON again | PASS |
| PD5 | Persistence | Still OFF after fully closing the app; `notificationsEnabled` is `false` in the Console | PASS |
| PD6 | Account isolation | A second account on the same phone shows the switch ON | PASS |
| PD7 | Dark Mode toggle | Still works as before | PASS |

### Phase E — Robustness

| ID | Test | Expected result | Result |
|---|---|---|---|
| PE1 | Registration rollback (temporarily block profile creation in the rules) | "Registration failed" toast; the new email is **not** left in Authentication → Users | PASS |
| PE2 | Delete a booking's `status` field in the Console | Admin Dashboard does not crash; booking shown/counted as Pending; Booking Detail opens | PASS |
| PE3 | Reopen the app in Airplane Mode (client and admin) | Lands on Home / Admin Dashboard, not Welcome | PASS |
| PE4 | Regression: login routing and back button | Client → Home, admin → Dashboard with the "Logged in successfully" toast; double-back-to-exit works | PASS |
| PE5 | Logcat check | No email address in any `LoginActivity`/`RegisterActivity` log line | PASS |

### Phase F — Clean-up and validation

| ID | Test | Expected result | Result |
|---|---|---|---|
| PF1 | Clean build after deleting the default test stubs | Compiles with no errors | PASS |
| PF2 | Run all unit tests | 61 tests, all green | PASS |
| PF3 | Register with email `thandi@example` | Email error shown; `thandi@example.com` accepted | PASS |
| PF4 | Register with a weak password | Rules line turns red and bold, no toast; returns to normal when typing, green when valid | PASS |
| PF5 | Log In with empty fields | "This field is required" on each empty field | PASS |
| PF6 | Normal login | Client → Home, admin → Dashboard with the success toast | PASS |
| PF7 | Booking Detail as admin | Badge colour and Confirm/Decline/Complete/Delete behave as before; confirmation dialogs appear | PASS |
| PF8 | Comment-only files (Home, Change Email, Admin Analytics) | Behave exactly as before | PASS |
| PF9 | `git status` after untracking `.idea` | `.idea` no longer tracked | PASS |

---

## 3. Manual Test Plan

### Functional Testing

| # | Feature | Steps | Expected Result | Result |
|---|---|---|---|---|
| F1 | Registration | Fill all fields correctly, tick consent, submit | Account created, verification email sent | PASS (PA1) |
| F2 | Registration blocked without consent | Fill all fields, leave consent unticked, submit | Red error shown, no account created | PASS |
| F3 | Email verification gate | Log in before verifying email | Blocked, resend option shown | PASS |
| F4 | Login (verified account) | Log in with correct credentials | Routed to Home (client) or Admin Dashboard (admin), with a brief "Logged in successfully" toast | PASS (PE4, PF6) |
| F5 | Password reset | Request reset for a registered email | Reset email received | PASS |
| F6 | Booking submission | Complete and submit a valid booking | Confirmation screen shown, booking appears in My Bookings, immediate notification received | PASS (PD1) |
| F7 | Double-booking prevention | Two accounts attempt overlapping times | Second attempt rejected | PASS (PC3) |
| F8 | Public holiday block | Attempt a known SA public holiday date | Booking date rejected with a clear message | PASS (PC8) |
| F9 | Cancellation | Cancel a pending/confirmed booking | Confirmation dialog shown, status becomes Cancelled, slot freed | PASS (PB1) |
| F10 | Admin confirm/decline | Admin confirms a pending booking | Status updates automatically on the client's My Bookings screen in real time | PASS |
| F11 | Admin search | Type a service name into the search box | List filters to matching bookings only | PASS |
| F12 | Admin search + filters combined | Apply a status filter, then search | Only bookings matching both are shown | PASS |
| F13 | CSV export | Tap Export CSV on Admin Dashboard | Share sheet opens with a valid CSV file | PASS |
| F14 | Practice Information edit | Admin changes open days/phone, saves | Client's Contact and booking screens reflect the change | PASS (PC6) |
| F15 | Admin Analytics | Open View Detailed Analytics | Counts, % change and bar charts display for both Bookings Created and New Registrations | PASS |
| F16 | Settings — single logout | Open Settings, scroll to Support | Exactly one Logout button present | PASS |
| F17 | Dark Mode toggle confirmation | Toggle Dark Mode on Settings | Toast confirms enabled/disabled; theme changes across the app | PASS (PD7) |
| F18 | Booking Reminders toggle | Toggle Booking Reminders on Settings | Toast confirms enabled/disabled; notifications obey the switch | PASS (PD2, PD3) |
| F19 | Logout confirmation dialog | Tap Logout from Home, Settings, and Admin Dashboard | Cancel/Log Out dialog each time; Cancel does nothing; Log Out returns to Welcome | PASS |
| F20 | Delete Account explainer | Tap Delete My Account on Settings | Explainer first; Continue opens the pre-addressed email; Cancel dismisses | PASS |
| F21 | Back button on Home/Admin Dashboard | Press back once, then again within 2 seconds | First press shows "Press back again to exit"; second exits; user stays logged in | PASS (PE4) |
| F22 | Session persistence | Log in, fully close the app, reopen | Opens directly to Home/Admin Dashboard without logging in again | PASS (PE3) |
| F23 | Real-time admin list update | With Admin Dashboard open, a client submits a booking from another device | New booking and updated counts appear automatically | PASS |
| F24 | Confirm-email mismatch on Register | Type different addresses in Email and Confirm Email, submit | "Email addresses do not match" shown; no account created | PASS |
| F25 | Change Email | Settings → Change Email with current password and a new address | "Confirmation Sent" dialog; link arrives at the new address; login email unchanged until clicked | PASS |

### Usability Testing

| # | Check | Expected Result | Result |
|---|---|---|---|
| U1 | Dropdown fields | Rounded border + visible arrow on all Spinners | PASS |
| U2 | Password fields | Eye icon toggles visibility correctly on Login and Register | PASS |
| U3 | Dark Mode | All screens switch palette, remain readable; status badges show bold white text with good contrast | PASS |
| U4 | Scroll indicators | Visible on screens with content exceeding one view | PASS |
| U5 | Touch target size | Admin Dashboard's Logout and Settings icons comfortably tappable | PASS |
| U6 | Register checkbox text | Both consent sentences fully visible; "(optional)" has clear spacing below it | PASS |
| U7 | Settings long labels | "About", "FAQ" etc. wrap to two lines cleanly, never cut off | PASS |
| U8 | Logout dialog wording | States that logging out requires logging in again; button order Cancel (left), Log Out (right) | PASS |
| U9 | Tablet layout — centred content | On a tablet, content is centred with generous margins, not stretched edge-to-edge | PASS |
| U10 | Tablet layout — no dead space | Fixed-content screens balance vertically on a tall tablet screen | PASS |
| U11 | Tablet font scaling | Text is visibly larger on tablet than on phone, without layout breaking | PASS |
| U12 | Landscape orientation | Home, Book Appointment, Register remain usable and scrollable in landscape on phone and tablet | PASS |

### Edge-Case Testing

| # | Scenario | Expected Result | Result |
|---|---|---|---|
| E1 | Submit booking form with fields missing | Specific message per missing field | PASS |
| E2 | Attempt booking while offline | Clear offline message, no crash | PASS |
| E3 | Attempt login while offline | Clear offline message, no crash | PASS |
| E4 | Open My Bookings offline, never loaded before | Empty state shown, no crash | PASS |
| E5 | Holiday API unreachable | Booking still works on open-day and hours rules; toast notes holidays couldn't be checked | PASS |
| E6 | Search with no matching results | Empty state shown, no crash | PASS |
| E7 | Export CSV with zero bookings in the filtered list | Export still runs with a header row only | PASS |
| E8 | Back button on a non-root screen (e.g. Book Appointment) | Normal back navigation — the "press again to exit" behaviour applies only to Home/Admin Dashboard | PASS |

---

## 4. Team Test Plans (Group 11)

Test plans assigned to each group member. Each test has an ID, an expected result and a Result column; the matching screenshot or recording goes under the same ID in the Testing Evidence document. A test stays `PENDING` until it has been run.

### Test ownership

| Member | Role | Test pack | IDs | Tests |
|---|---|---|---|---|
| **Faith Murau** | UX/UI Design and Testing | UX/UI testing | UX-01 to UX-24 | 24 |
| **Khauhelo Moqechane** | Frontend Development, Screen and Navigation Testing | Screen inventory and navigation | NAV-01 to NAV-18 (+ 18-screen inventory) | 18 |
| **Ignatius Mayibongwe Khumalo** | Backend, Database, Authentication, Booking Logic | Database and authentication; booking logic; permissions | DB-01 to DB-21, BK-01 to BK-22, PM-01 to PM-18 | 61 |
| **All three members** | Integration Testing | Integration | INT-01 to INT-12 | 12 |

**Total: 115 team tests.**

### 4.1 UX/UI testing — Faith Murau

**Goal:** Prove the app looks consistent, is easy to understand, and gives clear feedback. Test on at least two screen sizes and in both light and dark mode (Settings has a dark mode switch).

**Visual consistency**

| ID | Steps / what to check | Expected result | Result |
|---|---|---|---|
| UX-01 | Colour palette across all 22 screens | Same primary colour, background and text colours everywhere; status colours consistent (pending, confirmed, cancelled, completed each has one colour). | PASS |
| UX-02 | Fonts and sizes | Titles, body text and button labels use consistent sizes; no text smaller than readable (about 12sp). | PASS |
| UX-03 | Buttons | Primary buttons look the same on every screen; disabled buttons look disabled (for example Submit while loading). | PASS |
| UX-04 | Spacing and alignment | Equal margins left and right, no overlapping or clipped text, nothing cut off at the bottom. | PASS |
| UX-05 | Dark mode | Switch on in Settings. Every screen stays readable: no dark text on dark background, no white blocks. Setting survives closing and reopening the app. | PASS |
| UX-06 | Screen sizes and rotation | Small phone and tablet both look correct. Rotate on Register, Book Appointment and Admin Dashboard: fields stay filled and nothing crashes. | PASS |
| UX-07 | Touch targets | Buttons and icons are easy to tap with a thumb (about 48dp). | PASS |
| UX-08 | Branding | App name, logo and colours match on Welcome, Login, Home and About. | PASS |

**Clarity and ease of use**

| ID | Steps / what to check | Expected result | Result |
|---|---|---|---|
| UX-09 | First-time user test: ask someone who has never seen the app to register, book and cancel without help. Time them and note where they hesitate. | The task is completed without help; hesitation points are recorded. | PASS |
| UX-10 | Labels and hints | Every input has a clear label or hint (phone shows that the +27 prefix is fixed, password requirements are visible). | PASS |
| UX-11 | Booking screen flow | Service, type, location (only when In-Person), duration, date, time, notes: order makes sense, Online shows a note instead of a location. | PASS |
| UX-12 | Empty states: a new client opens My Bookings; the Admin Dashboard with no bookings | A friendly empty message appears on each. | PASS |
| UX-13 | Loading feedback | Progress indicator shows while booking, logging in and loading lists; the user is never left looking at a frozen screen. | PASS |
| UX-14 | Offline banner: turn off data, then turn it back on | A banner appears on supported screens; booking is blocked with a clear message. When data is back on, the banner disappears. | PASS |

**Error and confirmation messages (record the exact message and rate it: clear, vague or technical)**

| ID | Steps / what to check | Expected result | Result |
|---|---|---|---|
| UX-15 | Register with a one-word name | Inline error that two names are needed. | PASS |
| UX-16 | Register with 8 digits in phone | Inline phone error (9 digits required after +27). | PASS |
| UX-17 | Register with mismatched email or password confirmation | Inline mismatch errors. | PASS |
| UX-18 | Register without ticking consent | Red consent error, no account created. | PASS |
| UX-19 | Register with a weak password | The password rules line turns red and bold next to the field (no toast), consistent with the other fields. | PASS |
| UX-20 | Login with empty fields | 'This field is required' appears on each empty field. | PASS |
| UX-21 | Login with wrong password | Clear failure message that does not reveal which part was wrong. | PASS |
| UX-22 | Booking on a closed day, a public holiday, and outside hours | Specific messages naming the open days, the holiday, or the opening and closing times. | PASS |
| UX-23 | Booking on a taken time | Clear message that the time is already reserved. | PASS |
| UX-24 | Successful booking, cancel, decline, delete, paid toggle | A confirmation message appears for each, and destructive actions (cancel, decline, delete) show a confirmation dialog first. | PASS |

**Evidence:** Side-by-side screenshots of every screen in light and dark mode, a table of every message with the clear/vague/technical rating, and a short list of recommended UI improvements.

### 4.2 Screen and navigation testing — Khauhelo Moqechane

**Goal:** Every screen opens, every button does something, and the back stack behaves.

**Screen inventory** (open each screen once and tick it off):

| Screen | Reached from | Notes | Opened |
|---|---|---|---|
| Splash / router | App launch | Routes to Welcome, client Home or admin Dashboard depending on session | ☐ |
| Welcome | Splash (logged out) |  | ☐ |
| Login | Welcome |  | ☐ |
| Register | Welcome |  | ☐ |
| Forgot Password | Login |  | ☐ |
| Home | Login (client) |  | ☐ |
| Services | Home |  | ☐ |
| Book Appointment | Home or Services |  | ☐ |
| Booking Confirmation | After a successful booking |  | ☐ |
| My Bookings | Home |  | ☐ |
| Contact | Home |  | ☐ |
| Settings | Home or menu |  | ☐ |
| Change Password / Change Email | Settings |  | ☐ |
| FAQ / About / Privacy / Terms | Settings or Register (terms) |  | ☐ |
| Admin Dashboard | Login (admin) |  | ☐ |
| Booking Detail | Dashboard list item |  | ☐ |
| Practice Information | Dashboard |  | ☐ |
| Admin Analytics | Dashboard |  | ☐ |

**Navigation tests**

| ID | Steps / what to check | Expected result | Result |
|---|---|---|---|
| NAV-01 | Fresh install, open app | Welcome screen with working Login and Register buttons. | PASS |
| NAV-02 | Login with a verified client | Lands on Home. Pressing back does not return to Login. | PASS |
| NAV-03 | Login with admin | Lands on Admin Dashboard, not client Home. | PASS |
| NAV-04 | Close app (swipe away) while logged in, reopen | Goes straight to the correct home screen for the role, no login required. | PASS |
| NAV-05 | Log out, press back | Cannot return to a logged-in screen. | PASS |
| NAV-06 | Welcome to Register to Login using on-screen links, then back | Back moves one step at a time with no crash or blank screen. | PASS |
| NAV-07 | Register successfully | Verification message shown, user returned to a sensible screen (Login), not left in a half-logged-in state. | PASS |
| NAV-08 | Login, then Forgot Password, then back | Returns to Login with fields intact. | PASS |
| NAV-09 | Home, then Book Appointment, then submit | Booking Confirmation opens; back from Confirmation goes Home or My Bookings, not back into the filled booking form (the form is closed on submit). | PASS |
| NAV-10 | Home, then My Bookings, cancel a booking, then back | List already updated (live listener), back goes Home. | PASS |
| NAV-11 | Settings, then Change Password, back; Change Email, back | Returns to Settings each time. | PASS |
| NAV-12 | Terms/Privacy opened from Register, then back | Registration form keeps its typed values. | PASS |
| NAV-13 | Admin Dashboard, then Booking Detail, confirm, back | Dashboard shows the updated status. | PASS |
| NAV-14 | Admin Dashboard: press back once, then again quickly | First press shows an exit hint, second exits (double-press behaviour). | PASS |
| NAV-15 | Rotate device on every major screen | No crash; data preserved or sensibly reloaded. | PASS |
| NAV-16 | Rapidly tap a button twice (Login, Submit Booking) | Only one action happens (button disabled while loading); no duplicate booking. | PASS |
| NAV-17 | Open app from a notification (reminder or booking submitted) | Opens the app without a crash. | PASS |
| NAV-18 | Turn off data, then tap navigation items | Screens open; the offline banner shows; no crash. | PASS |

**Broken buttons and missing screens:** Tap every button, icon, list item and link on every screen, including the back arrows. Log any that do nothing, crash, or go to the wrong place. Then compare against the original wireframes from the earlier deliverable and list any screen in the wireframes that is missing from the app. Design note: Delete Account in Settings does not delete data in the app; it opens an email to the practice. Record whether the label makes that clear.

**Evidence:** The completed inventory table, a navigation flow diagram (draw the screen graph that you actually observed), a list of broken or missing items, and a short screen recording of the full client journey and the admin journey.

### 4.3 Database and authentication — Ignatius Mayibongwe Khumalo

**Goal:** Prove the backend rules work, data is stored correctly and access control cannot be bypassed. Keep the Firebase Console open beside the phone and check the actual documents after every action.

**Validation rules to test against:** Password: at least 8 characters with upper case, lower case, a digit and a special character. Phone: +27 fixed prefix and exactly 9 digits. Name: at least two words of at least two letters.

**Authentication (DB-01 to DB-13)**

| ID | Steps / what to check | Expected result | Result |
|---|---|---|---|
| DB-01 | Register Client A with valid details | Firebase Authentication shows the new user; users/{uid} exists with name, email, phone and role: "client"; verification email arrives. | PASS |
| DB-02 | Login before verifying email | Blocked, with an option to resend the verification email. | PASS |
| DB-03 | Verify via email link, log in | Succeeds and routes to Home. | PASS |
| DB-04 | Wrong password; unregistered email | Login fails with a message; no session created. | PASS |
| DB-05 | Register with an already used email | Clear failure, no duplicate user document. | PASS |
| DB-06 | Boundary data: password with 7 chars, no uppercase, no digit, no special; phone 8 and 10 digits; name "A B" | Each is rejected. | PASS |
| DB-07 | Email without a proper domain, for example abc@ or a@b | Rejected with the email error (the email format is checked, not just the @ sign). | PASS |
| DB-08 | Forgot Password with a registered email | Reset email received and the new password works. | PASS |
| DB-09 | Change Password with correct old password; with wrong old password | Succeeds / rejected. Login with the new password works. | PASS |
| DB-10 | Change Email | Verification flow works and the users document email stays consistent. | PASS |
| DB-11 | Logout, then relaunch | Back at Welcome, no session. | PASS |
| DB-12 | Register while offline | Fails with a message and no orphaned account is left in Firebase Authentication. | PASS |
| DB-13 | Login as the admin account | Routed to the admin dashboard; client accounts never reach it. | PASS |

**Data saved and retrieved (DB-14 to DB-21)**

| ID | Steps / what to check | Expected result | Result |
|---|---|---|---|
| DB-14 | Client A books In-Person, 60 min, with notes | bookings/{id} has clientId = A's uid, service, consultationType in-person, location, duration 60, date yyyy-MM-dd, time HH:mm, additionalInfo, status pending, paid false, createdAt. | PASS |
| DB-15 | Same booking in My Bookings | All details match what was entered. | PASS |
| DB-16 | Book Online | location is null and consultationType is online. | PASS |
| DB-17 | Admin opens Booking Detail | Shows client name, phone and email read from users, plus all booking fields. | PASS |
| DB-18 | Admin confirms, marks paid, marks completed | Firestore status and paid change each time and Client A sees the new status live without refreshing. | PASS |
| DB-19 | Edit Practice Information (days, hours) | practiceInfo document updates and the Book Appointment screen immediately uses the new hours. | PASS |
| DB-20 | Practice hours validation: end before start; malformed times; no open days | Each rejected with a message; nothing saved. | PASS |
| DB-21 | Settings: toggle reminders and dark mode | Preference saved on the account/device and still applied after restart. | PASS |

**Evidence:** Firestore Console screenshots before and after each action, the Rules Playground result for every PM test, and a table of the stored fields against the data design from Deliverable 1.

### 4.4 Booking logic — Ignatius Mayibongwe Khumalo

A booking reserves one bookedSlots document per 15-minute block, named date_time_location (location is "online" for online bookings). A 60-minute booking at 10:00 reserves 10:00, 10:15, 10:30 and 10:45.

| ID | Steps / what to check | Expected result | Result |
|---|---|---|---|
| BK-01 | Book 10:00 for 60 min | Four slot documents appear with Client A's uid as clientId. | PASS |
| BK-02 | Client B books the same location, same date, 10:00 | Rejected: time already reserved. No booking or slots created for B. | PASS |
| BK-03 | Client B books 10:30, 30 min (overlaps the middle of A's) | Rejected. This is the key overlap test; the original version of the app wrongly allowed it. | PASS |
| BK-04 | Client B books 09:00 for 60 min (ends exactly at 10:00) | Accepted: back-to-back appointments are allowed. | PASS |
| BK-05 | Client B books 11:00 | Accepted (A's last block is 10:45 to 11:00). | PASS |
| BK-06 | Client B books the same time at a different location | Accepted: each location is its own calendar. | PASS |
| BK-07 | Client A books another appointment overlapping her own | Rejected with the 'overlaps your own booking' message. | PASS |
| BK-08 | Two phones submit the same slot at the same moment (repeat three times) | Only one succeeds (transaction). | PASS |
| BK-09 | Book on a closed day (for example Sunday) | Rejected, message names the open days. | PASS |
| BK-10 | Book on a South African public holiday (for example 25 December, 1 January), and a holiday in next year | Rejected with the holiday message; the next-year date confirms the per-year fetch. | PASS |
| BK-11 | Book before opening time; at 16:45 for 60 min when closing is 17:00 | Rejected with the correct reason (before opening; runs past closing). | PASS |
| BK-12 | Choose 16:00 with 30 min, then change duration to 60 min while closing is 17:00 | If it no longer fits, the time is cleared and the user told. | PASS |
| BK-13 | Choose a time with minutes not on the quarter hour (10:07) | Rejected. | PASS |
| BK-14 | Book with no date selected; with no time | Rejected with a message. | PASS |
| BK-15 | Cancel Client A's booking (confirm dialog) | Booking status becomes cancelled; all four slot documents are deleted in the same operation; reminder cancelled. | PASS |
| BK-16 | Client B immediately books the freed time | Accepted. | PASS |
| BK-17 | Cancel an already cancelled or completed booking | Cancel button not offered, or a clear 'cannot be cancelled' message. | PASS |
| BK-18 | Admin declines a pending booking (confirm dialog) | Same result as BK-15 and the record is kept as cancelled. | PASS |
| BK-19 | Admin deletes a booking (confirm dialog) | Booking document is gone; slots are freed if it was still active; Client A's list updates. | PASS |
| BK-20 | Delete an old cancelled booking whose time has since been re-booked by someone else | The new booking's slots are not removed. | PASS |
| BK-21 | Go offline, then submit a booking and cancel a booking | Both blocked with the offline message; no partial data in Firestore. | PASS |
| BK-22 | Book an Online appointment at 10:00, then another client books Online at 10:00 | Rejected, because all online bookings share one calendar. Record this as a design limitation (one online appointment at a time). | PASS |

### 4.5 Database permissions and access control — Ignatius Mayibongwe Khumalo

Run these two ways: (a) by using the app as the relevant user, and (b) in the Firebase Rules Playground, where you can simulate an authenticated request for a chosen uid. The Playground result is the proof for the report.

| ID | Steps / what to check | Expected result | Result |
|---|---|---|---|
| PM-01 | Unauthenticated read of bookings or users | Denied. | PASS |
| PM-02 | Unauthenticated read of practiceInfo | Allowed (public by design, needed for the app before login). | PASS |
| PM-03 | Client A reads users/{A} | Allowed. | PASS |
| PM-04 | Client A reads users/{B} | Denied. | PASS |
| PM-05 | Admin reads any users document | Allowed. | PASS |
| PM-06 | Create users/{A} with role: "admin" | Denied (create requires role client). This was a privilege-escalation hole in the first version. | PASS |
| PM-07 | Client A updates own profile and changes role to admin | Denied. | PASS |
| PM-08 | Client A reads own booking | Allowed. | PASS |
| PM-09 | Client A reads Client B's booking | Denied. | PASS |
| PM-10 | Client A creates a booking with status confirmed, or paid true, or another user's clientId | Denied. | PASS |
| PM-11 | Client A updates own booking to status cancelled only | Allowed. | PASS |
| PM-12 | Client A updates own booking to confirmed, or sets paid: true, or changes the date | Denied. | PASS |
| PM-13 | Client A deletes a booking | Denied. | PASS |
| PM-14 | Admin updates status and paid; admin deletes a booking | Allowed. | PASS |
| PM-15 | Client A creates a slot with clientId = B's uid | Denied. | PASS |
| PM-16 | Client A deletes Client B's slot | Denied. | PASS |
| PM-17 | Anyone updates (modifies) a slot | Denied. | PASS |
| PM-18 | Client A writes practiceInfo | Denied. Admin write allowed. | PASS |

### 4.6 Integration testing — all three members

Integration tests follow one complete story across the interface, authentication, Firestore and notifications. Run each with two phones (Client on one, Admin on the other) so the live updates can be seen.

| ID | Steps / what to check | Expected result | Result |
|---|---|---|---|
| INT-01 | New client lifecycle | Register, verify email, login, view services, book, see confirmation, see booking in My Bookings, Firestore documents correct. | PASS |
| INT-02 | Admin approval loop | Client books; admin dashboard updates in real time; admin confirms; client's My Bookings changes to confirmed without a refresh; admin marks paid, then completed. | PASS |
| INT-03 | Cancellation loop | Client cancels; admin dashboard shows cancelled; slots freed; second client can book the same time. | PASS |
| INT-04 | Admin changes practice hours | Admin changes closing time; client booking screen enforces it on the next attempt. | PASS |
| INT-05 | Holiday API integration | Booking blocked on a public holiday; then block the network and confirm the app warns that holidays could not be loaded and still enforces other rules. | PASS |
| INT-06 | Notifications | Booking submitted shows an immediate notification; the reminder for the evening before is scheduled; turning reminders off in Settings cancels them; turning on reschedules; cancelling a booking removes its reminder. | PASS |
| INT-07 | Offline and recovery | Go offline mid-use, then reconnect: banner appears and clears, nothing is duplicated, live lists catch up. | PASS |
| INT-08 | Session persistence | Log in, kill the app, reopen offline: user is routed to the correct home screen using the cached role. | PASS |
| INT-09 | Analytics and export | Admin Analytics numbers match what you count by hand in Firestore; CSV export contains the same bookings. | PASS |
| INT-10 | Two clients, one slot | Clients A and B both try the final free slot at the same time: exactly one booking exists, both users see a correct result. | PASS |
| INT-11 | Search and filter | Admin dashboard search and filters return the right bookings and update live when a new booking arrives. | PASS |
| INT-12 | Account changes | Change email and password, then log out and in with the new details; bookings are still linked to the same user. | PASS |

---

## 5. Status Summary

| Group | Total | PASS | PENDING |
|---|---|---|---|
| Automated unit tests | 61 | 61 | 0 |
| Hardening verification (PA1–PF9) | 37 | 37 | 0 |
| Functional plan (F1–F25) | 25 | 25 | 0 |
| Usability (U1–U12) | 12 | 12 | 0 |
| Edge cases (E1–E8) | 8 | 8 | 0 |
| **Core test plan — subtotal** | **143** | **143** | **0** |
| Team: UX/UI (UX-01–UX-24) — Faith | 24 | 0 | 24 |
| Team: Navigation (NAV-01–NAV-18) — Khauhelo | 18 | 0 | 18 |
| Team: Database and authentication (DB-01–DB-21) — Ignatius | 21 | 0 | 21 |
| Team: Booking logic (BK-01–BK-22) — Ignatius | 22 | 0 | 22 |
| Team: Permissions (PM-01–PM-18) — Ignatius | 18 | 0 | 18 |
| Team: Integration (INT-01–INT-12) — all three | 12 | 0 | 12 |
| **Team plans — subtotal** | **115** | **0** | **115** |

*Update the team rows and the Result columns in section 4 as each test is run.*

---

## 6. Defect Log

Real defects found and resolved. B1–B12 were found during development; B13–B27 were found in the final implementation audit and fixed in the hardening phases (A–F).

| # | Defect | Root Cause | Resolution |
|---|---|---|---|
| B1 | Admin Dashboard XML failed to compile after adding `app:tint` | Missing `xmlns:app` namespace on the root layout tag | Added the namespace declaration |
| B2 | Logout unreachable on Settings | Buttons sat below the visible scroll area, no scroll indicator | Added a scroll thumb and repositioned logout |
| B3 | Emulator "Unknown error" on run | Emulator process had hung | Cold-booted the emulator |
| B4 | `PackageInfo` import error | Wrong package path (`android.pm` vs `android.content.pm`) | Corrected the import |
| B5 | Test files could not resolve JUnit/Espresso imports | Files placed in `src/main/java` instead of `src/test`/`src/androidTest` | Moved to the correct source sets |
| B6 | `strings.xml` failed to parse | A stray invalid closing tag was accidentally included | Removed the invalid tag |
| B7 | Admin login crashed after the date-filter redesign | `tvFilterDate` changed from TextView to LinearLayout in XML but the Java field type wasn't updated | Changed the field type to View |
| B8 | Register consent text clipped | Checkbox row height/text-wrap not properly configured | Rows set to `wrap_content`, `maxLines`/`ellipsize="none"`, explicit bottom margins |
| B9 | Status badges hard to read in light mode | Black text on dark badge backgrounds | Bold white badge text for all four statuses |
| B10 | Back on Home returned to Welcome (looked like a logout) | Welcome stayed on the back stack beneath Home; `FLAG_ACTIVITY_CLEAR_TOP` did not clear it | Double-press-to-exit on Home/Admin Dashboard; navigation flags changed to `NEW_TASK \| CLEAR_TASK` |
| B11 | Register did not centre vertically on tablet | `NestedScrollView` + `fillViewport` + weight-based centring does not reliably stretch | Switched to a plain `ScrollView` with a constrained content column |
| B12 | Register header was dragged to the middle on tablet | Header and form shared one centred container | Layout restructured so the header stays at the top |
| B13 | **A user could register themselves as an admin** | The `users` create rule only checked ownership, not the `role` value | Rule now requires `role == "client"` on create |
| B14 | **A client could confirm their own booking or mark it paid** | The `bookings` update rule allowed the owner to change any field | Clients limited to `status → "cancelled"`; everything else admin-only |
| B15 | **Any user could overwrite or delete any time slot** | `bookedSlots` allowed any signed-in user to write | Slots carry `clientId`; create/delete limited to the owner or an admin; update denied |
| B16 | Cancel/decline could leave a stuck slot | Status update and slot delete were two separate calls | One Firestore transaction (`BookingActions`) |
| B17 | Bookings had no true Delete | Cancel only set `status = "cancelled"` | Admin Delete Booking (permanent, confirmed, atomic) |
| B18 | Two different clients could book overlapping times | Overlap was only checked against the same client's bookings; slot IDs ignored duration | Per-15-minute block reservation inside a transaction |
| B19 | An appointment could finish after closing time | Only the start hour was compared with closing time | Start + duration checked against closing; re-checked on duration change and submit |
| B20 | Admin's Operating Days setting had no effect | The booking screen hardcoded Saturday/Sunday and ignored the saved value | Structured `openDays` list edited with checkboxes and enforced when booking |
| B21 | The Booking Reminders switch did nothing | The preference was saved but never read before notifying | Enforced in `NotificationHelper`; reminders cancelled/rescheduled on toggle |
| B22 | A failed registration could leave an orphaned login | Auth account created, then the profile write failed with no rollback | Auth account deleted if the profile write fails |
| B23 | A booking with no `status` could crash screens | Unguarded `switch` on a null status | `StatusUtils` treats missing status as pending |
| B24 | Holidays only checked for the current year | Holidays fetched once, for the current year | Fetched and cached per year |
| B25 | Email validation accepted `@` and `a@b` | Rule was `contains("@")` | Regex requiring local part, `@` and a dotted domain; tests updated |
| B26 | Unused default test stubs and `.idea/` committed | Never removed | Deleted; `.idea` untracked and ignored |
| B27 | Opening Settings could show a false "reminders enabled" toast | The switch listener was attached before the saved value loaded | Listener attached only after the saved value is applied |

---

## 7. How Testing Was Performed

- **Devices:** a real Android phone and a real Android tablet (model, Android version and screen size are recorded on the first page of the Testing Evidence document). Earlier development testing also used Android Studio emulator profiles.
- **Backend checks:** the Firebase Console was used independently to confirm that Firestore writes (bookings, slots, cancellations, deletions, registrations) matched what the app showed, and the Firestore Rules Playground was used to confirm that forbidden writes are denied.
- **Offline tests:** Airplane Mode was toggled on the device.
- **Alarm check:** `adb shell dumpsys alarm` was used to confirm scheduled reminders are cancelled and restored by the Booking Reminders switch.
- **Team testing:** each member runs their own pack (section 4) on their own device; integration tests use two phones, one logged in as a client and one as the admin, so live updates can be seen.
- **Evidence:** for each test ID, a screenshot (or short screen recording for real-time behaviour) is placed under the matching section of the Testing Evidence document.
