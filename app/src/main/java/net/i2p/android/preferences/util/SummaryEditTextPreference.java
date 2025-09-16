package net.i2p.android.preferences.util;

import android.annotation.TargetApi;
import android.content.Context;
//import android.support.v7.preference.EditTextPreference;
import androidx.preference.EditTextPreference;
import android.util.AttributeSet;

public class SummaryEditTextPreference extends EditTextPreference {

    public SummaryEditTextPreference(Context context) {
        super(context);
    }

    public SummaryEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SummaryEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public SummaryEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public CharSequence getSummary() {
        String summary = (String) super.getSummary();
        if (summary == null)
            summary = "%s";
        return String.format(summary, getText());
    }
}
