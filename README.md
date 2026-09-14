# MMM Social Workers and Associates — Booking System Application

A native Android booking application built for MMM Social Workers and Associates, a psychology and social work practice operating across Midrand, Johannesburg, and Centurion, Gauteng, South Africa, with online consultations also offered.

Built for ITDMA3-34 (Project – Mobile Application and Web Services), Eduvos.

## Technology Stack

- **Language:** Java
- **Platform:** Android (minSdk 24, targetSdk 36)
- **Backend:** Firebase — Authentication, Cloud Firestore, Cloud Messaging, Crashlytics — free Spark plan throughout
- **External API:** Nager.Date public holidays REST API (South Africa) — genuine HTTP integration, called via `HttpURLConnection`, parsed with `org.json`
- **Firestore region:** `africa-south1` (Johannesburg) — chosen for lowest latency to the practice's Gauteng-based clients

## Architecture

The app follows a client-heavy, serverless architecture: the Android app communicates directly with Firebase's managed services via the Firebase Android SDK, with no custom backend server. One external REST API call (Nager.Date) supplements this for public holiday data.

```
Presentation Layer (23 Activities + XML layouts)
        ↓
Business Logic Layer (utils/: ValidationUtils, ThemeManager,
  NotificationHelper, NetworkStatusHelper, AnalyticsHelper,
  HolidayApiClient, CsvExportUtils, TouchTargetUtils, FirebaseRefs)
        ↓
Firebase Android SDK  +  Nager.Date REST API (HTTP GET)
        ↓
Firebase Authentication / Cloud Firestore / Cloud Messaging / Crashlytics
```

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

- **Firebase SDK** — Authentication, Firestore, Cloud Messaging, Crashlytics
- **Nager.Date Public Holidays API** — `GET https://date.nager.at/api/v3/PublicHolidays/{year}/ZA`, called from `HolidayApiClient.java`, blocking bookings on South African public holidays even on an otherwise valid weekday

## Security

Firestore Security Rules enforce, server-side (verified during testing, not just hidden in the UI):

- A client can read/write only their own `users` document, and never their own `role` field
- A client can read/write only their own `bookings` documents
- Any authenticated user can read `bookedSlots` (no personal data exposed there)
- Only `role: "admin"` accounts can read/write all `bookings` and `practiceInfo`

## Logging and Monitoring

- Firebase Crashlytics — automatic crash/error capture, confirmed active
- Explicit `Log.d/i/w/e` calls in `LoginActivity`, `RegisterActivity`, `BookAppointmentActivity`, `AdminDashboardActivity` — login attempts, registration outcomes, booking submissions, holiday/overlap rejections, filter/search actions, CSV export results

## Backup and Recovery

- Firestore data protected by Google's managed infrastructure redundancy
- Local theme preference included in Android Auto Backup via `backup_rules.xml`/`data_extraction_rules.xml`
- Manual export: Admin Dashboard's "Export CSV" button exports the currently-filtered booking list and opens Android's share sheet — a genuine, demoable manual backup mechanism

## Known, Documented Limitations

