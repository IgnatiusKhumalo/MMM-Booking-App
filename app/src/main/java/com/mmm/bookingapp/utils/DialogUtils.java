package com.mmm.bookingapp.utils;

import android.app.AlertDialog;
import android.content.Context;

import com.mmm.bookingapp.R;

/**
 * Centralises small, reusable confirmation dialogs used across multiple
 * screens (Home, Admin Dashboard, Settings), so the wording and button
 * order stay identical everywhere rather than duplicated per screen.
 *
 * Button order follows Android's own AlertDialog convention and the
 * pattern used by most major apps: Cancel sits on the left (neutral,
 * dismisses, does nothing), the confirming action sits on the right.
 *
 * This class deliberately does NOT touch button colour/branding - each
 * screen's own existing button styling (Home's green, Settings' red
 * danger-styled Logout, Admin's red icon) is left exactly as it already
 * is; only the dialog's wording and confirm/cancel behaviour is shared.
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
     * delete my account" isn't left confused by a blank pre-addressed
     * email with no instructions.
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
}