package com.pavelsikun.seekbarpreference;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import net.i2p.android.router.R;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

/**
 * Based on MaterialSeekBarController created by mrbimc on 30.09.15.
 */
public class MaterialSeekBarController implements DiscreteSeekBar.OnProgressChangeListener {

    private final String TAG = getClass().getName();

    public static final int DEFAULT_CURRENT_VALUE = 50;
    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;
    private static final String DEFAULT_MEASUREMENT_UNIT = "";

    private int mMaxValue;
    private int mMaxDigits;
    private int mMinValue;
    private int mCurrentValue;
    private String mMeasurementUnit;

    private DiscreteSeekBar mSeekBar;
    private TextView mSeekBarValue;
    private TextView mMeasurementUnitView;

    private String mTitle;
    private String mSummary;

    private Context mContext;

    private Persistable mPersistable;

    public MaterialSeekBarController(Context context, AttributeSet attrs, Persistable persistable) {
        mContext = context;
        mPersistable = persistable;
        init(attrs, null);
    }

    public MaterialSeekBarController(Context context, AttributeSet attrs, View view, Persistable persistable) {
        mContext = context;
        mPersistable = persistable;
        init(attrs, view);
    }

    private void init(AttributeSet attrs, View view) {
        setValuesFromXml(attrs);
        if(view != null) onBindView(view);
    }
    private void setValuesFromXml(@Nullable AttributeSet attrs) {
        if (attrs == null) {
            mCurrentValue = DEFAULT_CURRENT_VALUE;
            mMinValue = DEFAULT_MIN_VALUE;
            mMaxValue = DEFAULT_MAX_VALUE;
            mMeasurementUnit = DEFAULT_MEASUREMENT_UNIT;
        } else {
            TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);
            try {
                mMinValue = a.getInt(R.styleable.SeekBarPreference_msbp_minValue, DEFAULT_MIN_VALUE);
                mMaxValue = a.getInt(R.styleable.SeekBarPreference_msbp_maxValue, DEFAULT_MAX_VALUE);
                mCurrentValue = a.getInt(R.styleable.SeekBarPreference_msbp_defaultValue, DEFAULT_CURRENT_VALUE);

                mTitle = a.getString(R.styleable.SeekBarPreference_msbp_title);
                mSummary = a.getString(R.styleable.SeekBarPreference_msbp_summary);

                if(mCurrentValue < mMinValue) mCurrentValue = (mMaxValue - mMinValue) / 2;
                mMeasurementUnit = a.getString(R.styleable.SeekBarPreference_msbp_measurementUnit);
                if (mMeasurementUnit == null)
                    mMeasurementUnit = DEFAULT_MEASUREMENT_UNIT;
            } finally {
                a.recycle();
            }
        }
        mMaxDigits = (int) Math.log10(mMaxValue) + 1;
    }

    public void setOnPersistListener(Persistable persistable) {
        mPersistable = persistable;
    }

    public void onBindView(@NonNull View view) {

        mSeekBar = (DiscreteSeekBar) view.findViewById(R.id.seekbar);
        mSeekBar.setMin(mMinValue);
        mSeekBar.setMax(mMaxValue);
        mSeekBar.setOnProgressChangeListener(this);

        mSeekBarValue = (TextView) view.findViewById(R.id.seekbar_value);
        setPaddedValue(mCurrentValue);

        mMeasurementUnitView = (TextView) view.findViewById(R.id.measurement_unit);
        mMeasurementUnitView.setText(mMeasurementUnit);

        mSeekBar.setProgress(mCurrentValue);

        if (!view.isEnabled()) {
            mSeekBar.setEnabled(false);
            mSeekBarValue.setEnabled(false);
        }

        if(mTitle != null || mSummary != null) {
            TextView title = (TextView) view.findViewById(android.R.id.title);
            TextView summary = (TextView) view.findViewById(android.R.id.summary);

            if(mTitle != null) title.setText(mTitle);
            if(mSummary != null) summary.setText(mSummary);
        }
    }

    protected void onSetInitialValue(boolean restoreValue, @NonNull Object defaultValue) {
        mCurrentValue = (mMaxValue - mMinValue) / 2;
        try {
            mCurrentValue = (Integer) defaultValue;
        } catch (Exception ex) {
            Log.e(TAG, "Invalid default value: " + defaultValue.toString());
        }
    }

    public void setEnabled(boolean enabled) {
        if (mSeekBar != null) mSeekBar.setEnabled(enabled);
        if (mSeekBarValue != null) mSeekBarValue.setEnabled(enabled);
    }

    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        if (mSeekBar != null) mSeekBar.setEnabled(!disableDependent);
        if (mSeekBarValue != null) mSeekBarValue.setEnabled(!disableDependent);
    }

    //SeekBarListener:
    @Override
    public void onProgressChanged(@NonNull DiscreteSeekBar seekBar, int progress, boolean fromUser) {
        mCurrentValue = progress;
        setPaddedValue(progress);
    }

    @Override
    public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(@NonNull DiscreteSeekBar seekBar) {
        setCurrentValue(mCurrentValue);
    }

    private void setPaddedValue(int value) {
        mSeekBarValue.setText(String.format("%0" + mMaxDigits +"d", value));
    }


    //public methods for manipulating this widget from java:
    public void setCurrentValue(int value) {
        mCurrentValue = value;
        if (mPersistable != null) mPersistable.onPersist(value);
    }

    public int getCurrentValue() {
        return mCurrentValue;
    }


    public void setMaxValue(int maxValue) {
        mMaxValue = maxValue;
        if (mSeekBar != null) mSeekBar.setMax(mMaxValue);
    }

    public int getMaxValue() {
        return mMaxValue;
    }


    public void setMinValue(int minValue) {
        mMinValue = minValue;
        if (mSeekBar != null) mSeekBar.setMin(mMinValue);
    }

    public int getMinValue() {
        return mMinValue;
    }


    public void setMeasurementUnit(String measurementUnit) {
        mMeasurementUnit = measurementUnit;
        if (mMeasurementUnitView != null) mMeasurementUnitView.setText(mMeasurementUnit);
    }

    public String getMeasurementUnit() {
        return mMeasurementUnit;
    }

    static int pxFromDp(int dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}
