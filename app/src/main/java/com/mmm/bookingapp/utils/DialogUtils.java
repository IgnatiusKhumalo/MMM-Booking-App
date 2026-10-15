package com.mmm.bookingapp.utils;

import android.app.AlertDialog;
import android.content.Context;

import com.mmm.bookingapp.R;

/**
 * Centralises small, reusable confirmation dialogs used across multiple
 * screens, so wording and button order stay identical everywhere.
 *
 * Button order follows Android's AlertDialog convention: Cancel on the
 * left (dismisses, does nothing), the confirming action on the right.
 * Button colours are left to each screen's own theme.
 */
public class DialogUtils {

    public static void showLogoutConfirmation(Context context, Runnable onConfirmLogout) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setCancelable(true)
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.btn_logout_top, (dialog, which) -> {
                    dialog.dismiss();
                    onConfirmLogout.run();
                })
                .show();
    }

    /**
     * Explains the account-deletion process clearly BEFORE opening the
     * email app, so a client who doesn't already know "email us to
     * delete my account" isn't left confused by a blank email.
     */
    public static void showDeleteAccountExplainer(Context context, Runnable onContinue) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_delete_account_title)
                .setMessage(R.string.dialog_delete_account_message)
                .setCancelable(true)
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.btn_continue, (dialog, which) -> {
                    dialog.dismiss();
                    onContinue.run();
                })
                .show();
    }

    /** Admin: confirm before declining (cancelling) a client's booking. */
    public static void showDeclineBookingConfirmation(Context context, Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_decline_booking_title)
                .setMessage(R.string.dialog_decline_booking_message)
                .setCancelable(true)
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.btn_decline_booking, (dialog, which) -> {
                    dialog.dismiss();
                    onConfirm.run();
                })
                .show();
    }

    /** Admin: confirm before permanently deleting a booking record. */
    public static void showDeleteBookingConfirmation(Context context, Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_delete_booking_title)
                .setMessage(R.string.dialog_delete_booking_message)
                .setCancelable(true)
                .setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.btn_delete_booking_confirm, (dialog, which) -> {
                    dialog.dismiss();
                    onConfirm.run();
                })
                .show();
    }
}