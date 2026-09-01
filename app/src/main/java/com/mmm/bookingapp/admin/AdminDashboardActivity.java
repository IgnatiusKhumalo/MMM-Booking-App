package com.mmm.bookingapp.admin;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mmm.bookingapp.R;
import com.mmm.bookingapp.adapters.BookingListAdapter;
import com.mmm.bookingapp.auth.WelcomeActivity;
import com.mmm.bookingapp.models.Booking;
import com.mmm.bookingapp.settings.SettingsActivity;
import com.mmm.bookingapp.utils.CsvExportUtils;
import com.mmm.bookingapp.utils.FirebaseRefs;
import com.mmm.bookingapp.utils.TouchTargetUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Screen 18 - Admin Dashboard (admin root screen).
 *
 * Now includes:
 * - Free-text search (etSearchBookings), combined with the existing
 *   status/location/date filters rather than replacing them - searches
 *   service, location, consultationType, status, and additionalInfo.
 * - CSV export (tvExportCsv) of the currently-filtered booking list,
 *   satisfying the Backup and Recovery requirement with a genuine,
 *   demoable manual export mechanism.
 * - Expanded touch targets (TouchTargetUtils) on btnLogoutAdmin and
 *   btnSettings, both visually correct at their compact size but under
 *   Android's 48dp minimum touch target guideline.
 * - Log calls at key admin actions (filter/search changes, export
 *   result) for the Logging and Monitoring requirement.
 */
public class AdminDashboardActivity extends AppCompatActivity implements BookingListAdapter.OnBookingActionListener {

    private static final String TAG = "AdminDashboard";

    private TextView tvSummaryPending, tvSummaryConfirmed, tvSummaryCompleted, tvSummaryCancelled;
    private Spinner spinnerFilterStatus, spinnerFilterLocation;
    private TextView tvFilterDate, tvClearFilters, tvEmptyState, tvExportCsv;
    private EditText etSearchBookings;
    private RecyclerView recyclerAdminBookings;

    private View barPending, fillerPending, barConfirmed, fillerConfirmed,
            barCompleted, fillerCompleted, barCancelled, fillerCancelled;
    private TextView tvCountPending, tvCountConfirmed, tvCountCompleted, tvCountCancelled;

    private final List<Booking> allBookings = new ArrayList<>();
    private final List<Booking> filteredBookings = new ArrayList<>();
    private BookingListAdapter adapter;

    private String filterStatus = "All";
    private String filterLocation = "All";
    private String filterDate = null; // "yyyy-MM-dd" or null
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvSummaryPending = findViewById(R.id.tvSummaryPending);
        tvSummaryConfirmed = findViewById(R.id.tvSummaryConfirmed);
        tvSummaryCompleted = findViewById(R.id.tvSummaryCompleted);
        tvSummaryCancelled = findViewById(R.id.tvSummaryCancelled);
        spinnerFilterStatus = findViewById(R.id.spinnerFilterStatus);
        spinnerFilterLocation = findViewById(R.id.spinnerFilterLocation);
        tvFilterDate = findViewById(R.id.tvFilterDate);
        tvClearFilters = findViewById(R.id.tvClearFilters);
        tvExportCsv = findViewById(R.id.tvExportCsv);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        etSearchBookings = findViewById(R.id.etSearchBookings);
        recyclerAdminBookings = findViewById(R.id.recyclerAdminBookings);

        barPending = findViewById(R.id.barPending);
        fillerPending = findViewById(R.id.fillerPending);
        barConfirmed = findViewById(R.id.barConfirmed);
        fillerConfirmed = findViewById(R.id.fillerConfirmed);
        barCompleted = findViewById(R.id.barCompleted);
        fillerCompleted = findViewById(R.id.fillerCompleted);
        barCancelled = findViewById(R.id.barCancelled);
        fillerCancelled = findViewById(R.id.fillerCancelled);
        tvCountPending = findViewById(R.id.tvCountPending);
        tvCountConfirmed = findViewById(R.id.tvCountConfirmed);
        tvCountCompleted = findViewById(R.id.tvCountCompleted);
        tvCountCancelled = findViewById(R.id.tvCountCancelled);

