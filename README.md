# MMM Social Workers and Associates — Booking System Application

A native Android booking application built for MMM Social Workers and Associates, a psychology and social work practice operating across Midrand, Johannesburg, and Centurion, Gauteng, South Africa, with online consultations also offered.

Built for ITDMA3-34 (Project – Mobile Application and Web Services), Eduvos.

## Technology Stack

- **Language:** Java
- **Platform:** Android (minSdk 24, targetSdk 36)
- **Backend:** Firebase — Authentication, Cloud Firestore, Crashlytics — free Spark plan throughout. Firebase Cloud Messaging is configured (via `google-services.json`) but not actively used for push notifications in this version — see Known Limitations below for why, and for what local notifications actually do instead.
- **External API:** Nager.Date public holidays REST API (South Africa) — genuine HTTP integration, called via `HttpURLConnection`, parsed with `org.json`
- **Firestore region:** `africa-south1` (Johannesburg) — chosen for lowest latency to the practice's Gauteng-based clients

## Architecture

The app follows a client-heavy, serverless architecture: the Android app communicates directly with Firebase's managed services via the Firebase Android SDK, with no custom backend server. One external REST API call (Nager.Date) supplements this for public holiday data.

```
Presentation Layer (23 Activities + XML layouts)
                    ↓
Business Logic Layer (utils/: ValidationUtils, ThemeManager,
NotificationHelper, NetworkStatusHelper, AnalyticsHelper,
HolidayApiClient, CsvExportUtils, TouchTargetUtils, DialogUtils,
FirebaseRefs)
                    ↓
Firebase Android SDK + Nager.Date REST API (HTTP GET)
                    ↓
Firebase Authentication / Cloud Firestore / Local Notifications / Crashlytics
```

## Real-Time Updates

My Bookings (client) and the Admin Dashboard's booking list both use Firestore's `addSnapshotListener`, not a one-time `.get()` read. This means:
- If an admin confirms or declines a booking while the client has My Bookings open, the status badge updates automatically, with no manual refresh.
- If a client submits a new booking, or an admin marks one paid/completed from another device, the Admin Dashboard's list and summary counts update automatically for any admin currently viewing it.

This is entirely free-tier (no Cloud Functions or Blaze plan required) — `addSnapshotListener` is a standard, always-free part of the Firestore Android SDK. Listeners are registered once when each screen is created and explicitly removed in `onDestroy()` to avoid leaks.

## Database Schema (Cloud Firestore)

### `users`
`uid, name, email, phone, role ("client"|"admin"), emailVerified, notificationsEnabled, marketingOptIn, createdAt`

### `bookings`
`bookingId, clientId, service, consultationType ("online"|"in-person"), location, duration, date, time, additionalInfo, status, paid, createdAt`

### `bookedSlots` (anonymous, conflict-checking only — no personal data)
`slotId, date, time, consultationType, location, duration`

### `practiceInfo` (single document, id `"main"`)
`phone, email, whatsappLink, locations, operatingDays, operatingHours, crisisLineNote`

## API Integrations

- **Firebase SDK** — Authentication, Firestore (including real-time listeners and `count()` aggregation queries), Crashlytics
- **Nager.Date Public Holidays API** — `GET https://date.nager.at/api/v3/PublicHolidays/{year}/ZA`, called from `HolidayApiClient.java`, blocking bookings on South African public holidays even on an otherwise valid weekday

## Security

Firestore Security Rules enforce, server-side, exactly what this README claims — the full, actual rules deployed to this project's Firebase Console are committed in [`firestore.rules.md`](./firestore.rules.md) in this repository, so there is one single source of truth rather than rules described in prose that could drift out of sync with what's actually enforced.

