package net.i2p.android.preferences.util;

import android.content.Context;
import android.support.v7.preference.EditTextPreference;
import android.util.AttributeSet;

import net.i2p.android.router.R;

public class PortPreference extends EditTextPreference {
    public PortPreference(Context context) {
        super(context);
    }

    public PortPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PortPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public CharSequence getSummary() {
        int port = getPersistedInt(-1);
        if (port < 0)
            return getContext().getResources().getString(R.string.unset);
        else
            return String.valueOf(port);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        int port = getPersistedInt(-1);
        if (port < 0)
            return defaultReturnValue;
        else
            return String.valueOf(port);
    }

    @Override
    protected boolean persistString(String value) {
        if (value == null || value.isEmpty())
            return persistInt(-1);

        int port;
        try {
            port = Integer.valueOf(value);
            if (port < 0)
                port = -1;
        } catch (NumberFormatException e) {
            port = -1;
        }

        return persistInt(port);
    }
}
