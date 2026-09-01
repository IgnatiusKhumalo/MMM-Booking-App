package com.mmm.bookingapp.utils;

import android.graphics.Rect;
import android.view.TouchDelegate;
import android.view.View;

/**
 * Expands a small View's tappable (touch) area without changing its
 * visual size, to meet Android's recommended 48dp minimum touch target
 * (see Android accessibility guidelines). Used specifically on two
 * Admin Dashboard header controls (Logout button, Settings row) that
 * were found to be 36-40dp visually - correct and intentional for the
 * compact header design, but under the accessibility minimum for touch.
 *
 * Must be called after the parent view has been laid out (e.g. inside
 * a post() call), since it needs the target's actual on-screen bounds.
 */
public class TouchTargetUtils {

    private static final int MIN_TOUCH_TARGET_DP = 48;

    public static void expandTouchArea(View parent, View target) {
        parent.post(() -> {
            float density = target.getResources().getDisplayMetrics().density;
            int minSizePx = (int) (MIN_TOUCH_TARGET_DP * density);

            Rect bounds = new Rect();
            target.getHitRect(bounds);

            int widthDeficit = Math.max(0, minSizePx - (bounds.right - bounds.left));
            int heightDeficit = Math.max(0, minSizePx - (bounds.bottom - bounds.top));

            bounds.left -= widthDeficit / 2;
            bounds.right += widthDeficit / 2;
            bounds.top -= heightDeficit / 2;
            bounds.bottom += heightDeficit / 2;

            parent.setTouchDelegate(new TouchDelegate(bounds, target));
        });
    }
}