package com.mmm.bookingapp.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.mmm.bookingapp.models.Booking;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Genuine, demoable manual backup mechanism for the "Backup and Recovery"
 * requirement, alongside Firestore's own Google-managed infrastructure
 * redundancy (already documented in the Blueprint). Exports the admin's
 * currently-filtered booking list to a real CSV file, saved to the app's
 * own external files directory (no storage permission needed - this is
 * scoped storage, per modern Android requirements), and immediately
 * offers it for sharing/saving via Android's standard share sheet using
 * a FileProvider, since apps cannot share raw file:// URIs on modern
 * Android for security reasons.
 */
public class CsvExportUtils {

    private static final String TAG = "CsvExportUtils";

    public static void exportBookingsToCsv(Context context, List<Booking> bookings) {
        try {
            File exportDir = new File(context.getExternalFilesDir(null), "exports");
            if (!exportDir.exists()) exportDir.mkdirs();

            String fileName = "mmm_bookings_export_" + System.currentTimeMillis() + ".csv";
            File csvFile = new File(exportDir, fileName);

            FileWriter writer = new FileWriter(csvFile);
            writer.append("Service,ConsultationType,Location,Duration,Date,Time,Status,Paid,AdditionalInfo\n");

            for (Booking b : bookings) {
                writer.append(csvEscape(b.getService())).append(",");
                writer.append(csvEscape(b.getConsultationType())).append(",");
                writer.append(csvEscape(b.getLocation() != null ? b.getLocation() : "Online")).append(",");
                writer.append(String.valueOf(b.getDuration())).append(",");
                writer.append(csvEscape(b.getDate())).append(",");
                writer.append(csvEscape(b.getTime())).append(",");
                writer.append(csvEscape(b.getStatus())).append(",");
                writer.append(String.valueOf(b.isPaid())).append(",");
                writer.append(csvEscape(b.getAdditionalInfo() != null ? b.getAdditionalInfo() : "")).append("\n");
            }
            writer.flush();
            writer.close();

            Log.i(TAG, "Exported " + bookings.size() + " bookings to " + csvFile.getAbsolutePath());
            shareCsvFile(context, csvFile);

        } catch (IOException e) {
            Log.e(TAG, "CSV export failed", e);
        }
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static void shareCsvFile(Context context, File file) {
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Save or share booking export"));
    }
}