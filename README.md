# 📱 MMM Social Workers and Associates — Booking System Application

A native Android booking application built for **MMM Social Workers and Associates**, a psychology and social work practice operating across Midrand, Johannesburg, and Centurion, Gauteng, South Africa, with online consultations also offered.

Built for **ITDMA3-34** (Project – Mobile Application and Web Services), Eduvos.

---

## Group Members and Task Allocation

**Group 11** · ITDMA3-34 · Eduvos

| | Member | Student no. | Role and responsibilities |
|:-:|---|---|---|
| 🎨 | **Faith Murau** | EDUV4821116 | **UX/UI Design and Testing** |
| 💻 | **Khauhelo Moqechane** | MD.2022.Q7M4P1 | **Frontend Development, Screen and Navigation Testing** |
| ⚙️ | **Ignatius Mayibongwe Khumalo** | EDUV4960805 | **Backend Development, Database Management, Authentication, Booking Logic Testing, GitHub Repository Management and Final Submission** |

> 🤝 **Shared responsibilities** — all three members take part in **Integration Testing**, **Demonstration Video Production** and **Final Report Preparation**.

### Who tests what

| Member | Test pack | IDs | Tests |
|---|---|---|---|
| Faith Murau | UX/UI: visual consistency, clarity, error and confirmation messages | UX-01 to UX-24 | 24 |
| Khauhelo Moqechane | Screen inventory and navigation flow | NAV-01 to NAV-18 | 18 |
| Ignatius Mayibongwe Khumalo | Database and authentication; booking logic; permissions (Rules Playground) | DB-01 to DB-21, BK-01 to BK-22, PM-01 to PM-18 | 61 |
| All three | End-to-end integration with two phones | INT-01 to INT-12 | 12 |

Every test, its expected result and its recorded result are in [`TESTING.md`](./TESTING.md) (section 4); the matching screenshots are in the Testing Evidence document.

---

## Table of Contents

