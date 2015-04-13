package net.i2p.android.router.util;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class IntListPreference extends ListPreference {
    public IntListPreference(Context context) {
        super(context);
    }

    public IntListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean persistString(String value) {
        return value != null && persistInt(Integer.valueOf(value));
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if(getSharedPreferences().contains(getKey())) {
            try {
                int intValue = getPersistedInt(0);
                return String.valueOf(intValue);
            } catch (ClassCastException e) {
                return getPersistedString("0");
            }
        } else {
            return defaultReturnValue;
        }
    }
}