- Admin-to-client push notifications (e.g. client instantly notified when admin confirms a booking) require Firebase Cloud Functions, which requires the paid Blaze plan. This app remains on the free Spark plan; status changes are reflected the next time the affected screen reloads. Booking-submission confirmations and appointment reminders work entirely client-side and function normally.
- Offline mode is precise, not universal: Contact, Settings, and previously-loaded My Bookings data work offline via Firestore's cache. Login, booking submission, and cancellation genuinely require connectivity, since they depend on Firebase Authentication and Firestore transactions, neither of which can operate offline by design.
- The Admin Analytics trend estimate is a simple heuristic, not machine learning. It calculates a projection from the most recent week-over-week growth rate. This is explicitly labelled in the UI itself, to avoid overclaiming AI/predictive-analytics capability the app does not have.

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
- **Backend:** Firebase (Auth, Firestore, FCM, Crashlytics), Spark (free) plan
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
        │   │   ├── MainActivity.java                     (Screen 1 - Splash)
        │   │   ├── MMMApplication.java                    (applies saved theme at startup)
        │   │   │
        │   │   ├── auth/
        │   │   │   ├── WelcomeActivity.java                (Screen 2)
        │   │   │   ├── RegisterActivity.java                (Screen 3)
        │   │   │   ├── LoginActivity.java                   (Screen 4)
        │   │   │   └── ForgotPasswordActivity.java          (Screen 5)
        │   │   │
        │   │   ├── client/
        │   │   │   ├── HomeActivity.java                    (Screen 6)
        │   │   │   ├── ServicesActivity.java                 (Screen 7)
        │   │   │   ├── BookAppointmentActivity.java          (Screen 8)
        │   │   │   ├── BookingConfirmationActivity.java      (Screen 9)
        │   │   │   ├── MyBookingsActivity.java                (Screen 10)
        │   │   │   └── ContactActivity.java                   (Screen 11)
        │   │   │
        │   │   ├── admin/
        │   │   │   ├── AdminDashboardActivity.java            (Screen 18)
        │   │   │   ├── BookingDetailActivity.java              (Screen 19)
        │   │   │   ├── PracticeInfoActivity.java               (Screen 20)
        │   │   │   └── AdminAnalyticsActivity.java             (new screen, beyond original 20)
        │   │   │
        │   │   ├── settings/
        │   │   │   ├── SettingsActivity.java                   (Screen 12)
        │   │   │   ├── ChangePasswordActivity.java             (Screen 13)
        │   │   │   ├── AboutActivity.java                      (Screen 14)
        │   │   │   ├── PrivacyPolicyActivity.java              (Screen 15)
        │   │   │   ├── TermsActivity.java                      (Screen 16)
        │   │   │   └── FAQActivity.java                        (Screen 17)
        │   │   │
        │   │   ├── models/
        │   │   │   ├── User.java              (uid, name, email, phone, role, emailVerified,
        │   │   │   │                            notificationsEnabled, marketingOptIn, createdAt)
        │   │   │   ├── Booking.java           (mirrors bookings collection)
        │   │   │   ├── BookedSlot.java        (mirrors bookedSlots collection)
        │   │   │   └── PracticeInfo.java      (mirrors practiceInfo collection)
        │   │   │
        │   │   ├── adapters/
        │   │   │   ├── BookingListAdapter.java    (shared by My Bookings + Admin Dashboard)
        │   │   │   ├── ServiceListAdapter.java    (Services screen)
        │   │   │   └── FAQListAdapter.java        (FAQ screen, expandable rows)
        │   │   │
        │   │   └── utils/
        │   │       ├── ValidationUtils.java
        │   │       ├── ThemeManager.java
        │   │       ├── NotificationHelper.java
        │   │       ├── ReminderBroadcastReceiver.java
        │   │       ├── FirebaseRefs.java
        │   │       ├── NetworkStatusHelper.java
        │   │       ├── AnalyticsHelper.java
        │   │       ├── HolidayApiClient.java       (real external REST API call)
        │   │       ├── CsvExportUtils.java          (manual backup/export mechanism)
        │   │       └── TouchTargetUtils.java        (accessibility - expands small touch targets)
        │   │
        │   └── res/
        │       ├── layout/          (all activity_*.xml, item_*.xml, dialog_*.xml)
        │       ├── values/          (colors.xml, strings.xml, themes.xml, dimens.xml)
        │       ├── values-night/    (colors.xml, themes.xml)
        │       ├── drawable/        (mmm_logo.png, custom icons and card/pill backgrounds)
        │       ├── xml/             (backup_rules.xml, data_extraction_rules.xml, file_paths.xml)
        │       └── mipmap-*/        (launcher icons)
        │
        ├── test/java/com/mmm/bookingapp/utils/
        │   └── ValidationUtilsTest.java       (27 JUnit unit tests)
        │
        └── androidTest/java/com/mmm/bookingapp/auth/
            └── WelcomeActivityTest.java        (Espresso instrumented test)
