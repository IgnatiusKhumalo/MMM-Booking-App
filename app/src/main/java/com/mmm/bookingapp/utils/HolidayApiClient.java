package com.mmm.bookingapp.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Real RESTful API integration - fulfils the "API Integration" requirement
 * with a genuine external HTTP call, distinct from Firebase's own SDK.
 *
 * Calls the free, public Nager.Date API (https://date.nager.at) to fetch
 * South African public holidays for a given year, and is wired into
 * BookAppointmentActivity to block bookings on those dates - the practice
 * is closed on public holidays even though they may fall Monday-Friday.
 *
 * Uses plain HttpURLConnection and org.json (both built into the Android
 * SDK) rather than adding a new networking library dependency, keeping
 * this consistent with the project's existing "no unnecessary dependencies"
 * approach. Runs on a background thread and delivers results back to the
 * main thread via a callback interface, since network calls must never
 * run on the UI thread.
 */
public class HolidayApiClient {

    private static final String TAG = "HolidayApiClient";
    private static final String BASE_URL = "https://date.nager.at/api/v3/PublicHolidays/";
    private static final String COUNTRY_CODE = "ZA";

    public interface HolidayCallback {
        void onHolidaysLoaded(Set<String> holidayDates); // "yyyy-MM-dd" format
        void onError(String message);
    }

    /**
     * Fetches public holidays for the current year on a background thread.
     * Safe to call from onCreate() - the callback is always delivered on
     * the main thread, so UI updates inside it are safe.
     */
    public static void fetchCurrentYearHolidays(HolidayCallback callback) {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        fetchHolidaysForYear(year, callback);
    }

    public static void fetchHolidaysForYear(int year, HolidayCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(BASE_URL + year + "/" + COUNTRY_CODE);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(8000);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Nager.Date API response code: " + responseCode);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "Unexpected response code fetching holidays: " + responseCode);
                    mainHandler.post(() -> callback.onError("Could not load public holidays (code " + responseCode + ")"));
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();

                Set<String> holidayDates = parseHolidayDates(responseBuilder.toString());
                Log.i(TAG, "Loaded " + holidayDates.size() + " public holidays for " + year);
                mainHandler.post(() -> callback.onHolidaysLoaded(holidayDates));

            } catch (Exception e) {
                Log.e(TAG, "Error fetching public holidays", e);
                mainHandler.post(() -> callback.onError("Network error loading public holidays: " + e.getMessage()));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    /**
     * Parses the Nager.Date JSON array response manually (each object has
     * a "date" field in "yyyy-MM-dd" format already, matching the format
     * used throughout this app's own date handling).
     */
    private static Set<String> parseHolidayDates(String jsonResponse) throws Exception {
        Set<String> dates = new HashSet<>();
        JSONArray array = new JSONArray(jsonResponse);
        for (int i = 0; i < array.length(); i++) {
            JSONObject holiday = array.getJSONObject(i);
            String date = holiday.optString("date", null);
            if (date != null) {
                dates.add(date);
            }
        }
        return dates;
    }
}