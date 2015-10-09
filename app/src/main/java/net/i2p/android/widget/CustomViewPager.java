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
    private int mFixedPage;
    private int mFixedPageString;

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mEnabled = false;
        mFixedPage = -1;
        mFixedPageString = 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mEnabled && mFixedPage < 0 && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mEnabled && mFixedPage < 0 && super.onInterceptTouchEvent(event);
    }

    @Override
    public void setCurrentItem(int item) {
        if ((mEnabled && (mFixedPage < 0 || item == mFixedPage))
                || (!mEnabled && item == 0))
            super.setCurrentItem(item);
        else if (!mEnabled)
            Toast.makeText(getContext(), Util.getRouterContext() == null ?
                    R.string.router_not_running : R.string.router_shutting_down,
                    Toast.LENGTH_SHORT).show();
        else if (mFixedPageString > 0)
            Toast.makeText(getContext(), getContext().getString(mFixedPageString),
                    Toast.LENGTH_SHORT).show();
    }

    public void setPagingEnabled(boolean enabled) {
        mEnabled = enabled;
        updatePagingState();
    }

    public void setFixedPage(int page, int res) {
        mFixedPage = page;
        mFixedPageString = res;
        updatePagingState();
    }

    public void updatePagingState() {
        if (mEnabled) {
            if (mFixedPage >= 0 && getCurrentItem() != mFixedPage)
                setCurrentItem(mFixedPage);

        } else if (getCurrentItem() != 0)
            setCurrentItem(0);
    }

    public static class SavedState extends BaseSavedState {
        boolean enabled;
        int fixedPage;
        int fixedPageString;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(enabled ? 1 : 0);
            out.writeInt(fixedPage);
            out.writeInt(fixedPageString);
        }

        @Override
        public String toString() {
            return "CustomViewPager.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " enabled=" + enabled + " fixedPage=" + fixedPage + "}";
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
            fixedPage = in.readInt();
            fixedPageString = in.readInt();
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.enabled = mEnabled;
        ss.fixedPage = mFixedPage;
        ss.fixedPageString = mFixedPageString;
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
        mFixedPage = ss.fixedPage;
        mFixedPageString = ss.fixedPageString;
    }
}
