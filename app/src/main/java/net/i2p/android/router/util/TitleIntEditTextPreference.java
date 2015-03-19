package net.i2p.android.router.util;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.util.AttributeSet;

public class TitleIntEditTextPreference extends EditTextPreference {

    public TitleIntEditTextPreference(Context context) {
        super(context);
        getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
    }

    public TitleIntEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
    }

    public TitleIntEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
    }

    @Override
    public CharSequence getTitle() {
        String title = (String) super.getTitle();
        if (title == null)
            title = "%s";
        return String.format(title, getText());
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        int defaultVal = Integer.valueOf(defaultReturnValue);
        return String.valueOf(getPersistedInt(defaultVal));
    }

    @Override
    protected boolean persistString(String value) {
        return persistInt(Integer.valueOf(value));
    }
}
