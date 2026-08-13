package com.mmm.bookingapp.admin;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.mmm.bookingapp.R;
import com.mmm.bookingapp.utils.AnalyticsHelper;
import com.mmm.bookingapp.utils.FirebaseRefs;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin Analytics Screen (new, beyond the original 20-screen Blueprint).
 * Shows time-bucketed counts (Today / This Week / This Month / This Year)
 * with percentage change vs. the equivalent previous period, for both
 * Bookings Created and New Registrations, plus a simple week-over-week
 * trend projection.
 *
 * Uses Firestore's count() aggregation queries - these execute entirely
 * on Firebase's servers and return only a number, not the underlying
 * documents, which is a genuinely more efficient pattern than the
 * document-download-and-count approach used on the original Admin
 * Dashboard bar chart. This is a deliberate, documented improvement,
 * not an inconsistency - both approaches are correct at this practice's
 * scale, but this one scales better as data grows.
 *
 * The trend projection is explicitly labelled in the UI as a simple
 * heuristic (this week's count adjusted by the most recent week-over-week
 * growth rate) - it is NOT machine learning or true predictive analytics,
 * and is never presented as such, per the documented decision to avoid
 * overclaiming AI capability the app does not have.
 *
 * Depends on a "createdAt" (long, epoch millis) field on both the
 * bookings collection (already existed) and the users collection (added
 * alongside this feature - see models/User.java). Accounts created before
 * this field existed are correctly excluded from date-bucketed counts by
 * Firestore's own inequality-query behaviour, rather than causing an error.
 */
public class AdminAnalyticsActivity extends AppCompatActivity {

    private TextView tvBookingsToday, tvBookingsTodayChange;
    private TextView tvBookingsWeek, tvBookingsWeekChange;
    private TextView tvBookingsMonth, tvBookingsMonthChange;
    private TextView tvBookingsYear, tvBookingsYearChange;

    private TextView tvRegsToday, tvRegsTodayChange;
    private TextView tvRegsWeek, tvRegsWeekChange;
    private TextView tvRegsMonth, tvRegsMonthChange;
    private TextView tvRegsYear, tvRegsYearChange;