Summary of what those rules enforce (verified during testing, not just hidden in the app's UI):
- A client can read/write only their own `users` document, and can never change their own `role` field (enforced via the rule comparing `request.resource.data.role == resource.data.role`)
- A client can read/write only their own `bookings` documents; an admin can read/write all of them
- Any authenticated user can read/write `bookedSlots` (no personal data is ever stored there, so this is safe)
- `practiceInfo` is publicly readable (so Contact works even before login) but admin-only to write
- Admin status itself is determined server-side by looking up the requester's own `users/{uid}` document and checking `role == "admin"` — a client can never simply claim to be an admin from the app

## Performance

- The Admin Analytics screen uses Firestore's server-side `count()` aggregation queries rather than downloading full documents to count client-side, keeping analytics fast regardless of how large the `bookings`/`users` collections grow.
- Real-time listeners (see above) replace the earlier polling-on-resume pattern, reducing redundant Firestore reads — data only re-fetches when it actually changes, not every time a screen becomes visible.
- All custom icons are vector drawables (`.xml`), not raster images, keeping the app's asset footprint small and icons sharp across every screen density.
- `RecyclerView` is used for every scrolling list (My Bookings, Admin Dashboard, Services, FAQ), which recycles row views rather than inflating a new one per item.

## Scalability

- Firestore is a fully managed, serverless NoSQL database that scales horizontally by design — this app does not manage or provision any database infrastructure itself.
- The package structure (`auth/`, `client/`, `admin/`, `settings/`, `utils/`) is additive: a new screen or role-specific feature is added as a new file in the relevant package, without needing to restructure existing code.
- `FirebaseRefs.java` centralises every Firestore collection reference in one file — a future schema change (e.g. renaming a collection) touches one class, not every screen that reads from it.
- The Admin Analytics screen's `count()` aggregation approach specifically avoids downloading a growing dataset just to display a number, keeping that screen's performance stable as the practice's booking history grows over years of use.

## Logging and Monitoring

- Firebase Crashlytics — automatic crash/error capture, confirmed active
- Explicit `Log.d/i/w/e` calls in `LoginActivity`, `RegisterActivity`, `BookAppointmentActivity`, `AdminDashboardActivity` — login attempts, registration outcomes, booking submissions, holiday/overlap rejections, filter/search actions, CSV export results

## Backup and Recovery

- Firestore data protected by Google's managed infrastructure redundancy
- Local theme preference included in Android Auto Backup via `backup_rules.xml`/`data_extraction_rules.xml`
- Manual export: Admin Dashboard's "Export CSV" button exports the currently-filtered booking list and opens Android's share sheet — a genuine, demoable manual backup mechanism
- Security rules themselves are version-controlled in [`firestore.rules.md`](./firestore.rules.md), so a previous rule set can always be recovered from Git history if a rule change ever needed reverting

## Notifications — Precisely Documented

This app uses **local, on-device notifications only** (`NotificationManager` + `AlarmManager`), not Firebase Cloud Messaging push, despite FCM being configured via `google-services.json`. Specifically:

- **Booking confirmation:** an immediate local notification fires the moment a client's own booking submission succeeds — this requires no server round-trip, since it's the same device notifying itself about its own just-completed action.
- **Appointment reminder:** a client receives **exactly one** reminder notification per booking, delivered locally on-device the **evening before the appointment, at 18:00**, via a single scheduled `AlarmManager` alarm. This is not a push notification — it is scheduled entirely on the client's own device at the moment they book, and requires no connectivity or server involvement to fire later.
- Both notifications use `NotificationCompat.BigTextStyle`, so a longer service name or message expands fully on long-press rather than being truncated with an ellipsis.

**What genuinely isn't implemented:** a notification triggered by one person's action (e.g. an admin confirming a booking) and delivered instantly to a *different* device (the client's) is not implemented, and cannot be, without a server-side trigger. See Known Limitations below.

## Known, Documented Limitations

- **Admin-to-client push notifications** (e.g. client instantly notified the moment an admin confirms a booking) require Firebase Cloud Functions, which requires the paid Blaze plan. This app remains on the free Spark plan. Status changes ARE now reflected live via Firestore's `addSnapshotListener` (see Real-Time Updates above) the moment the client's My Bookings screen is open — but no notification is pushed if the app is closed or backgrounded at the time.
- **Offline mode is precise, not universal**: Contact, Settings, and previously-loaded My Bookings data work offline via Firestore's cache. Login, booking submission, and cancellation genuinely require connectivity, since they depend on Firebase Authentication and Firestore transactions, neither of which can operate offline by design.
- **The Admin Analytics trend estimate is a simple heuristic, not machine learning.** It calculates a projection from the most recent week-over-week growth rate. This is explicitly labelled in the UI itself, to avoid overclaiming AI/predictive-analytics capability the app does not have.

## Repository Files

- `README.md` — this file
- `USER_GUIDE.md` — end-user instructions for both Client and Admin roles
- `TESTING.md` — full manual and automated test plan, with a bug log of real issues found and fixed during development
- `firestore.rules.md` — the actual, deployed Firestore Security Rules, kept in version control as the single source of truth referenced throughout this README

## Installation

1. Clone the repository
2. Open in Android Studio (Java, Gradle)
3. Place your own `google-services.json` in `app/`
4. Sync Gradle, build, run on an emulator or device (API 24+)

---

# PROJECT STRUCTURE / BLUEPRINT

## 1. App Identity

- **Display name:** MMM Social Workers & Associates
- **Language:** Java, exclusively
- **Backend:** Firebase (Auth, Firestore, Crashlytics), Spark (free) plan
- **GitHub:** `github.com/IgnatiusKhumalo/MMM-Booking-App` (private)
- **Colours (Light Mode, locked):** white/beige backgrounds, dark green (`#1F6F63`) primary buttons, light blue highlights, black/dark grey text
- **Colours (Dark Mode, locked):** `DARK_BACKGROUND` (24,24,24), `DARK_SURFACE` (40,40,40), `DARK_GREEN` (31,111,99), `LIGHT_BLUE` (120,200,210), `WHITE` (255,255,255), `LIGHT_GREY` (200,200,200), `BORDER` (70,70,70)
- Theme preference stored locally (SharedPreferences via `ThemeManager`), applied at startup via `MMMApplication`

## 2. User Roles

| Role | Creation | Access |
|---|---|---|
| Client | Self-registers, role auto-set `"client"`, no exceptions | Book, view own bookings, cancel, manage settings, request deletion |
| Admin | Manually provisioned via Firebase Console — no admin registration path exists anywhere in the app | Full booking management, Practice Information editing, booking analytics |

## 3. Full Project Structure

```
MMM-Booking-App/
├── README.md
├── USER_GUIDE.md
├── TESTING.md
└── app/
    ├── build.gradle.kts
    ├── google-services.json
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/mmm/bookingapp/
        │   │   ├── MainActivity.java (Screen 1 - Splash)
        │   │   ├── MMMApplication.java (applies saved theme at startup)
        │   │   │
        │   │   ├── auth/
        │   │   │   ├── WelcomeActivity.java (Screen 2)
        │   │   │   ├── RegisterActivity.java (Screen 3)
        │   │   │   ├── LoginActivity.java (Screen 4)
        │   │   │   └── ForgotPasswordActivity.java (Screen 5)
        │   │   │
        │   │   ├── client/
        │   │   │   ├── HomeActivity.java (Screen 6)
        │   │   │   ├── ServicesActivity.java (Screen 7)
        │   │   │   ├── BookAppointmentActivity.java (Screen 8)
        │   │   │   ├── BookingConfirmationActivity.java (Screen 9)
        │   │   │   ├── MyBookingsActivity.java (Screen 10 - real-time listener)
        │   │   │   └── ContactActivity.java (Screen 11)
        │   │   │
        │   │   ├── admin/
        │   │   │   ├── AdminDashboardActivity.java (Screen 18 - real-time listener)
        │   │   │   ├── BookingDetailActivity.java (Screen 19)
        │   │   │   ├── PracticeInfoActivity.java (Screen 20)
        │   │   │   └── AdminAnalyticsActivity.java (new screen - now includes bar charts)
        │   │   │
        │   │   ├── settings/
        │   │   │   ├── SettingsActivity.java (Screen 12)
        │   │   │   ├── ChangePasswordActivity.java (Screen 13)
        │   │   │   ├── AboutActivity.java (Screen 14)
        │   │   │   ├── PrivacyPolicyActivity.java (Screen 15)
        │   │   │   ├── TermsActivity.java (Screen 16)
        │   │   │   └── FAQActivity.java (Screen 17)
        │   │   │
        │   │   ├── models/
        │   │   │   ├── User.java (uid, name, email, phone, role, emailVerified,
        │   │   │   │   notificationsEnabled, marketingOptIn, createdAt)
        │   │   │   ├── Booking.java (mirrors bookings collection)
        │   │   │   ├── BookedSlot.java (mirrors bookedSlots collection)
        │   │   │   └── PracticeInfo.java (mirrors practiceInfo collection)
        │   │   │
        │   │   ├── adapters/
        │   │   │   ├── BookingListAdapter.java (shared by My Bookings + Admin Dashboard)
        │   │   │   ├── ServiceListAdapter.java (Services screen)
        │   │   │   └── FAQListAdapter.java (FAQ screen, expandable rows)
        │   │   │
        │   │   └── utils/
        │   │       ├── ValidationUtils.java
        │   │       ├── ThemeManager.java
        │   │       ├── NotificationHelper.java (now uses BigTextStyle)
        │   │       ├── ReminderBroadcastReceiver.java
        │   │       ├── FirebaseRefs.java
        │   │       ├── NetworkStatusHelper.java
        │   │       ├── AnalyticsHelper.java
        │   │       ├── HolidayApiClient.java (real external REST API call)
        │   │       ├── CsvExportUtils.java (manual backup/export mechanism)
        │   │       ├── TouchTargetUtils.java (accessibility - expands small touch targets)
        │   │       └── DialogUtils.java (new - shared logout/delete-account confirmation dialogs)
        │   │
        │   └── res/
        │       ├── layout/ (all activity_*.xml, item_*.xml, dialog_*.xml)
        │       ├── values/ (colors.xml, strings.xml, themes.xml, dimens.xml)
        │       ├── values-night/ (colors.xml, themes.xml)
        │       ├── drawable/ (mmm_logo.png, custom icons and card/pill backgrounds)
        │       ├── xml/ (backup_rules.xml, data_extraction_rules.xml, file_paths.xml)
        │       └── mipmap-*/ (launcher icons)
        │
        ├── test/java/com/mmm/bookingapp/utils/
        │   └── ValidationUtilsTest.java (27 JUnit unit tests)
        │
        └── androidTest/java/com/mmm/bookingapp/auth/
            └── WelcomeActivityTest.java (Espresso instrumented test)
```

## 4. Screen-by-Screen Summary (20 original + 1 new = 21 screens)

**Screen 1 — Splash:** Full logo shown edge-to-edge, practice name heading, loading spinner; checks cached session and routes to Welcome, Home, or Admin Dashboard automatically.

**Screen 2 — Welcome:** Landing page after Splash. Full logo, heading/tagline, two rounded-pill buttons (Register/Login), reassurance footer line.

**Screen 3 — Register:** Card-based layout (Personal Details, Account Security, Consent). Full Name/Email/Phone(+27, 9-digit enforced)/Password/Confirm Password with show/hide toggles. Compulsory consent checkbox (tappable links to About/Privacy Policy/Terms) and optional marketing-subscribe checkbox, both with generous spacing so no text is ever clipped.

**Screen 4 — Login:** Circular logo badge, "Welcome Back" heading, warm subtitle, bordered card-style Email/Password fields with icons, show/hide password toggle, offline-aware. On successful login, passes a flag so the destination screen can show a "Logged in successfully" confirmation.

**Screen 5 — Forgot Password:** Sends a Firebase-managed reset email, deliberately identical confirmation message regardless of whether the email is registered.

**Screen 6 — Home:** Header with dynamic first-name greeting + circular Logout icon (now confirms via a Cancel/Log Out dialog before signing out). Branding card, "Next Appointment" card with Book Appointment button, 2×2 card grid, footer support bar. Shows "Logged in successfully" toast on arrival from a fresh login.

**Screen 7 — Services:** Lists the practice's 7 real services, informational only.

**Screen 8 — Book Appointment:** Full dropdown/date/time logic including South African public holiday blocking, double-booking prevention, offline guard.

**Screen 9 — Booking Confirmation:** Summary of the submitted booking, status shown as Pending.

**Screen 10 — My Bookings:** Now uses a real-time Firestore listener — status changes made by an admin appear automatically without a manual refresh. Cancel requires confirmation; offline banner and booking-history summary unchanged.

**Screen 11 — Contact:** Practice phone/email/WhatsApp, locations, hours, crisis-line note, all read live from `practiceInfo`.

**Screen 12 — Settings:** Card-based layout. Dark Mode and Booking Reminders toggles now show a confirmation toast. Logout confirms via a Cancel/Log Out dialog. Delete My Account now shows a clear explainer dialog before opening the email app.

**Screen 13 — Change Password:** Requires re-authentication before allowing a change.

**Screens 14–17 — About / Privacy Policy / Terms / FAQ:** Static content screens.

**Screen 18 — Admin Dashboard:** Now uses a real-time Firestore listener for the booking list and summary counts — a new client booking or a status change made from Booking Detail appears automatically. Logout confirms via a Cancel/Log Out dialog. Shows "Logged in successfully" toast on arrival from a fresh login.

**Screen 19 — Booking Detail (Admin):** Confirm/Decline/Mark Paid/Mark Completed.

**Screen 20 — Practice Information (Admin):** Edits the shared `practiceInfo` document.

**New Screen — Admin Analytics:** Time-bucketed counts with % change, PLUS a real visual bar-chart comparison (Today/Week/Month/Year) for both Bookings Created and New Registrations, using the same weight-based View technique as the Admin Dashboard's overview chart. Includes a simple, explicitly-labelled trend estimate — not AI or machine learning.

## 5. Data Design — Firestore Security Rules (summary)

- `users/{uid}`: owner-only read/write; `role` field is write-protected (never client-settable)
- `bookings/{bookingId}`: owner-only read/write for clients; full read/write for admin
- `bookedSlots/{slotId}`: any authenticated user may read/write (no personal data stored here)
- `practiceInfo/main`: publicly readable; admin-only write

## 6. Core Business Rules

- Operating hours: Monday–Friday, 08:00–18:00, enforced from `practiceInfo`
- South African public holidays also blocked (Nager.Date API), on top of the weekday/hours rule
- Consultation Type drives Location's visibility (hidden + explained for Online, required for In-Person)
- Booking status always defaults to `"pending"`
- Cancellation is permanent, requires explicit confirmation, frees the matching `bookedSlots` entry
- Booking submission, cancellation, and fresh login all require live connectivity
- Logout (from any of Home/Admin Dashboard/Settings) always requires explicit confirmation via a Cancel/Log Out dialog before actually signing out
- Account deletion always shows an explanatory dialog before opening the pre-addressed email