```

## 4. Screen-by-Screen Summary (20 original + 1 new = 21 screens)

**Screen 1 — Splash:** Full logo shown edge-to-edge, practice name heading, loading spinner; checks cached session and routes to Welcome, Home, or Admin Dashboard automatically.

**Screen 2 — Welcome:** Landing page after Splash. Full logo, heading/tagline, two rounded-pill buttons (Register/Login), reassurance footer line.

**Screen 3 — Register:** Card-based layout (Personal Details, Account Security, Consent). Full Name/Email/Phone(+27, 9-digit enforced)/Password/Confirm Password with show/hide toggles. Compulsory consent checkbox (tappable links to About/Privacy Policy/Terms) and optional marketing-subscribe checkbox, both with generous spacing so no text is ever clipped.

**Screen 4 — Login:** Circular logo badge, "Welcome Back" heading, warm subtitle, bordered card-style Email/Password fields with icons, show/hide password toggle, offline-aware (blocks fresh login attempts with a clear message when there's no connectivity, since credential verification cannot happen offline).

**Screen 5 — Forgot Password:** Unchanged core function — sends a Firebase-managed reset email, deliberately identical confirmation message regardless of whether the email is registered (prevents account enumeration).

**Screen 6 — Home:** Header with dynamic first-name greeting + circular Logout icon. Branding card showing the full logo edge-to-edge. "Next Appointment" card with a full-width Book Appointment button. 2×2 card grid (Services/My Bookings/Contact/Settings) using real MaterialCardView with custom dark-green line-art icons. Footer support bar linking to Contact. Offline banner shown when disconnected.

**Screen 7 — Services:** Lists the practice's 7 real services, informational only.

**Screen 8 — Book Appointment:** Service/Consultation-Type/Location/Duration dropdowns with visible arrows; selecting Online replaces Location with a highlighted explanation box; date/time pickers enforce Mon–Fri, operating hours, AND South African public holidays (via the Nager.Date API); double-booking prevented via a Firestore transaction against bookedSlots; blocked with a clear message when offline (submission requires a live transaction).

**Screen 9 — Booking Confirmation:** Summary of the submitted booking, status shown as Pending.

**Screen 10 — My Bookings:** Client's own bookings list with cancel (permanent-action confirmation dialog required); offline banner shown when serving cached data; "bookings this year" summary line computed from already-loaded data.

**Screen 11 — Contact:** Practice phone/email/WhatsApp (tap-to-call/email/WhatsApp), locations, hours, crisis-line note, How-to-Book guidance — all read live from the `practiceInfo` document.

**Screen 12 — Settings:** Card-based layout (Account, Security, Notifications, App, Admin-only Practice Administration, Support). Exactly one Logout button (in Support), all long-label outlined buttons sized to wrap two lines without clipping.

**Screen 13 — Change Password:** Requires re-authentication with the current password before allowing a change.

**Screens 14–17 — About / Privacy Policy / Terms / FAQ:** Static content screens, reachable from Settings and also directly from Register's consent links.

**Screen 18 — Admin Dashboard:** Header with title/subtitle, circular Settings and Logout icons. Overview card: 2×2 flat-accent stat grid, one proportional stacked bar (single continuous bar, segments sized by real share of total), legend row, "View Detailed Analytics" button. Search bar (searches service/location/type/status/notes, combines with filters). Filter pills (Status/Location/Date). Utility row (Clear Filters / Export CSV). Booking list as modern cards with status badges (bold white text for contrast in both light and dark mode).

**Screen 19 — Booking Detail (Admin):** Confirm/Decline/Mark Paid/Mark Completed; Decline reuses the same underlying mechanism as a client cancellation.

**Screen 20 — Practice Information (Admin):** Edits the single shared `practiceInfo` document; changes apply app-wide immediately, no rebuild needed.

**New Screen — Admin Analytics:** Time-bucketed counts (Today/Week/Month/Year) with % change vs. the prior equivalent period, for Bookings Created and New Registrations, using Firestore `count()` aggregation queries (server-side counting, more scalable than downloading full documents). Includes a simple, explicitly-labelled week-over-week trend estimate — not AI or machine learning.

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
- Booking submission, cancellation, and fresh login all require live connectivity (documented, explained to the user, not left as a silent failure)
