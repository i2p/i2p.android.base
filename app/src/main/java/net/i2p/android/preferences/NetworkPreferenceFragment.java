package net.i2p.android.preferences;

import android.os.Bundle;

import net.i2p.android.router.R;
import net.i2p.android.router.SettingsActivity;

public class NetworkPreferenceFragment extends I2PreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle paramBundle, String s) {
        addPreferencesFromResource(R.xml.settings_net);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_label_bandwidth_net);
    }
}
