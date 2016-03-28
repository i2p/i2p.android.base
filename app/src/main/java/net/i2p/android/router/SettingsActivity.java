package net.i2p.android.router;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;

import net.i2p.android.I2PActivity;
import net.i2p.android.preferences.AdvancedPreferenceFragment;
import net.i2p.android.preferences.AppearancePreferenceFragment;
import net.i2p.android.preferences.GraphsPreferenceFragment;
import net.i2p.android.preferences.LoggingPreferenceFragment;
import net.i2p.android.preferences.NetworkPreferenceFragment;
import net.i2p.android.router.addressbook.AddressbookSettingsActivity;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.util.LocaleManager;

public class SettingsActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String PREFERENCE_CATEGORY = "preference_category";
    public static final String PREFERENCE_CATEGORY_NETWORK = "preference_category_network";
    public static final String PREFERENCE_CATEGORY_GRAPHS = "preference_category_graphs";
    public static final String PREFERENCE_CATEGORY_LOGGING = "preference_category_logging";
    public static final String PREFERENCE_CATEGORY_ADDRESSBOOK = "preference_category_addressbook";
    public static final String PREFERENCE_CATEGORY_APPEARANCE = "preference_category_appearance";
    public static final String PREFERENCE_CATEGORY_ADVANCED = "preference_category_advanced";

    private final LocaleManager localeManager = new LocaleManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        localeManager.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Fragment fragment;
        String category = getIntent().getStringExtra(PREFERENCE_CATEGORY);
        if (category != null)
            fragment = getFragmentForCategory(category);
        else
            fragment = new SettingsFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment, fragment)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        localeManager.onResume(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            Intent intent = new Intent(this, I2PActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
        return true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getResources().getString(R.string.PREF_LANGUAGE))) {
            localeManager.onResume(this);
            Intent intent = new Intent(RouterService.LOCAL_BROADCAST_LOCALE_CHANGED);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle paramBundle, String s) {
            migrateOldSettings();

            addPreferencesFromResource(R.xml.settings);

            this.findPreference(PREFERENCE_CATEGORY_NETWORK)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_NETWORK));
            this.findPreference(PREFERENCE_CATEGORY_GRAPHS)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_GRAPHS));
            this.findPreference(PREFERENCE_CATEGORY_LOGGING)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_LOGGING));
            this.findPreference(PREFERENCE_CATEGORY_ADDRESSBOOK)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_ADDRESSBOOK));
            this.findPreference(PREFERENCE_CATEGORY_APPEARANCE)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_APPEARANCE));
            this.findPreference(PREFERENCE_CATEGORY_ADVANCED)
                    .setOnPreferenceClickListener(new CategoryClickListener(PREFERENCE_CATEGORY_ADVANCED));
        }

        private void migrateOldSettings() {
            SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
            try {
                prefs.getInt("i2np.bandwidth.inboundKBytesPerSecond", 0);
            } catch (ClassCastException e) {
                // Migrate pre-0.9.25 settings
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("i2np.bandwidth.inboundKBytesPerSecond");
                editor.putInt("i2np.bandwidth.inboundKBytesPerSecond", Integer.parseInt(
                        prefs.getString("i2np.bandwidth.inboundKBytesPerSecond", "100")));
                editor.remove("i2np.bandwidth.outboundKBytesPerSecond");
                editor.putInt("i2np.bandwidth.outboundKBytesPerSecond", Integer.parseInt(
                        prefs.getString("i2np.bandwidth.outboundKBytesPerSecond", "100")));
                editor.remove("i2np.ntcp.maxConnections");
                editor.putInt("i2np.ntcp.maxConnections", Integer.parseInt(
                        prefs.getString("i2np.ntcp.maxConnections", "32")));
                editor.remove("i2np.udp.maxConnections");
                editor.putInt("i2np.udp.maxConnections", Integer.parseInt(
                        prefs.getString("i2np.udp.maxConnections", "32")));
                editor.apply();
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.menu_settings);
        }

        private class CategoryClickListener implements Preference.OnPreferenceClickListener {
            private String category;

            public CategoryClickListener(String category) {
                this.category = category;
            }

            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (PREFERENCE_CATEGORY_ADDRESSBOOK.equals(category)) {
                    Intent i = new Intent(getActivity(), AddressbookSettingsActivity.class);
                    startActivity(i);
                    return true;
                }

                Fragment fragment = getFragmentForCategory(category);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment, fragment)
                        .addToBackStack(null)
                        .commit();
                return true;
            }
        }
    }

    private static Fragment getFragmentForCategory(String category) {
        switch (category) {
            case PREFERENCE_CATEGORY_NETWORK:
                return new NetworkPreferenceFragment();
            case PREFERENCE_CATEGORY_GRAPHS:
                return new GraphsPreferenceFragment();
            case PREFERENCE_CATEGORY_LOGGING:
                return new LoggingPreferenceFragment();
            case PREFERENCE_CATEGORY_APPEARANCE:
                return new AppearancePreferenceFragment();
            case PREFERENCE_CATEGORY_ADVANCED:
                return new AdvancedPreferenceFragment();
            default:
                throw new AssertionError();
        }
    }
}