        LinearLayout btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        Button btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin);
        btnLogoutAdmin.setOnClickListener(v -> logout());

        // Accessibility fix: expand the touch target of these two compact
        // header controls to Android's 48dp minimum, without changing
        // their visual size.
        View rootHeader = findViewById(android.R.id.content);
        TouchTargetUtils.expandTouchArea(rootHeader, btnLogoutAdmin);
        TouchTargetUtils.expandTouchArea(rootHeader, btnSettings);

        Button btnViewAnalytics = findViewById(R.id.btnViewAnalytics);
        btnViewAnalytics.setOnClickListener(v -> startActivity(new Intent(this, AdminAnalyticsActivity.class)));

        setupFilters();
        setupSearch();

        recyclerAdminBookings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingListAdapter(filteredBookings, true, this);
        recyclerAdminBookings.setAdapter(adapter);

        tvClearFilters.setOnClickListener(v -> clearFilters());
        tvFilterDate.setOnClickListener(v -> showDateFilterPicker());
        tvExportCsv.setOnClickListener(v -> exportCurrentListToCsv());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllBookings();
    }

    private void setupSearch() {
        etSearchBookings.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                searchQuery = s.toString().trim().toLowerCase(Locale.US);
                Log.d(TAG, "Search query changed: \"" + searchQuery + "\"");
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilters() {
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(
                this, R.array.status_filter_options, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterStatus.setAdapter(statusAdapter);

        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(
                this, R.array.location_filter_options, android.R.layout.simple_spinner_item);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterLocation.setAdapter(locationAdapter);

        spinnerFilterStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterStatus = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Status filter changed: " + filterStatus);
                applyFilters();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerFilterLocation.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterLocation = parent.getItemAtPosition(position).toString();
                Log.d(TAG, "Location filter changed: " + filterLocation);
                applyFilters();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void showDateFilterPicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            filterDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            tvFilterDate.setText(String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year));
            Log.d(TAG, "Date filter set: " + filterDate);
            applyFilters();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void clearFilters() {
        filterStatus = "All";
        filterLocation = "All";
        filterDate = null;
        searchQuery = "";
        etSearchBookings.setText("");
        spinnerFilterStatus.setSelection(0);
        spinnerFilterLocation.setSelection(0);
        tvFilterDate.setText(R.string.filter_date_label);
        Log.d(TAG, "Filters and search cleared");
        applyFilters();
    }

    private void loadAllBookings() {
        FirebaseRefs.bookingsCollection().get()
                .addOnSuccessListener(querySnapshot -> {
                    allBookings.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Booking booking = doc.toObject(Booking.class);
                        booking.setBookingId(doc.getId());
                        allBookings.add(booking);
                    }
                    Log.i(TAG, "Loaded " + allBookings.size() + " bookings");
                    updateSummaryCounts();
                    applyFilters();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load bookings", e));
    }

    private void updateSummaryCounts() {
        int pending = 0, confirmed = 0, completed = 0, cancelled = 0;
        for (Booking b : allBookings) {
            switch (b.getStatus()) {
                case "pending": pending++; break;
                case "confirmed": confirmed++; break;
                case "completed": completed++; break;
                case "cancelled": cancelled++; break;
            }
        }
        tvSummaryPending.setText(pending + " " + getString(R.string.summary_pending));
        tvSummaryConfirmed.setText(confirmed + " " + getString(R.string.summary_confirmed));
        tvSummaryCompleted.setText(completed + " " + getString(R.string.summary_completed));
        tvSummaryCancelled.setText(cancelled + " " + getString(R.string.summary_cancelled));

        updateAnalyticsBars(pending, confirmed, completed, cancelled);
    }

    private void updateAnalyticsBars(int pending, int confirmed, int completed, int cancelled) {
        int max = Math.max(1, Math.max(Math.max(pending, confirmed), Math.max(completed, cancelled)));

        setBarWeight(barPending, fillerPending, pending, max);
        setBarWeight(barConfirmed, fillerConfirmed, confirmed, max);
        setBarWeight(barCompleted, fillerCompleted, completed, max);
        setBarWeight(barCancelled, fillerCancelled, cancelled, max);

        tvCountPending.setText(String.valueOf(pending));
        tvCountConfirmed.setText(String.valueOf(confirmed));
        tvCountCompleted.setText(String.valueOf(completed));
        tvCountCancelled.setText(String.valueOf(cancelled));
    }

    private void setBarWeight(View bar, View filler, int count, int max) {
        LinearLayout.LayoutParams barParams = (LinearLayout.LayoutParams) bar.getLayoutParams();
        LinearLayout.LayoutParams fillerParams = (LinearLayout.LayoutParams) filler.getLayoutParams();
        barParams.weight = count;
        fillerParams.weight = Math.max(0, max - count);
        bar.setLayoutParams(barParams);
        filler.setLayoutParams(fillerParams);
    }

    /**
     * Combines the existing dropdown filters with the new free-text
     * search - a booking must pass BOTH the active filters AND (if a
     * search query is present) match at least one searchable field.
     */
    private void applyFilters() {
        filteredBookings.clear();
        for (Booking b : allBookings) {
            if (!"All".equals(filterStatus) && !b.getStatus().equalsIgnoreCase(filterStatus)) continue;

            if (!"All".equals(filterLocation)) {
                String bookingLocation = "in-person".equals(b.getConsultationType()) ? b.getLocation() : "Online";
                if (bookingLocation == null || !bookingLocation.equalsIgnoreCase(filterLocation)) continue;
            }

            if (filterDate != null && !filterDate.equals(b.getDate())) continue;

            if (!searchQuery.isEmpty() && !matchesSearch(b)) continue;

            filteredBookings.add(b);
        }
        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredBookings.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerAdminBookings.setVisibility(filteredBookings.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private boolean matchesSearch(Booking b) {
        String location = "in-person".equals(b.getConsultationType()) ? b.getLocation() : "Online";
        String haystack = safeLower(b.getService()) + " "
                + safeLower(location) + " "
                + safeLower(b.getConsultationType()) + " "
                + safeLower(b.getStatus()) + " "
                + safeLower(b.getAdditionalInfo());
        return haystack.contains(searchQuery);
    }

    private String safeLower(String value) {
        return value != null ? value.toLowerCase(Locale.US) : "";
    }

    private void exportCurrentListToCsv() {
        if (filteredBookings.isEmpty()) {
            Log.w(TAG, "Export attempted with no bookings in the current filtered list");
        }
        Log.i(TAG, "Exporting " + filteredBookings.size() + " bookings to CSV");
        CsvExportUtils.exportBookingsToCsv(this, filteredBookings);
    }

    @Override
    public void onCancelClick(Booking booking) {
        // Not used in admin mode - Cancel button is hidden entirely here,
        // per BookingListAdapter's documented adminView behaviour.
    }

    @Override
    public void onItemClick(Booking booking) {
        Intent intent = new Intent(this, BookingDetailActivity.class);
        intent.putExtra("bookingId", booking.getBookingId());
        startActivity(intent);
    }

    private void logout() {
        Log.i(TAG, "Admin logout");
        FirebaseRefs.getAuth().signOut();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}