# MMM Social Workers and Associates — Booking System Application

A native Android booking application built for MMM Social Workers and Associates, a psychology and social work practice operating across Midrand, Johannesburg, and Centurion, Gauteng, South Africa, with online consultations also offered.

Built for ITDMA3-34 (Project – Mobile Application and Web Services), Eduvos.

## Technology Stack

- **Language:** Java
- **Platform:** Android (minSdk 24, targetSdk 36)
- **Backend:** Firebase — Authentication, Cloud Firestore, Crashlytics — free Spark plan throughout. Firebase Cloud Messaging is configured (via `google-services.json`) but not actively used for push notifications in this version — see Notifications and Known Limitations below for why, and for what local notifications actually do instead.
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
