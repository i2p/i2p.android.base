package net.i2p.android.preferences;

import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.app.Fragment;
import android.support.v4.preference.PreferenceFragment;

import net.i2p.android.router.R;
import net.i2p.android.router.SettingsActivity;

public class AdvancedPreferenceFragment extends PreferenceFragment {
    private static final String PREFERENCE_CATEGORY_TRANSPORTS = "preference_category_transports";
    private static final String PREFERENCE_CATEGORY_EXPL_TUNNELS = "preference_category_expl_tunnels";

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        addPreferencesFromResource(R.xml.settings_advanced);

        findPreference(PREFERENCE_CATEGORY_TRANSPORTS)
                .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_TRANSPORTS));
        findPreference(PREFERENCE_CATEGORY_EXPL_TUNNELS)
                .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_EXPL_TUNNELS));
    }

    @Override
    public void onResume() {
        super.onResume();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_label_advanced);
    }

    private class CategoryClickListener implements Preference.OnPreferenceClickListener {
        private String category;

        public CategoryClickListener(String category) {
            this.category = category;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Fragment fragment;
            switch (category) {
                case PREFERENCE_CATEGORY_TRANSPORTS:
                    fragment = new TransportsPreferenceFragment();
                    break;
                case PREFERENCE_CATEGORY_EXPL_TUNNELS:
                    fragment = new ExploratoryPoolPreferenceFragment();
                    break;
                default:
                    throw new AssertionError();
            }

            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment, fragment)
                    .addToBackStack(null)
                    .commit();
            return true;
        }
    }
}
