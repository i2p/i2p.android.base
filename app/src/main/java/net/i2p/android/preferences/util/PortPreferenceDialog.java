package net.i2p.android.preferences.util;

import android.os.Bundle;
//import android.support.v7.preference.EditTextPreferenceDialogFragmentCompat;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

public class PortPreferenceDialog extends EditTextPreferenceDialogFragmentCompat {
    public static PortPreferenceDialog newInstance(String key) {
        final PortPreferenceDialog fragment = new PortPreferenceDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        ((EditText)view.findViewById(android.R.id.edit)).setInputType(InputType.TYPE_CLASS_NUMBER);
    }
}
