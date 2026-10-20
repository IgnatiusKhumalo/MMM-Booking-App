package com.mmm.bookingapp.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
 * Admin Analytics Screen.
 *
 * For both Bookings Created and New Registrations it shows Today / This
 * Week / This Month / This Year counts with the percentage change against
 * the previous equivalent period. Counts come from Firestore's
 * server-side count() aggregation, so no booking or user documents are
 * downloaded just to be counted.
 *
 * Each card also draws four bars comparing the four periods. The bars use
 * plain Views sized by layout weights (no charting library): each bar is
 * scaled against the LARGEST of the four values, not summed to 100%,
 * because the periods overlap (today is inside this week, and so on).
 *
 * The next-week figure is a simple trend estimate from the latest
 * week-over-week change. It is labelled as such on screen - it is not AI
 * or machine learning.
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

    private View barChartBookingsToday, barChartBookingsWeek, barChartBookingsMonth, barChartBookingsYear;
    private View fillerChartBookingsToday, fillerChartBookingsWeek, fillerChartBookingsMonth, fillerChartBookingsYear;
    private View barChartRegsToday, barChartRegsWeek, barChartRegsMonth, barChartRegsYear;
    private View fillerChartRegsToday, fillerChartRegsWeek, fillerChartRegsMonth, fillerChartRegsYear;

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

        barChartBookingsToday = findViewById(R.id.barChartBookingsToday);
        fillerChartBookingsToday = findViewById(R.id.fillerChartBookingsToday);
        barChartBookingsWeek = findViewById(R.id.barChartBookingsWeek);
        fillerChartBookingsWeek = findViewById(R.id.fillerChartBookingsWeek);
        barChartBookingsMonth = findViewById(R.id.barChartBookingsMonth);
        fillerChartBookingsMonth = findViewById(R.id.fillerChartBookingsMonth);
        barChartBookingsYear = findViewById(R.id.barChartBookingsYear);
        fillerChartBookingsYear = findViewById(R.id.fillerChartBookingsYear);

        barChartRegsToday = findViewById(R.id.barChartRegsToday);
        fillerChartRegsToday = findViewById(R.id.fillerChartRegsToday);
        barChartRegsWeek = findViewById(R.id.barChartRegsWeek);
        fillerChartRegsWeek = findViewById(R.id.fillerChartRegsWeek);
        barChartRegsMonth = findViewById(R.id.barChartRegsMonth);
        fillerChartRegsMonth = findViewById(R.id.fillerChartRegsMonth);
        barChartRegsYear = findViewById(R.id.barChartRegsYear);
        fillerChartRegsYear = findViewById(R.id.fillerChartRegsYear);

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
                updateChartBars(today, week, month, year,
                        barChartBookingsToday, fillerChartBookingsToday,
                        barChartBookingsWeek, fillerChartBookingsWeek,
                        barChartBookingsMonth, fillerChartBookingsMonth,
                        barChartBookingsYear, fillerChartBookingsYear);
            } else {
                updateChartBars(today, week, month, year,
                        barChartRegsToday, fillerChartRegsToday,
                        barChartRegsWeek, fillerChartRegsWeek,
                        barChartRegsMonth, fillerChartRegsMonth,
                        barChartRegsYear, fillerChartRegsYear);
            }
        });
    }

    /**
     * Sizes the four comparison bars: each bar's weight is its own value
     * and its filler's weight is the remainder up to the largest of the
     * four values. When all four values are zero the bars simply have no
     * width - an honest "no data yet" state, not an error.
     */
    private void updateChartBars(long today, long week, long month, long year,
                                 View barToday, View fillerToday,
                                 View barWeek, View fillerWeek,
                                 View barMonth, View fillerMonth,
                                 View barYear, View fillerYear) {
        long max = Math.max(1, Math.max(Math.max(today, week), Math.max(month, year)));

        setBarWeight(barToday, fillerToday, today, max);
        setBarWeight(barWeek, fillerWeek, week, max);
        setBarWeight(barMonth, fillerMonth, month, max);
        setBarWeight(barYear, fillerYear, year, max);
    }

    private void setBarWeight(View bar, View filler, long value, long max) {
        LinearLayout.LayoutParams barParams = (LinearLayout.LayoutParams) bar.getLayoutParams();
        LinearLayout.LayoutParams fillerParams = (LinearLayout.LayoutParams) filler.getLayoutParams();
        barParams.weight = (int) value;
        fillerParams.weight = (int) Math.max(0, max - value);
        bar.setLayoutParams(barParams);
        filler.setLayoutParams(fillerParams);
    }

    private Task<AggregateQuerySnapshot> countSince(CollectionReference collection, long start) {
        return collection.whereGreaterThanOrEqualTo("createdAt", start).count().get(AggregateSource.SERVER);
    }

    private Task<AggregateQuerySnapshot> countBetween(CollectionReference collection, long start, long end) {
        return collection.whereGreaterThanOrEqualTo("createdAt", start)
                .whereLessThan("createdAt", end)
                .count().get(AggregateSource.SERVER);
    }

    /** Simple trend estimate: this week's count grown by the latest week-over-week rate. */
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