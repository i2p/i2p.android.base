package net.i2p.android.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;

import net.i2p.android.router.R;

public class CustomViewPager extends ViewPager {
    private boolean mEnabled;

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mEnabled = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mEnabled && super.onInterceptTouchEvent(event);
    }

    @Override
    public void setCurrentItem(int item) {
        if (mEnabled || item == 0)
            super.setCurrentItem(item);
        else
            Toast.makeText(getContext(), R.string.router_not_running, Toast.LENGTH_SHORT).show();
    }

    public void setPagingEnabled(boolean enabled) {
        mEnabled = enabled;
    }
}
