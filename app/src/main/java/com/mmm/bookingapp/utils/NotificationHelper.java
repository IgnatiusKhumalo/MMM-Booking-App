package com.mmm.bookingapp.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.mmm.bookingapp.R;

import java.util.Calendar;

/**
 * Centralises notification permission handling and local notification
 * scheduling, per the Project Structure document.
 *
 * IMPORTANT DOCUMENTED LIMITATION: this class can only produce
 * notifications that originate FROM this same device (booking
 * confirmation, appointment reminders). It cannot deliver a push
 * notification triggered by another user's action on another device
 * (e.g. an admin confirming a booking) - that requires a server-side
 * trigger (Firebase Cloud Functions), which is out of scope for the
 * free Spark plan this project is built on. See Blueprint Section 7
 * and the D6 Future Work recommendation.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "mmm_booking_channel";
    private static final int PERMISSION_REQUEST_CODE = 100;

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
     * Android 13+ (API 33+) - without this, notifications silently fail
     * to display with no visible error, per the documented gap fix.
     * Safe to call on any Android version; does nothing below API 33.
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

    /**
     * Shows an immediate local notification. Used for booking confirmation
     * (the client's own device confirming their own just-submitted action) -
     * legitimate without a backend, since no other device is involved.
     */
    public static void showImmediateNotification(Context context, String title, String message) {
        if (!hasPermission(context)) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        android.app.NotificationManager manager =
                (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    /**
     * Schedules a local reminder notification for the evening before the
     * appointment (18:00 the day before), using AlarmManager. This runs
     * entirely on the client's own device - no server or backend involved,
     * so it works fully within the free Spark plan.
     *
     * bookingId is used to build a stable, unique request code so a
     * specific booking's reminder can later be cancelled if that booking
     * is cancelled (see cancelReminder below).
     */
    public static void scheduleReminder(Context context, String bookingId, String dateStr,
                                        String service) {
        if (!hasPermission(context)) return;

        try {
            String[] dateParts = dateStr.split("-");
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]) - 1;
            int day = Integer.parseInt(dateParts[2]);

            Calendar reminderTime = Calendar.getInstance();
            reminderTime.set(year, month, day, 18, 0, 0);
            reminderTime.add(Calendar.DAY_OF_MONTH, -1); // evening before

            // Don't schedule a reminder in the past (e.g. booking made same-day
            // or last-minute) - simply skip silently in that case.
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
                // setAndAllowWhileIdle: approximate delivery timing, does NOT
                // require the special SCHEDULE_EXACT_ALARM permission that
                // exact alarms need on Android 12+ - the right, simpler choice
                // for a "reminder" which doesn't need to-the-second precision.
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, reminderTime.getTimeInMillis(), pendingIntent);
            }
        } catch (Exception ignored) {
            // Malformed date or scheduling failure - non-critical, booking
            // itself already succeeded regardless of reminder scheduling.
        }
    }

    /** Cancels a previously scheduled reminder - called when a booking is cancelled. */
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