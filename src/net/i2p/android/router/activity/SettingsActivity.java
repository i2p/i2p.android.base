package net.i2p.android.router.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import net.i2p.android.router.R;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings1);
    }
}
