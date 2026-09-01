# MMM Social Workers and Associates — Booking System Application

A native Android booking application built for MMM Social Workers and Associates, a psychology and social work practice operating across Midrand, Johannesburg, and Centurion, Gauteng, South Africa, with online consultations also offered.

Built for ITDMA3-34 (Project – Mobile Application and Web Services), Eduvos.

## Technology Stack
- **Language:** Java
- **Platform:** Android (minSdk 24, targetSdk 36)
- **Backend:** Firebase (Authentication, Cloud Firestore, Cloud Messaging, Crashlytics) — free Spark plan
- **External API:** [Nager.Date](https://date.nager.at) public holidays REST API (South Africa)
- **Firestore region:** africa-south1 (Johannesburg)

## Architecture
The app follows a client-heavy, serverless architecture: the Android app communicates directly with Firebase's managed services via the Firebase Android SDK, with no custom backend server. A single external REST API call (Nager.Date) supplements this for public holiday data, called directly via `HttpURLConnection` and parsed with `org.json` — no third-party networking library required.

```
Presentation Layer (Activities + XML layouts)
                ↓
Business Logic Layer (utils/: ValidationUtils, ThemeManager,
NotificationHelper, NetworkStatusHelper, AnalyticsHelper,
HolidayApiClient, CsvExportUtils, TouchTargetUtils, FirebaseRefs)
                ↓
Firebase Android SDK + Nager.Date REST API (HTTP GET)
                ↓
Firebase Authentication / Cloud Firestore / Cloud Messaging / Crashlytics
```

## Database Schema (Cloud Firestore)

**users**
```
uid, name, email, phone, role ("client"|"admin"), emailVerified,
notificationsEnabled, marketingOptIn, createdAt
```

**bookings**
```
bookingId, clientId, service, consultationType ("online"|"in-person"),
location, duration, date, time, additionalInfo, status, paid, createdAt
```

**bookedSlots** (anonymous, conflict-checking only — no personal data)
```
slotId, date, time, consultationType, location, duration
```

**practiceInfo** (single document, id "main")
```
phone, email, whatsappLink, locations, operatingDays, operatingHours,
crisisLineNote
```

## API Integrations
1. **Firebase SDK** — Authentication, Firestore, Cloud Messaging, Crashlytics (all via Google's official Android SDK).
2. **Nager.Date Public Holidays API** — `GET https://date.nager.at/api/v3/PublicHolidays/{year}/ZA`, called from `HolidayApiClient.java`, blocking bookings on South African public holidays even on an otherwise valid weekday.

## Security
Firestore Security Rules enforce (server-side, not just in the app's UI):
- A client can read/write only their own `users` document, and never their own `role` field.
- A client can read/write only their own `bookings` documents.
- Any authenticated user can read `bookedSlots` (no personal data exposed there).
- Only `role: "admin"` accounts can read/write all `bookings` and `practiceInfo`.

## Logging and Monitoring
- **Firebase Crashlytics** — automatic crash/error capture.
- **Explicit `Log.d/i/w/e` calls** at key decision points in `LoginActivity`, `RegisterActivity`, `BookAppointmentActivity`, and `AdminDashboardActivity` (login attempts, registration outcomes, booking submissions, holiday/overlap rejections, filter/search actions, CSV export results).

## Backup and Recovery
- **Firestore data** (bookings, accounts, practice info) is protected by Google's managed infrastructure redundancy.
- **Local settings** (theme preference) are included in Android's Auto Backup via `backup_rules.xml` / `data_extraction_rules.xml`.
- **Manual export**: Admin Dashboard's "Export to CSV" button exports the currently-filtered booking list to a CSV file and opens it for sharing/saving via Android's share sheet — a genuine, demoable manual backup mechanism.

## Known, Documented Limitations
- **Admin-to-client push notifications** (e.g. a client being notified the instant an admin confirms their booking) require Firebase Cloud Functions, which requires the paid Blaze plan. This app remains on the free Spark plan; status changes are instead reflected the next time the affected screen reloads. Booking-submission confirmations and appointment reminders work entirely client-side and function normally.
- **Offline mode is precise, not universal**: Contact, Settings, and previously-loaded My Bookings data work offline via Firestore's cache. Login, booking submission, and cancellation genuinely require connectivity, since they depend on Firebase Authentication and Firestore transactions, neither of which can operate offline by design.

## Installation
1. Clone this repository.
2. Open in Android Studio (Java, Gradle).
3. Place your own `google-services.json` in `app/`.
4. Sync Gradle, build, and run on an emulator or device (API 24+).

## Project Structure
See `Project_Structure.docx` (submitted alongside Deliverable 3) for the full annotated file tree.

## User Guide
See `USER_GUIDE.md` in this repository for end-user instructions.