    private TextView tvTrendProjection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_analytics);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvBookingsToday = findViewById(R.id.tvBookingsToday);
        tvBookingsTodayChange = findViewById(R.id.tvBookingsTodayChange);
        tvBookingsWeek = findViewById(R.id.tvBookingsWeek);
        tvBookingsWeekChange = findViewById(R.id.tvBookingsWeekChange);
        tvBookingsMonth = findViewById(R.id.tvBookingsMonth);
        tvBookingsMonthChange = findViewById(R.id.tvBookingsMonthChange);
        tvBookingsYear = findViewById(R.id.tvBookingsYear);
        tvBookingsYearChange = findViewById(R.id.tvBookingsYearChange);

        tvRegsToday = findViewById(R.id.tvRegsToday);
        tvRegsTodayChange = findViewById(R.id.tvRegsTodayChange);
        tvRegsWeek = findViewById(R.id.tvRegsWeek);
        tvRegsWeekChange = findViewById(R.id.tvRegsWeekChange);
        tvRegsMonth = findViewById(R.id.tvRegsMonth);
        tvRegsMonthChange = findViewById(R.id.tvRegsMonthChange);
        tvRegsYear = findViewById(R.id.tvRegsYear);
        tvRegsYearChange = findViewById(R.id.tvRegsYearChange);

        tvTrendProjection = findViewById(R.id.tvTrendProjection);

        loadAnalytics();
    }

    private void loadAnalytics() {
        loadCollectionAnalytics(FirebaseRefs.bookingsCollection(),
                tvBookingsToday, tvBookingsTodayChange,
                tvBookingsWeek, tvBookingsWeekChange,
                tvBookingsMonth, tvBookingsMonthChange,
                tvBookingsYear, tvBookingsYearChange,
                true);

        loadCollectionAnalytics(FirebaseRefs.usersCollection(),
                tvRegsToday, tvRegsTodayChange,
                tvRegsWeek, tvRegsWeekChange,
                tvRegsMonth, tvRegsMonthChange,
                tvRegsYear, tvRegsYearChange,
                false);
    }

    private void loadCollectionAnalytics(CollectionReference collection,
                                         TextView tvToday, TextView tvTodayChange,
                                         TextView tvWeek, TextView tvWeekChange,
                                         TextView tvMonth, TextView tvMonthChange,
                                         TextView tvYear, TextView tvYearChange,
                                         boolean isBookings) {

        List<Task<AggregateQuerySnapshot>> tasks = new ArrayList<>();
        tasks.add(countSince(collection, AnalyticsHelper.startOfToday()));
        tasks.add(countBetween(collection, AnalyticsHelper.startOfYesterday(), AnalyticsHelper.startOfToday()));
        tasks.add(countSince(collection, AnalyticsHelper.startOfWeek()));
        tasks.add(countBetween(collection, AnalyticsHelper.startOfWeekOffset(1), AnalyticsHelper.startOfWeek()));
        tasks.add(countSince(collection, AnalyticsHelper.startOfMonth()));
        tasks.add(countBetween(collection, AnalyticsHelper.startOfPreviousMonth(), AnalyticsHelper.startOfMonth()));
        tasks.add(countSince(collection, AnalyticsHelper.startOfYear()));
        tasks.add(countBetween(collection, AnalyticsHelper.startOfPreviousYear(), AnalyticsHelper.startOfYear()));

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            long today = ((AggregateQuerySnapshot) results.get(0)).getCount();
            long yesterday = ((AggregateQuerySnapshot) results.get(1)).getCount();
            long week = ((AggregateQuerySnapshot) results.get(2)).getCount();
            long lastWeek = ((AggregateQuerySnapshot) results.get(3)).getCount();
            long month = ((AggregateQuerySnapshot) results.get(4)).getCount();
            long lastMonth = ((AggregateQuerySnapshot) results.get(5)).getCount();
            long year = ((AggregateQuerySnapshot) results.get(6)).getCount();
            long lastYear = ((AggregateQuerySnapshot) results.get(7)).getCount();

            tvToday.setText(String.valueOf(today));
            tvTodayChange.setText(AnalyticsHelper.formatPercentChange(yesterday, today));
            tvWeek.setText(String.valueOf(week));
            tvWeekChange.setText(AnalyticsHelper.formatPercentChange(lastWeek, week));
            tvMonth.setText(String.valueOf(month));
            tvMonthChange.setText(AnalyticsHelper.formatPercentChange(lastMonth, month));
            tvYear.setText(String.valueOf(year));
            tvYearChange.setText(AnalyticsHelper.formatPercentChange(lastYear, year));

            if (isBookings) {
                updateTrendProjection(lastWeek, week);
            }
        });
    }

    private Task<AggregateQuerySnapshot> countSince(CollectionReference collection, long start) {
        return collection.whereGreaterThanOrEqualTo("createdAt", start).count().get(AggregateSource.SERVER);
    }

    private Task<AggregateQuerySnapshot> countBetween(CollectionReference collection, long start, long end) {
        return collection.whereGreaterThanOrEqualTo("createdAt", start)
                .whereLessThan("createdAt", end)
                .count().get(AggregateSource.SERVER);
    }

    private void updateTrendProjection(long lastWeek, long thisWeek) {
        if (lastWeek == 0) {
            tvTrendProjection.setText(R.string.trend_insufficient_data);
            return;
        }
        double growthRate = (double) (thisWeek - lastWeek) / lastWeek;
        long projectedNextWeek = Math.max(0, Math.round(thisWeek * (1 + growthRate)));
        tvTrendProjection.setText(getString(R.string.trend_projection_text, projectedNextWeek));
    }
}