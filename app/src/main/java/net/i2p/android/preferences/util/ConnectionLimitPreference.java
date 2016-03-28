package net.i2p.android.preferences.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

import net.i2p.android.router.R;

public class ConnectionLimitPreference extends EditTextPreference {
    private boolean mValueInTitle;

    public ConnectionLimitPreference(Context context) {
        this(context, null);
    }

    public ConnectionLimitPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ConnectionLimitPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    void init(Context context, AttributeSet attrs) {
        TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.ConnectionLimitPreference, 0, 0);
        mValueInTitle = attr.getBoolean(R.styleable.ConnectionLimitPreference_clp_valueInTitle, false);
        attr.recycle();
    }

    @Override
    public CharSequence getTitle() {
        if (mValueInTitle)
            return formatValue((String) super.getTitle());
        else
            return super.getTitle();
    }

    @Override
    public CharSequence getSummary() {
        if (mValueInTitle)
            return super.getSummary();
        else
            return formatValue((String) super.getSummary());
    }

    private CharSequence formatValue(String format) {
        String text = getText();
        if ("0".equals(text))
            text = getContext().getString(R.string.unlimited);

        if (format == null)
            format = "%s";
        return String.format(format, text);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if(getSharedPreferences().contains(getKey())) {
            int intValue = getPersistedInt(0);
            return String.valueOf(intValue);
        } else {
            return defaultReturnValue;
        }
    }

    @Override
    protected boolean persistString(String value) {
        return value != null && persistInt(Integer.valueOf(value));
    }
}
