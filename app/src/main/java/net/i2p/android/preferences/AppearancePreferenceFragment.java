package net.i2p.android.preferences;

import android.os.Bundle;

import net.i2p.android.router.R;
import net.i2p.android.router.SettingsActivity;

public class AppearancePreferenceFragment extends I2PreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle paramBundle, String s) {
        addPreferencesFromResource(R.xml.settings_appearance);
    }

    @Override
    public void onStart() {
        super.onStart();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(
                (SettingsActivity) getActivity()
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_label_appearance);
    }

    @Override
    public void onStop() {
        super.onStop();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                (SettingsActivity) getActivity()
        );
    }
}
