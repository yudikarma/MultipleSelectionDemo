package com.yudikarma.multipleselectiondemo.utils;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.GridLayoutManager;

/**
 * No Predictive Animations GridLayoutManager
 * https://stackoverflow.com/questions/30220771/recyclerview-inconsistency-detected-invalid-item-position
 */
public class NpaGridLayoutManager extends GridLayoutManager {
    /**
     * Disable predictive animations. There is a bug in RecyclerView which causes views that
     * are being reloaded to pull invalid ViewHolders from the internal recycler stack if the
     * adapter size has decreased since the ViewHolder was recycled.
     */
    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }

    public NpaGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public NpaGridLayoutManager(Context context, int spanCount) {
        super(context, spanCount);
    }

    public NpaGridLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
        super(context, spanCount, orientation, reverseLayout);
    }
}
