package net.i2p.android.preferences.util;

import android.content.Context;
//import android.support.v7.preference.ListPreference;
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
        if (getSharedPreferences().contains(getKey())) {
            try {
                getPersistedInt(0);
            } catch (ClassCastException e) {
                // Fix for where this preference was previously stored in a ListPreference
                getSharedPreferences().edit().remove(getKey()).apply();
            }
        }

        return value != null && persistInt(Integer.valueOf(value));
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        if(getSharedPreferences().contains(getKey())) {
            try {
                int intValue = getPersistedInt(0);
                return String.valueOf(intValue);
            } catch (ClassCastException e) {
                return super.getPersistedString("0");
            }
        } else {
            return defaultReturnValue;
        }
    }
}
