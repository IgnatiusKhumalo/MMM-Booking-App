package com.mmm.bookingapp.admin;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
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
 * Redesigned per the detailed mockup spec. Key changes from the previous
 * version, flagged explicitly:
 * - Settings and Logout are now circular icon-only buttons (View, not
 *   LinearLayout/Button) - visual type change only.
 * - The bar chart is now ONE proportional stacked bar instead of 4
 *   separate comparison bars. updateAnalyticsBars() now computes each
 *   segment's weight as its raw share of the total (not relative to the
 *   largest category), and falls back to a neutral grey full-width bar
 *   when there is no data at all, so the chart never renders blank.
 */
public class AdminDashboardActivity extends AppCompatActivity implements BookingListAdapter.OnBookingActionListener {

    private static final String TAG = "AdminDashboard";

    private TextView tvSummaryPending, tvSummaryConfirmed, tvSummaryCompleted, tvSummaryCancelled;
    private Spinner spinnerFilterStatus, spinnerFilterLocation;
    private View tvFilterDate;
    private TextView tvClearFilters, tvEmptyState, tvExportCsv;
    private android.widget.EditText etSearchBookings;
    private RecyclerView recyclerAdminBookings;

    private View barPending, barConfirmed, barCompleted, barCancelled, barEmptyFallback;
    private TextView tvCountPending, tvCountConfirmed, tvCountCompleted, tvCountCancelled;

    private final List<Booking> allBookings = new ArrayList<>();
    private final List<Booking> filteredBookings = new ArrayList<>();
    private BookingListAdapter adapter;

    private String filterStatus = "All";
    private String filterLocation = "All";
    private String filterDate = null;
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
        barConfirmed = findViewById(R.id.barConfirmed);
        barCompleted = findViewById(R.id.barCompleted);
        barCancelled = findViewById(R.id.barCancelled);
        barEmptyFallback = findViewById(R.id.barEmptyFallback);
        tvCountPending = findViewById(R.id.tvCountPending);
        tvCountConfirmed = findViewById(R.id.tvCountConfirmed);
        tvCountCompleted = findViewById(R.id.tvCountCompleted);
        tvCountCancelled = findViewById(R.id.tvCountCancelled);

        // Settings and Logout are now plain circular icon buttons -
        // looked up as View since their XML type is now ImageButton,
        // not LinearLayout/Button as before.
        View btnSettings = findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        View btnLogoutAdmin = findViewById(R.id.btnLogoutAdmin);
        btnLogoutAdmin.setOnClickListener(v -> logout());

        TouchTargetUtils.expandTouchArea(findViewById(android.R.id.content), btnLogoutAdmin);
        TouchTargetUtils.expandTouchArea(findViewById(android.R.id.content), btnSettings);

        android.widget.Button btnViewAnalytics = findViewById(R.id.btnViewAnalytics);
        btnViewAnalytics.setOnClickListener(v -> startActivity(new Intent(this, AdminAnalyticsActivity.class)));

        setupFilters();
        setupSearch();

        recyclerAdminBookings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingListAdapter(filteredBookings, true, this);
        recyclerAdminBookings.setAdapter(adapter);
        recyclerAdminBookings.setNestedScrollingEnabled(false);

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
                applyFilters();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerFilterLocation.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterLocation = parent.getItemAtPosition(position).toString();
                applyFilters();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void showDateFilterPicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            filterDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
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
        tvSummaryPending.setText(String.valueOf(pending));
        tvSummaryConfirmed.setText(String.valueOf(confirmed));
        tvSummaryCompleted.setText(String.valueOf(completed));
        tvSummaryCancelled.setText(String.valueOf(cancelled));

        updateAnalyticsBars(pending, confirmed, completed, cancelled);
    }

    /**
     * Renders ONE proportional stacked bar: each segment's weight is set
     * to its own raw count, so - since all four segments and the fallback
     * share a single row - Android's weight system alone makes each
     * segment's width naturally proportional to the total. When the total
     * is zero, all four real segments collapse to zero width and the
     * fallback grey segment takes the full row instead, so the bar is
     * never left blank.
     */
    private void updateAnalyticsBars(int pending, int confirmed, int completed, int cancelled) {
        int total = pending + confirmed + completed + cancelled;

        setWeight(barPending, pending);
        setWeight(barConfirmed, confirmed);
        setWeight(barCompleted, completed);
        setWeight(barCancelled, cancelled);
        setWeight(barEmptyFallback, total == 0 ? 1 : 0);

        tvCountPending.setText(String.valueOf(pending));
        tvCountConfirmed.setText(String.valueOf(confirmed));
        tvCountCompleted.setText(String.valueOf(completed));
        tvCountCancelled.setText(String.valueOf(cancelled));
    }

    private void setWeight(View bar, int weight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bar.getLayoutParams();
        params.weight = weight;
        bar.setLayoutParams(params);
    }

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
        Log.i(TAG, "Exporting " + filteredBookings.size() + " bookings to CSV");
        CsvExportUtils.exportBookingsToCsv(this, filteredBookings);
    }

    @Override
    public void onCancelClick(Booking booking) {
        // Not used in admin mode.
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