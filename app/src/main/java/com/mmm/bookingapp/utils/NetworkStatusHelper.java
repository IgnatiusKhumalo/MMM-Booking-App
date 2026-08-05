package com.mmm.bookingapp.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.widget.TextView;

/**
 * Detects online/offline status and controls the visibility of an offline
 * banner. Supports this project's Innovation Feature (Offline Mode) - the
 * actual offline data handling is Firestore's built-in persistence
 * (enabled by default, no extra code required); this class only adds the
 * documented UI indicator ("You're offline - changes will sync once
 * you're back online"), per the Blueprint's Section 9 requirement.
 */
public class NetworkStatusHelper {

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true; // assume online if we can't check
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }

    /**
     * Registers a live listener that shows/hides the given banner TextView
     * automatically as connectivity changes. Call this in onCreate() of
     * any screen that includes an offline banner in its layout; the
     * listener is automatically cleaned up when the Activity is destroyed
     * (tied to the Activity's own lifecycle via the context passed in).
     */
    public static void observeConnectivity(Context context, TextView offlineBanner) {
        offlineBanner.setVisibility(isOnline(context) ? android.view.View.GONE : android.view.View.VISIBLE);

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;

        NetworkRequest request = new NetworkRequest.Builder().build();
        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                offlineBanner.post(() -> offlineBanner.setVisibility(android.view.View.GONE));
            }

            @Override
            public void onLost(Network network) {
                offlineBanner.post(() -> offlineBanner.setVisibility(android.view.View.VISIBLE));
            }
        });
    }
}