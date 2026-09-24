package com.mmm.bookingapp.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.mmm.bookingapp.R;

/**
 * A plain LinearLayout that caps its own width at @dimen/content_max_width
 * and stays horizontally centered when the parent is wider than that cap.
 * On phones, content_max_width is set high enough to never actually
 * apply. On 7"+ tablets, it caps the content column to a comfortable
 * width instead of stretching every button and line of text edge-to-edge
 * across a 10" screen. This is the ONLY structural change made for
 * tablet support - every screen's own internal layout is untouched.
 */
public class MaxWidthLinearLayout extends LinearLayout {

    public MaxWidthLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxWidth = getResources().getDimensionPixelSize(R.dimen.content_max_width);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        if (widthSize > maxWidth) {
            int newMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY);
            super.onMeasure(newMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}