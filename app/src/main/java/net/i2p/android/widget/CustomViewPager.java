package net.i2p.android.widget;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;

import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;

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
            Toast.makeText(getContext(), Util.getRouterContext() == null ?
                    R.string.router_not_running : R.string.router_shutting_down,
                    Toast.LENGTH_SHORT).show();
    }

    public void setPagingEnabled(boolean enabled) {
        mEnabled = enabled;
        updatePagingState();
    }

    public void updatePagingState() {
        if (!mEnabled && getCurrentItem() != 0)
            setCurrentItem(0);
    }

    public static class SavedState extends BaseSavedState {
        boolean enabled;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(enabled ? 1 : 0);
        }

        @Override
        public String toString() {
            return "CustomViewPager.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " enabled=" + enabled + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        });

        SavedState(Parcel in, ClassLoader loader) {
            super(in);
            if (loader == null) {
                loader = getClass().getClassLoader();
            }
            enabled = in.readInt() != 0;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.enabled = mEnabled;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState)state;
        super.onRestoreInstanceState(ss.getSuperState());

        mEnabled = ss.enabled;
    }
}
