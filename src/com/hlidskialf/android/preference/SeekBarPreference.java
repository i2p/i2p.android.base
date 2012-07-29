/*
 * The following code was written by Matthew Wiggins
 * and is released under the APACHE 2.0 license
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Slight modifications and bugfixes by Sponge <sponge@mail.i2p>
 * These modifications are released under the WTFPL (any version)
 *
 * We don't need negative numbers yet, and may never need to.
 *
 * XML Usage example:
 *
 * <com.hlidskialf.android.preference.SeekBarPreference android:key="duration"
 *      android:title="Duration of something"
 *      android:summary="How long something will last"
 *      android:dialogMessage="Something duration"
 *      android:defaultValue="5"
 *      android:text=" minutes"
 *      android:max="60"
 *      />
 *
 */
package com.hlidskialf.android.preference;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

    private static final String androidns = "http://schemas.android.com/apk/res/android";
    private SeekBar mSeekBar;
    private TextView mSplashText;
    private TextView mValueText;
    private Context mContext;
    private String mDialogMessage, mSuffix;
    private String mDefault = "0";
    private int mMax = 0;
    private int mValue = 0;
    private int mDirection = LinearLayout.HORIZONTAL;


    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mDialogMessage = attrs.getAttributeValue(androidns, "dialogMessage");
        mSuffix = attrs.getAttributeValue(androidns, "text");
        mDefault = attrs.getAttributeValue(androidns, "defaultValue");
        mMax = Integer.parseInt(attrs.getAttributeValue(androidns, "max"));
        if (attrs.getAttributeValue(androidns, "direction") != null) {
            mDirection = Integer.parseInt(attrs.getAttributeValue(androidns, "direction"));
        }
    }

    @Override
    protected View onCreateDialogView() {
        LinearLayout.LayoutParams params;
        LinearLayout layout = new LinearLayout(mContext);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 10);

        // Set the width so that it is as usable as possible.
        // We multiplymMax so that the smaller ranges will get a bigger area.

        if (mDirection == LinearLayout.HORIZONTAL) {
            layout.setMinimumWidth(mMax*5);
        } else {
            layout.setMinimumHeight(mMax*5);
        }

        mSplashText = new TextView(mContext);
        if (mDialogMessage != null) {
            mSplashText.setText(mDialogMessage);
        }
        layout.addView(mSplashText);

        mValueText = new TextView(mContext);
        mValueText.setGravity(Gravity.CENTER_HORIZONTAL);
        mValueText.setTextSize(32);
        params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.addView(mValueText, params);

        mSeekBar = new SeekBar(mContext);
        mSeekBar.setOnSeekBarChangeListener(this);
        // Move the bar away from the changing text, so you can see it, and
        // move it away from the edges to improve usability for the end-ranges.
        mSeekBar.setPadding(6, 30, 6, 6);
        layout.addView(mSeekBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (shouldPersist()) {
            mValue = Integer.parseInt(getPersistedString(mDefault));
        }
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
        return layout;
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue) {
        super.onSetInitialValue(restore, defaultValue);
        if (restore) {
            mValue = shouldPersist() ? Integer.parseInt(getPersistedString(mDefault)) : 0;
        } else {
            mValue = (Integer) defaultValue;
        }
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch) {
        String t = String.valueOf(value);
        mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));
        if (shouldPersist()) {
            persistString(t);
        }
        callChangeListener(new Integer(value));
    }

    public void onStartTrackingTouch(SeekBar seek) {
    }

    public void onStopTrackingTouch(SeekBar seek) {
    }

    public void setMax(int max) {
        mMax = max;
    }

    public int getMax() {
        return mMax;
    }

    public void setProgress(int progress) {
        mValue = progress;
        if (mSeekBar != null) {
            mSeekBar.setProgress(progress);
        }
    }

    public int getProgress() {
        return mValue;
    }
}
