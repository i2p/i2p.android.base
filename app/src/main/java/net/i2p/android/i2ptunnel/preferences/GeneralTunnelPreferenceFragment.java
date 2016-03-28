package net.i2p.android.i2ptunnel.preferences;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;

import net.i2p.android.i2ptunnel.util.TunnelLogic;
import net.i2p.android.i2ptunnel.util.TunnelUtil;
import net.i2p.android.router.R;
import net.i2p.util.Addresses;

import java.util.Set;

public class GeneralTunnelPreferenceFragment extends BaseTunnelPreferenceFragment {
    private CheckBoxPreference persistentKeys;

    public static GeneralTunnelPreferenceFragment newInstance(int tunnelId) {
        GeneralTunnelPreferenceFragment f = new GeneralTunnelPreferenceFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TUNNEL_ID, tunnelId);
        f.setArguments(args);
        return f;
    }

    @Override
    protected void loadPreferences() {
        String type = TunnelUtil.getController(mGroup, mTunnelId).getType();
        new TunnelPreferences(type).runLogic();
    }

    @Override
    public void onStart() {
        super.onStart();

        // In case this was changed when toggling NEW_KEYS and then we navigated back
        if (persistentKeys != null)
            persistentKeys.setChecked(getPreferenceManager().getSharedPreferences().getBoolean(
                    getString(R.string.TUNNEL_OPT_PERSISTENT_KEY),
                    getResources().getBoolean(R.bool.DEFAULT_PERSISTENT_KEY)
            ));
    }

    class TunnelPreferences extends TunnelLogic {
        PreferenceScreen ps;
        PreferenceCategory generalCategory;
        PreferenceCategory portCategory;

        public TunnelPreferences(String type) {
            super(type);
        }

        @Override
        protected void general() {
            addPreferencesFromResource(R.xml.tunnel_gen);
            ps = getPreferenceScreen();
            generalCategory = (PreferenceCategory) ps.findPreference(
                    getString(R.string.TUNNEL_CAT_GENERAL));
            portCategory = (PreferenceCategory) ps.findPreference(
                    getString(R.string.TUNNEL_CAT_PORT));
        }

        @Override
        protected void generalClient() {
            addPreferencesFromResource(R.xml.tunnel_gen_client, generalCategory);

            // PERSISTENT_KEY and NEW_KEYS can't be set simultaneously
            persistentKeys = (CheckBoxPreference) findPreference(getString(R.string.TUNNEL_OPT_PERSISTENT_KEY));
            persistentKeys.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    final SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
                    if ((Boolean) o && prefs.getBoolean(getString(R.string.TUNNEL_OTP_NEW_KEYS),
                            getResources().getBoolean(R.bool.DEFAULT_NEW_KEYS))) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(R.string.persistent_key_conflict_title)
                                .setMessage(R.string.persistent_key_conflict_msg)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putBoolean(getString(R.string.TUNNEL_OTP_NEW_KEYS), false);
                                        editor.apply();
                                        persistentKeys.setChecked(true);
                                    }
                                })
                                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                    }
                                });
                        builder.show();
                        return false;
                    } else
                        return true;
                }
            });
        }

        @Override
        protected void generalClientStreamr(boolean isStreamr) {
            if (isStreamr) {
                generalCategory.removePreference(generalCategory.findPreference(getString(R.string.TUNNEL_SHARED_CLIENT)));
                addPreferencesFromResource(R.xml.tunnel_gen_server_port, portCategory);
                portCategory.removePreference(portCategory.findPreference(getString(R.string.TUNNEL_TARGET_PORT)));
                portCategory.removePreference(portCategory.findPreference(getString(R.string.TUNNEL_USE_SSL)));
            }
        }

        @Override
        protected void generalClientPort() {
            addPreferencesFromResource(R.xml.tunnel_gen_client_port, portCategory);
        }

        @Override
        protected void generalClientPortStreamr(boolean isStreamr) {
            ListPreference reachableBy = (ListPreference) portCategory.findPreference(getString(R.string.TUNNEL_INTERFACE));
            if (isStreamr)
                portCategory.removePreference(reachableBy);
            else
                setupReachableBy(reachableBy);
        }

        private void setupReachableBy(final ListPreference reachableBy) {
            reachableBy.setEnabled(false);
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    Set<String> interfaceSet = Addresses.getAllAddresses();
                    final String[] interfaces = interfaceSet.toArray(new String[interfaceSet.size()]);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            reachableBy.setEntries(interfaces);
                            reachableBy.setEntryValues(interfaces);
                            reachableBy.setEnabled(true);
                        }
                    });
                    return null;
                }
            }.execute();
        }

        @Override
        protected void generalClientProxy(boolean isProxy) {
            if (isProxy) {
                generalCategory.removePreference(generalCategory.findPreference(getString(R.string.TUNNEL_DEST)));
                addPreferencesFromResource(R.xml.tunnel_gen_client_proxy);
            }
        }

        @Override
        protected void generalClientProxyHttp(boolean isHttp) {
            if (!isHttp)
                ps.removePreference(ps.findPreference(getString(R.string.TUNNEL_HTTPCLIENT_SSL_OUTPROXIES)));
        }

        @Override
        protected void generalClientStandardOrIrc(boolean isStandardOrIrc) {
            if (!isStandardOrIrc)
                portCategory.removePreference(portCategory.findPreference(getString(R.string.TUNNEL_USE_SSL)));
        }

        @Override
        protected void generalClientIrc() {
            addPreferencesFromResource(R.xml.tunnel_gen_client_irc);
        }

        @Override
        protected void generalServerHttp() {
            addPreferencesFromResource(R.xml.tunnel_gen_server_http, generalCategory);
        }

        @Override
        protected void generalServerHttpBidirOrStreamr(boolean isStreamr) {
            addPreferencesFromResource(R.xml.tunnel_gen_client_port, portCategory);
            portCategory.removePreference(portCategory.findPreference(getString(R.string.TUNNEL_USE_SSL)));
            if (isStreamr)
                portCategory.removePreference(portCategory.findPreference(getString(R.string.TUNNEL_LISTEN_PORT)));

            setupReachableBy((ListPreference) portCategory.findPreference(getString(R.string.TUNNEL_INTERFACE)));
        }

        @Override
        protected void generalServerPort() {
            addPreferencesFromResource(R.xml.tunnel_gen_server_port, portCategory);
        }

        @Override
        protected void generalServerPortStreamr(boolean isStreamr) {
            if (isStreamr) {
                portCategory.removePreference(portCategory.findPreference(getString(R.string.TUNNEL_TARGET_HOST)));
                portCategory.removePreference(portCategory.findPreference(getString(R.string.TUNNEL_USE_SSL)));
            }
        }

        @Override
        protected void advanced() {
            Preference advanced = new Preference(getActivity());
            advanced.setKey(getString(R.string.TUNNEL_CAT_ADVANCED));
            advanced.setTitle(R.string.settings_label_advanced);
            advanced.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Fragment fragment = AdvancedTunnelPreferenceFragment.newInstance(mTunnelId);
                    getFragmentManager().beginTransaction()
                            .replace(R.id.fragment, fragment)
                            .addToBackStack(null)
                            .commit();
                    return true;
                }
            });
            ps.addPreference(advanced);
        }

        @Override
        protected void advancedStreamr(boolean isStreamr) {
        }

        @Override
        protected void advancedServerOrStreamrClient(boolean isServerOrStreamrClient) {
        }

        @Override
        protected void advancedServer() {
        }

        @Override
        protected void advancedServerHttp(boolean isHttp) {
        }

        @Override
        protected void advancedIdle() {
        }

        @Override
        protected void advancedIdleServerOrStreamrClient(boolean isServerOrStreamrClient) {
        }

        @Override
        protected void advancedClient() {
        }

        @Override
        protected void advancedClientHttp() {
        }

        @Override
        protected void advancedClientProxy() {
        }

        @Override
        protected void advancedOther() {
        }
    }
}
