package net.i2p.android.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;

import net.i2p.android.router.R;
import net.i2p.android.router.SettingsActivity;
import net.i2p.android.preferences.util.PortPreference;
import net.i2p.android.router.util.Util;
import net.i2p.router.RouterContext;

public class TransportsPreferenceFragment extends I2PreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle paramBundle, String s) {
        // Load any properties that the router might have changed on us.
        loadProperties();
        addPreferencesFromResource(R.xml.settings_transports);
        setupTransportSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_label_transports);
    }

    @SuppressLint("ApplySharedPref")
    private void loadProperties() {
        Context context= getActivity();
        RouterContext ctx = Util.getRouterContext();
        if (ctx != null) {
            final String udpPortKey = context.getString(R.string.PROP_UDP_INTERNAL_PORT);
            final String ntcpPortKey = context.getString(R.string.PROP_I2NP_NTCP_PORT);
            final String ntcpAutoPortKey = context.getString(R.string.PROP_I2NP_NTCP_AUTO_PORT);

            int udpPort = ctx.getProperty(udpPortKey, -1);
            int ntcpPort = ctx.getProperty(ntcpPortKey, -1);
            boolean ntcpAutoPort = ctx.getBooleanPropertyDefaultTrue(ntcpAutoPortKey);
            if (ntcpPort < 0 && ntcpAutoPort)
                ntcpPort = udpPort;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getInt(udpPortKey, -1) != udpPort ||
                    prefs.getInt(ntcpPortKey, -1) != ntcpPort) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(udpPortKey, udpPort);
                editor.putInt(ntcpPortKey, ntcpPort);
                // commit() instead of apply() because this needs to happen
                // before AdvancedPreferenceFragment loads its Preferences.
                editor.commit();
            }
        }
    }

    private void setupTransportSettings() {
        final Context context= getActivity();
        PreferenceScreen ps = getPreferenceScreen();

        final String udpEnableKey = context.getString(R.string.PROP_ENABLE_UDP);
        final String ntcpEnableKey = context.getString(R.string.PROP_ENABLE_NTCP);
        final String udpPortKey = context.getString(R.string.PROP_UDP_INTERNAL_PORT);
        final String ntcpPortKey = context.getString(R.string.PROP_I2NP_NTCP_PORT);
        final String ntcpAutoPortKey = context.getString(R.string.PROP_I2NP_NTCP_AUTO_PORT);

        final CheckBoxPreference udpEnable = (CheckBoxPreference) ps.findPreference(udpEnableKey);
        final CheckBoxPreference ntcpEnable = (CheckBoxPreference) ps.findPreference(ntcpEnableKey);
        final PortPreference udpPort = (PortPreference) ps.findPreference(udpPortKey);
        final PortPreference ntcpPort = (PortPreference) ps.findPreference(ntcpPortKey);
        final CheckBoxPreference ntcpAutoPort = (CheckBoxPreference) ps.findPreference(ntcpAutoPortKey);

        udpEnable.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final Boolean checked = (Boolean) newValue;
                if (checked || ntcpEnable.isChecked())
                    return true;
                else {
                    Toast.makeText(context, R.string.settings_need_transport_enabled, Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        ntcpEnable.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final Boolean checked = (Boolean) newValue;
                if (checked || udpEnable.isChecked())
                    return true;
                else {
                    Toast.makeText(context, R.string.settings_need_transport_enabled, Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        udpPort.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (ntcpAutoPort.isChecked())
                    ntcpPort.setText((String) newValue);
                return true;
            }
        });

        ntcpAutoPort.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final Boolean checked = (Boolean) newValue;
                if (checked)
                    ntcpPort.setText(udpPort.getText());
                return true;
            }
        });
    }
}