- [Group Members and Task Allocation](#group-members-and-task-allocation)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Real-Time Updates](#real-time-updates)
- [Booking Rules and Conflict Prevention](#booking-rules-and-conflict-prevention)
- [CRUD Map](#crud-map)
- [Responsive UI (Phone and Tablet)](#responsive-ui-phone-and-tablet)
- [Database Schema (Cloud Firestore)](#database-schema-cloud-firestore)
- [API Integrations](#api-integrations)
- [Security](#security)
- [Performance](#performance)
- [Scalability](#scalability)
- [Logging and Monitoring](#logging-and-monitoring)
- [Backup and Recovery](#backup-and-recovery)
- [Notifications — Precisely Documented](#notifications--precisely-documented)
- [Testing Summary](#testing-summary)
- [Known, Documented Limitations](#known-documented-limitations)
- [Repository Files](#repository-files)
- [Installation](#installation)
- [Project Structure / Blueprint](#project-structure--blueprint)
  - [1. App Identity](#1-app-identity)
  - [2. User Roles](#2-user-roles)
  - [3. Full Project Structure](#3-full-project-structure)
  - [4. Screen-by-Screen Summary](#4-screen-by-screen-summary-20-original--2-added--22-screens)
  - [5. Data Design — Firestore Security Rules (summary)](#5-data-design--firestore-security-rules-summary)
  - [6. Core Business Rules](#6-core-business-rules)
- [Requirements Coverage](#requirements-coverage)

---

## Technology Stack

- **Language:** Java
- **Platform:** Android (minSdk 24, targetSdk 36)
- **Backend:** Firebase — Authentication, Cloud Firestore, Crashlytics — free Spark plan throughout. Firebase Cloud Messaging is configured (via `google-services.json`) but not actively used for push notifications in this version — see [Known Limitations](#known-documented-limitations) below for why, and for what local notifications actually do instead.
- **External API:** Nager.Date public holidays REST API (South Africa) — genuine HTTP integration, called via `HttpURLConnection`, parsed with `org.json`
- **Firestore region:** `africa-south1` (Johannesburg) — chosen for lowest latency to the practice's Gauteng-based clients
- **Testing:** JUnit 4 (61 unit tests) and Espresso (1 instrumented UI test) — see [Testing Summary](#testing-summary)

---

## Architecture

The app follows a client-heavy, serverless architecture: the Android app communicates directly with Firebase's managed services via the Firebase Android SDK, with no custom backend server. One external REST API call (Nager.Date) supplements this for public holiday data.

```
Presentation Layer (22 Activities + XML layouts)
                    ↓
Models (User, Booking, BookedSlot, PracticeInfo)
                    ↓
Business Logic Layer (utils/):
  ValidationUtils, StatusUtils, BookingActions, ThemeManager,
  NotificationHelper, NetworkStatusHelper, SessionCache, AnalyticsHelper,
  HolidayApiClient, CsvExportUtils, TouchTargetUtils, DialogUtils,
  MaxWidthLinearLayout, FirebaseRefs
                    ↓
Firebase Android SDK + Nager.Date REST API (HTTP GET)
                    ↓
Firebase Authentication / Cloud Firestore / Local Notifications / Crashlytics
```

Each Activity talks to Firestore through `FirebaseRefs` (one place for every collection reference). The two operations that must change several documents at once — cancelling/declining/deleting a booking and creating a booking — are not written inline in the screens; they run as single Firestore transactions (`BookingActions.java` and `BookAppointmentActivity.submitBookingTransaction()`).

---

## Real-Time Updates

My Bookings (client) and the Admin Dashboard's booking list both use Firestore's `addSnapshotListener`, not a one-time `.get()` read. This means:

- If an admin confirms or declines a booking while the client has My Bookings open, the status badge updates automatically, with no manual refresh.
- If a client submits a new booking, or an admin marks one paid/completed from another device, the Admin Dashboard's list and summary counts update automatically for any admin currently viewing it.

This is entirely free-tier (no Cloud Functions or Blaze plan required) — `addSnapshotListener` is a standard, always-free part of the Firestore Android SDK. Listeners are registered once when each screen is created (`attachBookingsListener()` in `client/MyBookingsActivity.java` and `admin/AdminDashboardActivity.java`) and explicitly removed in `onDestroy()` to avoid leaks.

---

## Booking Rules and Conflict Prevention

All of these are enforced in `client/BookAppointmentActivity.java` (client-side checks) and, for conflicts, again inside a Firestore transaction (so two people booking at the same moment cannot both succeed).

| Rule | How it works | Where |
|---|---|---|
| **Open days** | The date must fall on one of the days the admin ticked in Practice Information (stored as the structured list `openDays`). If none is saved yet, Monday–Friday applies. | `PracticeInfo.isOpenOn()`, `BookAppointmentActivity.handleDatePicked()` |
| **Public holidays** | South African public holidays are fetched from Nager.Date **per year** (cached by year) and blocked, even on an open weekday. If the API is unreachable the booking is still allowed on the open-day and hours rules, and the client is told holidays could not be checked. | `HolidayApiClient`, `BookAppointmentActivity.ensureHolidaysLoaded()` |
| **Operating hours** | The whole appointment must fit inside opening hours: `start ≥ opening` **and** `start + duration ≤ closing` (so a 60-minute booking at 17:45 is rejected when the practice closes at 18:00). Re-checked when the duration changes and again on submit. | `BookAppointmentActivity.timeFitsOperatingHours()` |
| **Quarter-hour starts** | Appointments start on :00, :15, :30 or :45, matching the 15-minute reservation blocks below. | `BookAppointmentActivity.showTimePicker()` |
| **No overlaps — anyone** | A booking reserves one `bookedSlots` document per 15-minute block it covers (a 60-minute booking reserves 4). The transaction reads every block first and fails if **any** already exists, whether it belongs to the same client or another one. Firestore transactions cannot run queries, so reserving per-block documents is what makes a true overlap check possible. | `BookedSlot.buildBlockIds()`, `BookAppointmentActivity.submitBookingTransaction()` |
| **Each location is its own calendar** | Slot IDs include the location (Midrand / Johannesburg / Centurion) or `online`, so the same time at two different locations is allowed. *(One-line change in `BookedSlot.buildId()` if the practice wants one shared calendar.)* | `BookedSlot.buildId()` |
| **Atomic cancel / decline / delete** | Changing a booking and releasing its slot blocks happen in one transaction — they succeed or fail together, so a failed slot release can never leave a booking cancelled with its time still blocked. Blocks are released only while the booking is still pending/confirmed and the block belongs to that client, so deleting an old cancelled booking can never free a slot someone else has since booked. | `utils/BookingActions.java` |

---

## CRUD Map

The rubric asks for at least two complete functional modules, each with Create, Read, Update and Delete. This is where each operation lives.

### Module 1 — Bookings (full in-app CRUD)

| Operation | What the user does | Implementation |
|---|---|---|
| **Create** | Client submits a booking | `client/BookAppointmentActivity.java` → `submitBookingTransaction()` (writes the booking and its slot blocks atomically) |
| **Read** | Client sees own bookings; admin sees all | `client/MyBookingsActivity.java` → `attachBookingsListener()`; `admin/AdminDashboardActivity.java` → `attachBookingsListener()`; `admin/BookingDetailActivity.java` → `loadBooking()` |
| **Update** | Admin confirms / marks paid / marks completed; client or admin cancels | `admin/BookingDetailActivity.java` → `updateStatus()`, `updatePaidStatus()`; cancel/decline → `utils/BookingActions.cancelBooking()` (called from `MyBookingsActivity.performCancellation()` and `BookingDetailActivity.declineBooking()`) |
| **Delete** | Admin permanently removes a booking record (separate from Decline, which keeps the record for history) | `admin/BookingDetailActivity.java` → `deleteBooking()` → `utils/BookingActions.deleteBooking()` (confirmation dialog first; admin-only, enforced by Security Rules) |

### Module 2 — User accounts and profile

| Operation | What the user does | Implementation |
|---|---|---|
| **Create** | Register (Auth account + profile document, with rollback if the profile write fails) | `auth/RegisterActivity.java` → `createFirebaseAccount()`, `rollbackAuthAccount()` |
| **Read** | View own profile details | `settings/SettingsActivity.java` → `loadUserData()` |
| **Update** | Edit name/phone; change password; change email; booking-reminders preference | `SettingsActivity.saveAccountChanges()`, `settings/ChangePasswordActivity.java`, `settings/ChangeEmailActivity.java`, `SettingsActivity.onRemindersToggled()` |
| **Delete** | Client requests deletion in-app; the practice deletes the account | `SettingsActivity` → "Delete My Account" explains the process, then opens a pre-addressed email to the practice; the admin removes the Auth account and profile in the Firebase Console. *By design:* the Firebase client SDK cannot delete another user's Authentication account without Cloud Functions (paid Blaze plan). |

### Supporting module — Practice Information (admin)

Read (every client screen) and Update (`admin/PracticeInfoActivity.java` → `savePracticeInfo()`). It is a single shared settings document, so it deliberately has no create/delete.

---

## Responsive UI (Phone and Tablet)

The app renders correctly across small phones, large phones, 7-inch tablets, and 10-inch tablets, using Android's own resource-qualifier system rather than any hardcoded screen dimensions.

### The mechanism

Two files drive every screen's tablet behaviour:

- **`app/src/main/res/values/dimens.xml`** — defines the default (phone) values: `content_max_width` (9999dp — effectively unconstrained on a phone) and `screen_horizontal_padding` (18dp), plus 13 named text-size dimens (`text_size_10` through `text_size_26`) matching every font size used across the app.
- **`app/src/main/res/values-sw600dp/dimens.xml`** — Android automatically applies this file instead of the default _only_ on screens with a smallest width of 600dp or more (7-inch tablets and larger), with zero code needed to detect device type. Here, `content_max_width` becomes `640dp`, `screen_horizontal_padding` becomes `48dp`, and every text-size dimen is scaled up roughly 1.3x for comfortable reading at arm's length on a larger screen.

Every layout file references these dimens by name (e.g. `android:textSize="@dimen/text_size_14"`, `android:padding="@dimen/screen_horizontal_padding"`) rather than hardcoding a raw `sp`/`dp` value, so the exact same layout XML renders correctly on both device classes without any per-device branching in Java.

### `MaxWidthLinearLayout` — the width-capping mechanism

**`app/src/main/java/com/mmm/bookingapp/utils/MaxWidthLinearLayout.java`** is a small custom `LinearLayout` subclass (one overridden method, `onMeasure()`) that caps its own width at `@dimen/content_max_width` and centres itself when the available screen is wider than that cap. On a phone, the cap is high enough to never engage — content fills the screen exactly as it always did. On a tablet, content is capped at a comfortable reading width and centred, rather than stretching form fields and buttons edge-to-edge across a 10-inch screen.

### Vertical centering on fixed-content screens

For screens with short, fixed content (forms and dashboards, not scrolling lists), three patterns keep content vertically balanced on a tall/tablet screen rather than pinned to the top with empty space below:

- **Single-block centering** (`activity_welcome.xml`, `activity_home.xml`): the whole screen's `MaxWidthLinearLayout` uses `layout_height="match_parent"` with `android:gravity="center_vertical"`, inside a `ScrollView` with `android:fillViewport="true"`.
- **Split-header centering** (`activity_login.xml`, `activity_book_appointment.xml`, `activity_settings.xml`, `activity_change_password.xml`, `activity_change_email.xml`, `activity_contact.xml`, `activity_booking_detail.xml`, `activity_practice_info.xml`, `activity_admin_analytics.xml`): the header/back-button row is pinned to the top as its own fixed block, and only the form content below it sits inside a `layout_weight="1"` wrapper with `gravity="center_vertical"`.
- **ConstraintLayout centering** (`activity_register.xml`): one content column constrained on all sides with `layout_constraintWidth_max="@dimen/content_max_width"` and a 0.5 vertical bias, inside a `ScrollView` with `fillViewport="true"`.

List-based and long-content screens (`activity_admin_dashboard.xml`, `activity_my_bookings.xml`, `activity_services.xml`, `activity_faq.xml`, `activity_about.xml`, `activity_privacy_policy.xml`, `activity_terms.xml`, `activity_booking_confirmation.xml`) deliberately use `MaxWidthLinearLayout` for horizontal width-capping only, with no vertical centering — a growing list or a long block of text should fill the screen naturally, not jump to the middle as it grows.

### Orientation support

Every screen's `ScrollView`/`NestedScrollView` wrapper uses `fillViewport="true"`, so content that doesn't fill the available height (portrait, tall screen) centres correctly, while content taller than the available height (landscape, short screen) scrolls normally instead of being clipped — the same mechanism handles both orientations without separate `layout-land/` resource files.

---

## Database Schema (Cloud Firestore)

### `users`

`uid, name, email, phone, role ("client"|"admin"), emailVerified, notificationsEnabled, marketingOptIn, createdAt`

### `bookings`

`bookingId, clientId, service, consultationType ("online"|"in-person"), location, duration (30|45|60), date, time, additionalInfo, status ("pending"|"confirmed"|"cancelled"|"completed"), paid, createdAt`

### `bookedSlots` (conflict-checking only — one document per 15-minute block)

`slotId (date_time_location|online), clientId, date, time, consultationType, location, duration`

`clientId` is the only identifying field, kept so the Security Rules can let only the owning client (or an admin) create or delete a block. It is never displayed. No name, phone, service or notes are stored here.

### `practiceInfo` (single document, id `"main"`)

`phone, email, whatsappLink, locations, openDays (structured list, e.g. ["MONDAY", …]), operatingDays (readable text generated from openDays, shown on Contact), operatingHours {start, end}, crisisLineNote`

---

## API Integrations

- **Firebase SDK** — Authentication, Firestore (including real-time listeners, transactions and `count()` aggregation queries), Crashlytics
- **Nager.Date Public Holidays API** — `GET https://date.nager.at/api/v3/PublicHolidays/{year}/ZA`, called from `utils/HolidayApiClient.java` (per year, cached), blocking bookings on South African public holidays even on an otherwise open day

---

## Security

Firestore Security Rules enforce, server-side, exactly what this README claims — the full, actual rules deployed to this project's Firebase Console are committed in [`firestore.rules.md`](./firestore.rules.md), so there is one single source of truth rather than rules described in prose that could drift out of sync with what's actually enforced.

What those rules enforce (verified during testing, not just hidden in the app's UI):

| Collection | Rule |
|---|---|
| `users` | A user can read only their own document (an admin can read any). A new profile can **only be created with `role == "client"`**, so nobody can register themselves as an admin. A user can update their own document but can **never change `role`**. |
| `bookings` | A client can read only their own bookings (an admin can read all). A booking can only be **created** by its owner, as **pending and unpaid**. A client can **update** their own booking only to cancel it (the only field they may touch is `status`, and only to `"cancelled"`); an admin can update anything. Only an **admin can delete**. |
| `bookedSlots` | Any signed-in user can read (needed to check availability; no personal data is stored). A block can only be **created** by the client it is created for (`clientId` must match the requester), and only deleted by that client or an admin. Blocks can **never be updated**. |
| `practiceInfo` | Publicly readable (so Contact works before login); admin-only to write. |

Admin status itself is determined server-side by looking up the requester's own `users/{uid}` document and checking `role == "admin"` — a client can never simply claim to be an admin from the app. Admin accounts exist only because an administrator created them manually in the Firebase Console.

---

## Performance

- The Admin Analytics screen uses Firestore's server-side `count()` aggregation queries rather than downloading full documents to count client-side, keeping analytics fast regardless of how large the `bookings`/`users` collections grow.
- Real-time listeners (see above) replace polling-on-resume, reducing redundant Firestore reads — data only re-fetches when it actually changes.
- All custom icons are vector drawables (`.xml`), not raster images, keeping the app's asset footprint small and icons sharp across every screen density.
- `RecyclerView` is used for every scrolling list. *One honest trade-off:* the Admin Dashboard places its `RecyclerView` inside a `NestedScrollView` (so the overview card, search and filters scroll together with the list), which means that list renders all of its rows instead of recycling them. This is fine at one practice's booking volume; it would be restructured (a single `RecyclerView` with a header) if the list grew to thousands of rows.

---

## Scalability

- Firestore is a fully managed, serverless NoSQL database that scales horizontally by design — this app does not manage or provision any database infrastructure itself.
- The package structure (`auth/`, `client/`, `admin/`, `settings/`, `utils/`, `models/`, `adapters/`) is additive: a new screen or role-specific feature is added as a new file in the relevant package, without needing to restructure existing code.
- `FirebaseRefs.java` centralises every Firestore collection reference in one file — a future schema change touches one class, not every screen that reads from it. `BookingActions.java` and `StatusUtils.java` likewise hold the booking-change and status logic in one place each.
- Conflict prevention uses per-block slot documents inside a transaction, so correctness does not depend on how many bookings exist.

---

## Logging and Monitoring

- Firebase Crashlytics — automatic crash/error capture, confirmed active
- Explicit `Log.d/i/w/e` calls in `LoginActivity`, `RegisterActivity`, `BookAppointmentActivity` and `AdminDashboardActivity` — login attempts, registration outcomes and rollbacks, booking submissions, holiday/overlap rejections, filter/search actions, CSV export results
- **Privacy:** log messages never contain email addresses or other personal details; where an account must be identified the log uses the Firebase user ID only.

---

## Backup and Recovery

- Firestore data protected by Google's managed infrastructure redundancy
- Local theme preference included in Android Auto Backup via `backup_rules.xml`/`data_extraction_rules.xml`
- Manual export: Admin Dashboard's "Export CSV" button exports the currently-filtered booking list and opens Android's share sheet — a genuine, demoable manual backup mechanism (`utils/CsvExportUtils.java`)
- Security rules themselves are version-controlled in [`firestore.rules.md`](./firestore.rules.md), so a previous rule set can always be recovered from Git history if a rule change ever needed reverting

---

## Notifications — Precisely Documented

This app uses **local, on-device notifications only** (`NotificationManager` + `AlarmManager`), not Firebase Cloud Messaging push, despite FCM being configured via `google-services.json`. Specifically:

- **Booking confirmation:** an immediate local notification fires the moment a client's own booking submission succeeds — this requires no server round-trip, since it's the same device notifying itself about its own just-completed action.
- **Appointment reminder:** a client receives **exactly one** reminder notification per booking, delivered locally on-device the **evening before the appointment, at 18:00**, via a single scheduled `AlarmManager` alarm. It requires no connectivity or server involvement to fire later.
- **The Booking Reminders switch is enforced.** The choice is saved on the account (`users/{uid}.notificationsEnabled`) and cached on the device per account. With the switch off, **neither** notification is created. Switching off also cancels the reminders already scheduled for the user's active bookings, and switching back on reschedules them. (`utils/NotificationHelper.java`, `settings/SettingsActivity.java` → `onRemindersToggled()`)
- Both notifications use `NotificationCompat.BigTextStyle`, so a longer service name expands fully on long-press rather than being truncated with an ellipsis.

> **What genuinely isn't implemented:** a notification triggered by one person's action (e.g. an admin confirming a booking) and delivered instantly to a _different_ device (the client's) is not implemented, and cannot be, without a server-side trigger. See Known Limitations below.

---

## Testing Summary

| Layer | Result |
|---|---|
| **Unit tests** — `ValidationUtilsTest` (37) + `BookingRulesTest` (24) | **61 tests, all passing.** Cover every validation rule, the 15-minute slot-block logic that prevents overlaps, open-day and hours rules, and status handling. |
| **Instrumented UI test** — `WelcomeActivityTest` | 1 Espresso test on the Welcome screen — passing |
| **Hardening verification** (IDs PA1–PF9) | **37 of 37 passed** on a real Android phone: security rules, atomic cancel/delete, booking-rule enforcement, reminder switch, registration rollback, validation |
| **Manual test plan** — functional (F1–F25), usability (U1–U12), edge cases (E1–E8) | **45 of 45 passed** on the real phone and tablet |
| **Core test plan total** | **143 of 143 passed** |
| **Team test plans** — UX (24), navigation (18), database (21), booking logic (22), permissions (18), integration (12) | 115 tests assigned to the three group members; each member's results are recorded in `TESTING.md` section 4 as they are run |
| **Evidence** | Testing Evidence document — one screenshot box per test ID, referenced from `TESTING.md` |

See [`TESTING.md`](./TESTING.md) for every test, its expected result and its recorded result, and the defect log (27 defects found and fixed during development and the final audit).

---

## Known, Documented Limitations

- **Admin-to-client push notifications** (e.g. client instantly notified the moment an admin confirms a booking) require Firebase Cloud Functions, which requires the paid Blaze plan. This app remains on the free Spark plan. Status changes ARE reflected live via Firestore's `addSnapshotListener` (see [Real-Time Updates](#real-time-updates)) the moment the client's My Bookings screen is open — but no notification is pushed if the app is closed or backgrounded at the time.
- **Offline mode is precise, not universal**: Contact, Settings, and previously-loaded My Bookings data work offline via Firestore's cache. Login, booking submission, and cancellation genuinely require connectivity, since they depend on Firebase Authentication and Firestore transactions, neither of which can operate offline by design. When the role lookup fails on app start (for example first launch offline with no cache), the splash screen falls back to the role remembered on the device from the last login (`utils/SessionCache.java`); that cached role only chooses which screen opens and grants no access.
- **The Admin Analytics trend estimate is a simple heuristic, not machine learning.** It calculates a projection from the most recent week-over-week growth rate. This is explicitly labelled in the UI itself, to avoid overclaiming AI/predictive-analytics capability the app does not have.
- **Account recovery when both password and email access are lost simultaneously is not self-service.** This is universal to every email/password login system — password recovery works by emailing a reset link, so losing access to both at once leaves no automated way back in, by design. A client can change their registered email proactively, while still logged in and while they still know their password, via Settings → Change Email (`settings/ChangeEmailActivity.java`, using Firebase's `verifyBeforeUpdateEmail()`, which requires clicking a confirmation link sent to the new address). For the genuine worst case, the documented fallback is contacting the practice directly (phone/WhatsApp, both shown on the Contact screen even when logged out), so an admin can assist manually via the Firebase Console.
- **Account deletion is fulfilled by the practice**, not instantly in-app: the client requests it (an explainer dialog, then a pre-addressed email) and an admin removes the Authentication account and profile in the Firebase Console. The Firebase client SDK cannot delete another user's Authentication account without Cloud Functions.
- **Clients cancel and rebook rather than reschedule.** There is no in-app edit-a-booking screen; cancelling frees the slot and the client books the new time.
- **Each location (and Online) is treated as its own calendar**, so the same time at two locations can both be booked. If the practice has a single practitioner across all locations, this is a one-line change in `BookedSlot.buildId()`.
- **Public holiday check depends on an external API.** If Nager.Date is unreachable, bookings are still allowed on the open-day and hours rules and the client is told holidays could not be checked. This is a deliberate availability-over-strictness decision.
- **Booking Reminders preference is cached per device.** It is saved on the account, but the on-device copy refreshes when Settings is opened; a second phone signed into the same account keeps its own cached value until then.
- **Older test slots.** Slot documents created before the security hardening have no `clientId`; a client cannot cancel an old booking whose slot lacks it (an admin can decline or delete it). This only affects test data created before the hardening.

---

## Repository Files

- `README.md` — this file
- `USER_GUIDE.md` — end-user instructions for both Client and Admin roles
- `TESTING.md` — automated test suite, the full manual test plan with recorded results, and the defect log
- `firestore.rules.md` — the actual, deployed Firestore Security Rules, kept in version control as the single source of truth referenced throughout this README
- `docs/Testing_Evidence.pdf` — screenshot evidence for each test ID in `TESTING.md` (exported from the Word document)

---

## Installation

1. Clone the repository
2. Open in Android Studio (Java, Gradle)
3. Place your own `google-services.json` in `app/`
4. Publish the rules from `firestore.rules.md` in the Firebase Console (Firestore → Rules → Publish)
5. Sync Gradle, build, run on a device or emulator (API 24+)
6. Create the first admin by registering normally, then changing that user's `role` to `"admin"` in the Firebase Console (admin accounts cannot be created from inside the app)

---

# Project Structure / Blueprint

## 1. App Identity

- **Display name:** MMM Social Workers & Associates
- **Language:** Java, exclusively
- **Backend:** Firebase (Auth, Firestore, Crashlytics), Spark (free) plan
- **GitHub:** `github.com/IgnatiusKhumalo/MMM-Booking-App` (public)
- **Colours (Light Mode, locked):** white/beige backgrounds, dark green (`#1F6F63`) primary buttons, light blue highlights, black/dark grey text
- **Colours (Dark Mode, locked):** `DARK_BACKGROUND` (24,24,24), `DARK_SURFACE` (40,40,40), `DARK_GREEN` (31,111,99), `LIGHT_BLUE` (120,200,210), `WHITE` (255,255,255), `LIGHT_GREY` (200,200,200), `BORDER` (70,70,70)
- Theme preference stored locally (SharedPreferences via `ThemeManager`), applied at startup via `MMMApplication`

## 2. User Roles

| Role   | Creation                                                                                          | Access                                                                                                   |
| ------ | ------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| Client | Self-registers, role auto-set `"client"` (enforced by the Security Rules)                          | Book, view own bookings, cancel, manage settings, change password/email, request account deletion        |
| Admin  | Manually provisioned via Firebase Console — no admin registration path exists anywhere in the app | Full booking management (confirm/decline/paid/complete/delete), Practice Information editing, analytics |

## 3. Full Project Structure

```
MMM-Booking-App/
├── README.md
├── USER_GUIDE.md
├── TESTING.md
├── firestore.rules.md (actual deployed Firestore rules)
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/ (Gradle wrapper and version catalog)
├── docs/
│   └── Testing_Evidence.pdf (screenshot evidence, exported from the Word document)
└── app/
    ├── build.gradle.kts
    ├── .gitignore
    ├── google-services.json
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml (all 22 activities + reminder receiver + FileProvider)
        │   ├── java/com/mmm/bookingapp/
        │   │   ├── MainActivity.java (Screen 1 - Splash, routes by saved session/role)
        │   │   ├── MMMApplication.java (applies saved theme at startup)
        │   │   │
        │   │   ├── auth/
        │   │   │   ├── WelcomeActivity.java (Screen 2)
        │   │   │   ├── RegisterActivity.java (Screen 3 - rolls back Auth account if profile write fails)
        │   │   │   ├── LoginActivity.java (Screen 4)
        │   │   │   └── ForgotPasswordActivity.java (Screen 5)
        │   │   │
        │   │   ├── client/
        │   │   │   ├── HomeActivity.java (Screen 6)
        │   │   │   ├── ServicesActivity.java (Screen 7)
        │   │   │   ├── BookAppointmentActivity.java (Screen 8 - open days, hours, holidays, block reservation)
        │   │   │   ├── BookingConfirmationActivity.java (Screen 9)
        │   │   │   ├── MyBookingsActivity.java (Screen 10 - real-time listener, atomic cancel)
        │   │   │   └── ContactActivity.java (Screen 11)
        │   │   │
        │   │   ├── settings/
        │   │   │   ├── SettingsActivity.java (Screen 12)
        │   │   │   ├── ChangePasswordActivity.java (Screen 13)
        │   │   │   ├── AboutActivity.java (Screen 14)
        │   │   │   ├── PrivacyPolicyActivity.java (Screen 15)
        │   │   │   ├── TermsActivity.java (Screen 16)
        │   │   │   ├── FAQActivity.java (Screen 17)
        │   │   │   └── ChangeEmailActivity.java (Screen 22)
        │   │   │
        │   │   ├── admin/
        │   │   │   ├── AdminDashboardActivity.java (Screen 18 - real-time listener)
        │   │   │   ├── BookingDetailActivity.java (Screen 19 - confirm/decline/paid/complete/delete)
        │   │   │   ├── PracticeInfoActivity.java (Screen 20 - open-day checkboxes, validated hours)
        │   │   │   └── AdminAnalyticsActivity.java (Screen 21 - counts, % change, bar charts, trend)
        │   │   │
        │   │   ├── models/
        │   │   │   ├── User.java (mirrors users collection)
        │   │   │   ├── Booking.java (mirrors bookings collection)
        │   │   │   ├── BookedSlot.java (one 15-minute block; buildId / buildBlockIds)
        │   │   │   └── PracticeInfo.java (practice settings, openDays, hours helpers)
        │   │   │
        │   │   ├── adapters/
        │   │   │   ├── BookingListAdapter.java (shared by My Bookings + Admin Dashboard)
        │   │   │   ├── ServiceListAdapter.java (Services screen)
        │   │   │   └── FAQListAdapter.java (FAQ screen, expandable rows)
        │   │   │
        │   │   └── utils/
        │   │       ├── ValidationUtils.java (input rules)
        │   │       ├── StatusUtils.java (null-safe status + badge colour/label)
        │   │       ├── BookingActions.java (atomic cancel / delete transaction)
        │   │       ├── FirebaseRefs.java (every Firestore collection reference)
        │   │       ├── NetworkStatusHelper.java (connectivity checks + offline banners)
        │   │       ├── SessionCache.java (remembers role for offline splash routing)
        │   │       ├── NotificationHelper.java (reminder switch enforced, BigTextStyle)
        │   │       ├── ReminderBroadcastReceiver.java (fires the scheduled reminder)
        │   │       ├── HolidayApiClient.java (real external REST API call)
        │   │       ├── AnalyticsHelper.java (date buckets and % change)
        │   │       ├── CsvExportUtils.java (manual backup/export mechanism)
        │   │       ├── ThemeManager.java (Dark Mode preference)
        │   │       ├── DialogUtils.java (shared confirmation dialogs)
        │   │       ├── TouchTargetUtils.java (accessibility - 48dp touch targets)
        │   │       └── MaxWidthLinearLayout.java (tablet width capping)
        │   │
        │   └── res/
        │       ├── layout/ (26 files)
        │       │   ├── activity_main.xml (Screen 1 - Splash)
        │       │   ├── activity_welcome.xml (Screen 2)
        │       │   ├── activity_register.xml (Screen 3)
        │       │   ├── activity_login.xml (Screen 4)
        │       │   ├── activity_forgot_password.xml (Screen 5)
        │       │   ├── activity_home.xml (Screen 6)
        │       │   ├── activity_services.xml (Screen 7)
        │       │   ├── activity_book_appointment.xml (Screen 8)
        │       │   ├── activity_booking_confirmation.xml (Screen 9)
        │       │   ├── activity_my_bookings.xml (Screen 10)
        │       │   ├── activity_contact.xml (Screen 11)
        │       │   ├── activity_settings.xml (Screen 12)
        │       │   ├── activity_change_password.xml (Screen 13)
        │       │   ├── activity_about.xml (Screen 14)
        │       │   ├── activity_privacy_policy.xml (Screen 15)
        │       │   ├── activity_terms.xml (Screen 16)
        │       │   ├── activity_faq.xml (Screen 17)
        │       │   ├── activity_admin_dashboard.xml (Screen 18)
        │       │   ├── activity_booking_detail.xml (Screen 19)
        │       │   ├── activity_practice_info.xml (Screen 20)
        │       │   ├── activity_admin_analytics.xml (Screen 21)
        │       │   ├── activity_change_email.xml (Screen 22)
        │       │   ├── dialog_cancel_confirmation.xml (cancel-booking confirmation dialog)
        │       │   ├── item_booking_card.xml (booking row (My Bookings + Admin Dashboard))
        │       │   ├── item_faq_card.xml (FAQ expandable row)
        │       │   └── item_service_card.xml (service row)
        │       ├── values/
        │       │   ├── colors.xml (light palette)
        │       │   ├── dimens.xml (phone sizes + 13 text-size dimens)
        │       │   ├── ic_launcher_background.xml
        │       │   ├── strings.xml (all text)
        │       │   └── themes.xml (themes and shared styles)
        │       ├── values-night/ (Dark Mode)
        │       │   ├── colors.xml
        │       │   └── themes.xml
        │       ├── values-sw600dp/ (tablet sizes)
        │       │   └── dimens.xml (content width cap, padding, larger text)
        │       ├── drawable/ (icons, backgrounds, logo)
        │       │   ├── bg_card.xml
        │       │   ├── bg_card_bordered.xml
        │       │   ├── bg_circle_button.xml
        │       │   ├── bg_dropdown_field.xml
        │       │   ├── bg_input_plain.xml
        │       │   ├── bg_pill_dark.xml
        │       │   ├── bg_pill_outline.xml
        │       │   ├── bg_status_pill.xml
        │       │   ├── ic_arrow_right.xml
        │       │   ├── ic_calendar_check.xml
        │       │   ├── ic_calendar_clock.xml
        │       │   ├── ic_chevron_down.xml
        │       │   ├── ic_download.xml
        │       │   ├── ic_email.xml
        │       │   ├── ic_eye.xml
        │       │   ├── ic_eye_off.xml
        │       │   ├── ic_heart_filled.xml
        │       │   ├── ic_launcher_background.xml
        │       │   ├── ic_launcher_foreground.xml
        │       │   ├── ic_lock.xml
        │       │   ├── ic_logout_arrow.xml
        │       │   ├── ic_phone_line.xml
        │       │   ├── ic_search.xml
        │       │   ├── ic_settings_sliders.xml
        │       │   └── mmm_logo.png (practice logo)
        │       ├── xml/
        │       │   ├── backup_rules.xml (Auto Backup (theme preference))
        │       │   ├── data_extraction_rules.xml (device-transfer rules)
        │       │   └── file_paths.xml (FileProvider paths for CSV export)
        │       └── mipmap-anydpi-v26/, mipmap-hdpi/, mipmap-mdpi/, mipmap-xhdpi/, mipmap-xxhdpi/, mipmap-xxxhdpi/ (launcher icons)
        │
        ├── test/java/com/mmm/bookingapp/
        │   ├── utils/ValidationUtilsTest.java (37 JUnit tests)
        │   └── models/BookingRulesTest.java (24 JUnit tests)
        │
        └── androidTest/java/com/mmm/bookingapp/auth/
            └── WelcomeActivityTest.java (Espresso instrumented test)
```

## 4. Screen-by-Screen Summary (20 original + 2 added = 22 screens)

**Screen 1 — Splash:** Full logo shown edge-to-edge, practice name heading, loading spinner; checks the saved session and routes to Welcome, Home, or Admin Dashboard automatically. If the role cannot be fetched (offline, no cache) it uses the role remembered from the last login.

**Screen 2 — Welcome:** Landing page after Splash. Full logo, heading/tagline, two rounded-pill buttons (Register/Login), reassurance footer line.

**Screen 3 — Register:** Card-based layout (Personal Details, Account Security, Consent). Full Name / Email / Confirm Email / Phone (+27, 9 digits) / Password / Confirm Password with show/hide toggles. Every problem is shown next to its field; the password-rules line turns green when valid and red/bold if submitted invalid. Compulsory consent checkbox (tappable links to About/Privacy Policy/Terms) and an optional subscribe checkbox. If the profile write fails after the Auth account is created, the Auth account is deleted again.

**Screen 4 — Login:** Circular logo badge, "Welcome Back" heading, bordered card-style Email/Password fields with icons, show/hide password toggle. Empty fields are flagged on the field; offline attempts are blocked with a clear message; unverified emails are blocked with a resend option. On success a flag is passed so the destination shows a "Logged in successfully" confirmation.

**Screen 5 — Forgot Password:** Sends a Firebase-managed reset email, deliberately identical confirmation message regardless of whether the email is registered.

**Screen 6 — Home:** Header with first-name greeting + circular Logout icon (Cancel/Log Out confirmation). Branding card, "Next Appointment" card with Book Appointment button, 2×2 card grid, footer support bar. Double-press-back-to-exit; never signs the user out.

**Screen 7 — Services:** Lists the practice's 7 real services, informational only.

**Screen 8 — Book Appointment:** Service / consultation type / location / duration / date / time. Enforces open days, public holidays, operating hours including the appointment's end time, quarter-hour starts, and no overlaps with any other booking; offline-guarded.

**Screen 9 — Booking Confirmation:** Summary of the submitted booking, status shown as Pending.

**Screen 10 — My Bookings:** Real-time list. Cancel requires confirmation and is atomic. Offline banner and booking-history summary.

**Screen 11 — Contact:** Practice phone/email/WhatsApp, locations, readable open days and hours, crisis-line note, all read live from `practiceInfo`.

**Screen 12 — Settings:** Card-based layout. Edit name/phone, Change Password, Change Email, Booking Reminders and Dark Mode switches (each with a confirmation toast), About/Privacy/Terms/Contact/FAQ, Delete My Account (explainer dialog first), one Logout (confirmation dialog), and an admin-only Practice Administration card.

**Screen 13 — Change Password:** Requires re-authentication before allowing a change.

**Screens 14–17 — About / Privacy Policy / Terms / FAQ:** Static content screens.

**Screen 18 — Admin Dashboard:** Real-time booking list and summary counts, one proportional status bar, search, Status/Location/Date filters, CSV export, link to analytics. Null-safe status handling. Double-press-back-to-exit; Logout confirms first.

**Screen 19 — Booking Detail (Admin):** Confirm / Decline / Mark Paid / Mark Completed, plus a separate **Delete Booking** (permanent) — Decline and Delete each ask for confirmation and run atomically.

**Screen 20 — Practice Information (Admin):** Edits the shared `practiceInfo` document: contact details, open-day checkboxes (which the booking screen enforces), validated opening/closing times, crisis note.

**Screen 21 — Admin Analytics:** Time-bucketed counts with % change plus a bar-chart comparison (Today/Week/Month/Year) for Bookings Created and New Registrations, and a simple, explicitly-labelled trend estimate — not AI or machine learning.

**Screen 22 — Change Email:** Re-authenticates with the current password, then sends a confirmation link to the new address; the login email changes only once that link is clicked.

## 5. Data Design — Firestore Security Rules (summary)

- `users/{uid}`: owner/admin read; create only as `role == "client"`; `role` can never be changed by the owner
- `bookings/{bookingId}`: owner/admin read; owner creates as pending/unpaid; owner may only cancel (`status` → `"cancelled"`); admin may update anything; admin-only delete
- `bookedSlots/{slotId}`: any signed-in user reads; created only for the requester's own `clientId`; deleted only by that client or an admin; never updated
- `practiceInfo/main`: publicly readable; admin-only write

Full rules: [`firestore.rules.md`](./firestore.rules.md).

## 6. Core Business Rules

- Open days come from Practice Information (default Monday–Friday); opening hours default to 08:00–18:00 and are admin-editable
- An appointment must start within opening hours, start on the quarter hour, and **finish** by closing time
- South African public holidays are also blocked (Nager.Date API, per year), on top of the open-day and hours rules
- No two appointments may overlap at the same location (or both online), whoever the clients are
- Consultation Type drives Location's visibility (hidden + explained for Online, required for In-Person)
- Booking status always starts as `"pending"` and `paid` as `false` (enforced by the Security Rules)
- A client cancelling, or an admin declining, sets the booking to `"cancelled"` and frees its slots in one transaction; only an admin can permanently delete a booking
- Booking submission, cancellation, and a fresh login all require live connectivity
- Booking Reminders off means no booking notifications at all, and cancels already-scheduled reminders
- Logout (from Home, Admin Dashboard or Settings) always requires explicit confirmation before signing out; back-pressing on a root screen never logs the user out
- Account deletion always shows an explanatory dialog before opening the pre-addressed email; the practice completes the deletion

---

## Requirements Coverage

Full mapping of this project's implementation against the module's stated Application Requirements, with the specific file(s) responsible for each. The percentage shows how completely each requirement is delivered; anything below 100% states exactly what remains.

| Requirement | Status | Note and Reference |
| --- | --- | --- |
| User Authentication | 100% Implemented | Firebase Authentication (email/password), with an email-verification gate before login. `auth/LoginActivity.java`, `auth/RegisterActivity.java` (with Auth-account rollback if the profile write fails), `auth/ForgotPasswordActivity.java`. Session persists across app restarts (`MainActivity.java`). |
| Role-Based Access | 100% Implemented | Roles are enforced **server-side**: a new profile can only be created as `"client"` and `role` can never be changed by the user (`firestore.rules.md`, `users` rule). Admin accounts are created manually in the Firebase Console. Routing by role: `LoginActivity.routeByRole()`, `MainActivity.checkLoginStateAndRoute()`. |
| Database Integration | 100% Implemented | Cloud Firestore, 4 collections (`users`, `bookings`, `bookedSlots`, `practiceInfo`). All collection references centralised in `utils/FirebaseRefs.java`. |
| CRUD Operations | 100% Implemented | Two complete modules — see the [CRUD Map](#crud-map): Bookings (create/read/update/delete in-app) and User accounts (create/read/update in-app; delete requested in-app and fulfilled by the admin by design). |
| API Integration | 100% Implemented | Two integrations: Firebase SDK for all backend communication, and a genuine external REST call to the Nager.Date public-holidays API — `utils/HolidayApiClient.java`, used by `client/BookAppointmentActivity.java` → `ensureHolidaysLoaded()`. |
| Input Validation | 100% Implemented | `utils/ValidationUtils.java` — name, email (format regex), phone, password rules, password match, email match; operating-hours validation in `admin/PracticeInfoActivity.java`; booking date/time/overlap rules in `BookAppointmentActivity.java`. Backed by 61 unit tests. |
| Error Handling | 100% Implemented | Inline field errors on Register/Login/Practice Information; offline guards before every network-dependent action (`utils/NetworkStatusHelper.java`); null-safe status handling (`utils/StatusUtils.java`); clear messages for slot-taken, past-closing, closed-day and holiday cases. |
| Security | 100% Implemented | Server-side Firestore Security Rules committed in `firestore.rules.md` and deployed; role self-assignment, client edits to `status`/`paid`, and slot tampering are blocked (verified in testing, IDs PA1–PA3). |
| Responsive UI | 100% Implemented | Phone and tablet, portrait and landscape: `utils/MaxWidthLinearLayout.java`, `res/values/dimens.xml`, `res/values-sw600dp/dimens.xml` — see the [Responsive UI](#responsive-ui-phone-and-tablet) section. |
| Navigation | 100% Implemented | 22 screens with a documented rule (back arrow on every screen except the root screens Welcome, Home and Admin Dashboard); double-press-back-to-exit on Home/Admin Dashboard; stack cleared on login (`FLAG_ACTIVITY_CLEAR_TASK`). |
| Search and Filtering | 100% Implemented | Admin Dashboard: free-text search (service/location/type/status/notes) combined with Status/Location/Date filters — `AdminDashboardActivity.applyFilters()`, `matchesSearch()`. |
| Notifications | 90% Implemented | Local notifications fully working and obeying the Booking Reminders switch: channel, Android 13+ permission, immediate confirmation, evening-before reminder, cancellation of reminders — `utils/NotificationHelper.java`. Server-triggered admin-to-client push is **not** implemented (needs the paid Blaze plan) — see Known Limitations. |
| Offline Functionality | 100% Implemented | Real connectivity checks before login, booking and cancellation; Firestore's offline cache serves previously-loaded data; offline banners; offline-safe splash routing (`utils/NetworkStatusHelper.java`, `utils/SessionCache.java`). Limits documented above. |
| Real-Time Features | 100% Implemented | `addSnapshotListener` on both `MyBookingsActivity` and `AdminDashboardActivity`. |
| Data Visualisation | 100% Implemented | Admin Dashboard proportional status bar (`updateAnalyticsBars()`); Admin Analytics bar charts for bookings and registrations (`AdminAnalyticsActivity.updateChartBars()`) — plain `View` weights, no charting library. |
| Accessibility | 100% Implemented | `contentDescription` on icon-only controls; `utils/TouchTargetUtils.java` expands the two compact Admin Dashboard icon buttons to the 48dp touch-target guideline; full Dark Mode palette (`res/values-night/`); text sizes scale for tablets. |
| Performance | 100% Implemented | `count()` aggregation for analytics, real-time listeners instead of polling, vector icons, `RecyclerView` lists. One documented trade-off on the Admin Dashboard list (see [Performance](#performance)). |
| Testing | 90% Implemented | Automated: 61 unit tests (all passing) + 1 Espresso test. Manual: the 37 hardening tests and the 45-test manual plan (functional, usability, edge cases) all passed on real devices, with evidence in the Testing Evidence document. Remaining 10%: the 115 team test plans (UX, navigation, database, booking logic, permissions, integration) assigned to the three members — see `TESTING.md` section 4. |
| Documentation | 100% Implemented | This README, `USER_GUIDE.md`, `TESTING.md`, `firestore.rules.md`, and code comments describing each class's behaviour. |
| User Guide | 100% Implemented | `USER_GUIDE.md` — separate plain-language instructions for the Client and Admin roles, plus a troubleshooting table. |
| Backup and Recovery | 100% Implemented | Firestore managed redundancy; Android Auto Backup of the theme preference (`res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml`); manual CSV export (`utils/CsvExportUtils.java`); security rules version-controlled. |
| Logging and Monitoring | 100% Implemented | Firebase Crashlytics; explicit `Log.d/i/w/e` calls with no personal data in `LoginActivity`, `RegisterActivity`, `BookAppointmentActivity`, `AdminDashboardActivity`. |
| Scalability | 100% Implemented | Serverless Firestore; additive package structure; centralised `FirebaseRefs`/`BookingActions`/`StatusUtils`; transaction-based conflict prevention independent of data volume. |
| Deployment | 100% Implemented | Built and run on a real Android phone and a real Android tablet against the live Firebase project (`africa-south1`); installation steps above. |
| Innovation Feature | 100% Implemented | Four features beyond the basic requirements: **Offline Mode** (cache + live connectivity banners + offline-safe routing), **Role-Based Access** (server-enforced), **Data Analytics Dashboard** (live charts on Admin Dashboard and Admin Analytics), and **Predictive Trend Projection** (honestly labelled as a simple week-over-week heuristic, `AdminAnalyticsActivity.updateTrendProjection()`). |
