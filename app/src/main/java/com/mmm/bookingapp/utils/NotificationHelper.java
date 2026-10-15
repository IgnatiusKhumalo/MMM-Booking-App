package com.mmm.bookingapp.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

/**
 * Centralises notification permission handling, the user's "Booking
 * Reminders" preference, and local notification scheduling, per the
 * Project Structure document.
 *
 * Booking Reminders switch: the preference is saved on the account in
 * Firestore (users/{uid}.notificationsEnabled, the source of truth) and
 * cached on this device in SharedPreferences, keyed per account, so the
 * checks below are instant, work offline, and never leak one user's
 * setting to another user on the same phone. Both notifications this
 * class can produce - the immediate booking confirmation and the
 * evening-before reminder - are skipped when the switch is off. The
 * reminder broadcast receiver shows its notification through
 * showImmediateNotification(), so it is covered by the same check.
 *
 * DOCUMENTED LIMITATION: this class can only produce notifications that
 * originate FROM this same device (booking confirmation, appointment
 * reminders). It cannot deliver a push notification triggered by another
 * user's action on another device (e.g. an admin confirming a booking) -
 * that needs a server-side trigger (Firebase Cloud Functions), which is
 * out of scope for the free Spark plan. See the README's Notifications
 * section and Known Limitations.
 *
 * Every notification uses NotificationCompat.BigTextStyle, so a long
 * service name expands fully instead of being cut off with an ellipsis.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "mmm_booking_channel";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private static final String PREFS_NAME = "mmm_notification_prefs";
    private static final String PREF_KEY_PREFIX = "reminders_enabled_";

    /** Call once, early (e.g. in MainActivity or Application), before any notification is shown. */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Booking Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Booking confirmations and appointment reminders");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    /**
     * Requests the POST_NOTIFICATIONS runtime permission, required on
     * Android 13+ (API 33+) - without it notifications silently fail to
     * display. Safe to call on any Android version; does nothing below
     * API 33.
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    private static boolean hasPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ---------- Booking Reminders preference ----------

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Per-account key, so one user's setting never applies to another user on the same device. */
    private static String prefKey() {
        String uid = FirebaseRefs.getAuth().getUid();
        return PREF_KEY_PREFIX + (uid != null ? uid : "anonymous");
    }

    /** True unless the signed-in user has switched Booking Reminders off. Defaults to on. */
    public static boolean areRemindersEnabled(Context context) {
        return prefs(context).getBoolean(prefKey(), true);
    }

    /**
     * Saves the preference on this device. Call this BEFORE scheduling or
     * cancelling reminders in response to a toggle, because
     * scheduleReminder() checks this value.
     */
    public static void setRemindersEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(prefKey(), enabled).apply();
    }

    // ---------- Notifications ----------

    /**
     * Shows an immediate local notification (booking confirmation: the
     * client's own device confirming their own just-submitted action, so
     * no backend is needed). Skipped when the permission is missing or
     * Booking Reminders is switched off.
     */
    public static void showImmediateNotification(Context context, String title, String message) {
        if (!hasPermission(context)) return;
        if (!areRemindersEnabled(context)) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    /**
     * Schedules a local reminder for the evening before the appointment
     * (18:00 the day before) using AlarmManager. This runs entirely on
     * the client's own device - no server involved - so it works within
     * the free Spark plan. Exactly ONE reminder is scheduled per booking.
     * Skipped when Booking Reminders is switched off.
     *
     * bookingId builds a stable, unique request code so a specific
     * booking's reminder can later be cancelled (see cancelReminder).
     */
    public static void scheduleReminder(Context context, String bookingId, String dateStr,
                                        String service) {
        if (!hasPermission(context)) return;
        if (!areRemindersEnabled(context)) return;

        try {
            String[] dateParts = dateStr.split("-");
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]) - 1;
            int day = Integer.parseInt(dateParts[2]);

            Calendar reminderTime = Calendar.getInstance();
            reminderTime.set(year, month, day, 18, 0, 0);
            reminderTime.add(Calendar.DAY_OF_MONTH, -1); // evening before

            // Don't schedule a reminder in the past (booking made same-day
            // or last-minute) - skip silently.
            if (reminderTime.getTimeInMillis() <= System.currentTimeMillis()) return;

            Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
            intent.putExtra("service", service);

            int requestCode = bookingId.hashCode();
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                // setAndAllowWhileIdle: approximate delivery, and does NOT need
                // the special SCHEDULE_EXACT_ALARM permission that exact alarms
                // need on Android 12+ - right for a reminder that doesn't
                // need to-the-second precision.
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, reminderTime.getTimeInMillis(), pendingIntent);
            }
        } catch (Exception ignored) {
            // Malformed date or scheduling failure - non-critical; the booking
            // itself already succeeded regardless of reminder scheduling.
        }
    }

    /** Cancels a previously scheduled reminder - on booking cancellation, or when reminders are switched off. */
    public static void cancelReminder(Context context, String bookingId) {
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        int requestCode = bookingId.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}