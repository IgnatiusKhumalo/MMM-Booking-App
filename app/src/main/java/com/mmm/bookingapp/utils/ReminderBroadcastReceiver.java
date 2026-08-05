package com.mmm.bookingapp.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Fires when a scheduled reminder alarm (set by NotificationHelper.scheduleReminder)
 * triggers, and displays the actual notification at that time.
 */
public class ReminderBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String service = intent.getStringExtra("service");
        String message = "You have an appointment tomorrow" +
                (service != null ? " for " + service : "") + ".";

        NotificationHelper.showImmediateNotification(context, "Appointment Reminder", message);
    }
}