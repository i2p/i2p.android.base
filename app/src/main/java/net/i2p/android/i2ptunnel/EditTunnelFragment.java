package net.i2p.android.i2ptunnel;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.v4.preference.PreferenceFragment;

import net.i2p.I2PAppContext;
import net.i2p.android.i2ptunnel.util.TunnelLogic;
import net.i2p.android.i2ptunnel.util.TunnelUtil;
import net.i2p.android.router.R;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.i2ptunnel.ui.TunnelConfig;

public class EditTunnelFragment extends PreferenceFragment {
    private static final String ARG_TUNNEL_ID = "tunnelId";

    private TunnelControllerGroup mGroup;
    private int mTunnelId;

    public static EditTunnelFragment newInstance(int tunnelId) {
        EditTunnelFragment f = new EditTunnelFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TUNNEL_ID, tunnelId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);

        String error;
        try {
            mGroup = TunnelControllerGroup.getInstance();
            error = mGroup == null ? getResources().getString(R.string.i2ptunnel_not_initialized) : null;
        } catch (IllegalArgumentException iae) {
            mGroup = null;
            error = iae.toString();
        }

        if (mGroup == null) {
            // TODO Show error
        } else if (getArguments().containsKey(ARG_TUNNEL_ID)) {
            mTunnelId = getArguments().getInt(ARG_TUNNEL_ID, 0);
            TunnelUtil.writeTunnelToPreferences(getActivity(), mGroup, mTunnelId);
            // https://stackoverflow.com/questions/17880437/which-settings-file-does-preferencefragment-read-write
            getPreferenceManager().setSharedPreferencesName(TunnelUtil.getPreferencesFilename(mTunnelId));
            loadPreferences();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // TODO do this here, or only when explicitly exiting the Activity?
        // We kinda need to do it here, because this is the only method that is
        // guaranteed to be called
        if (mGroup != null) {
            TunnelConfig cfg = TunnelUtil.createConfigFromPreferences(getActivity(), mGroup, mTunnelId);
            TunnelUtil.saveTunnel(I2PAppContext.getGlobalContext(), mGroup, mTunnelId, cfg);
        }
    }

    private void loadPreferences() {
        String type = TunnelUtil.getController(mGroup, mTunnelId).getType();
        new TunnelPreferences(type).runLogic();
    }

    class TunnelPreferences extends TunnelLogic {
        PreferenceScreen ps;
        PreferenceCategory generalCategory;
        PreferenceCategory portCategory;
        PreferenceScreen advanced;
        PreferenceCategory tunParamCategory;

        public TunnelPreferences(String type) {
            super(type);
            ps = getPreferenceScreen();
        }

        @Override
        protected void general() {
            addPreferencesFromResource(R.xml.tunnel_gen);
            generalCategory = (PreferenceCategory) ps.findPreference(
                    getString(R.string.TUNNEL_CAT_GENERAL));
            portCategory = (PreferenceCategory) ps.findPreference(
                    getString(R.string.TUNNEL_CAT_PORT));
        }

        @Override
        protected void generalClient() {
            addPreferencesFromResource(R.xml.tunnel_gen_client, generalCategory);
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
            if (isStreamr)
                portCategory.removePreference(portCategory.findPreference(getString(R.string.TUNNEL_INTERFACE)));
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
            addPreferencesFromResource(R.xml.tunnel_adv);
            advanced = (PreferenceScreen) ps.findPreference(
                    getString(R.string.TUNNEL_CAT_ADVANCED));
            tunParamCategory = (PreferenceCategory) ps.findPreference(
                    getString(R.string.TUNNEL_CAT_TUNNEL_PARAMS));
        }

        @Override
        protected void advancedStreamr(boolean isStreamr) {
            if (isStreamr)
                tunParamCategory.removePreference(tunParamCategory.findPreference(getString(R.string.TUNNEL_OPT_PROFILE)));
        }

        @Override
        protected void advancedServerOrStreamrClient(boolean isServerOrStreamrClient) {
            if (isServerOrStreamrClient)
                tunParamCategory.removePreference(tunParamCategory.findPreference(getString(R.string.TUNNEL_OPT_DELAY_CONNECT)));
        }

        @Override
        protected void advancedServer() {
            addPreferencesFromResource(R.xml.tunnel_adv_server, advanced);
        }

        @Override
        protected void advancedServerHttp(boolean isHttp) {
            if (isHttp)
                addPreferencesFromResource(R.xml.tunnel_adv_server_http, advanced);
            else {
                PreferenceCategory accessCtlCategory = (PreferenceCategory) ps.findPreference(
                        getString(R.string.TUNNEL_CAT_ACCESS_CONTROL));
                accessCtlCategory.removePreference(accessCtlCategory.findPreference(getString(R.string.TUNNEL_OPT_REJECT_INPROXY)));
            }
        }

        @Override
        protected void advancedIdle() {
            addPreferencesFromResource(R.xml.tunnel_adv_idle, advanced);
        }

        @Override
        protected void advancedIdleServerOrStreamrClient(boolean isServerOrStreamrClient) {
            if (isServerOrStreamrClient)
                advanced.removePreference(advanced.findPreference(getString(R.string.TUNNEL_OPT_DELAY_OPEN)));
        }

        @Override
        protected void advancedClient() {
            PreferenceCategory idleCategory = (PreferenceCategory) ps.findPreference(
                    getString(R.string.TUNNEL_CAT_IDLE)
            );
            addPreferencesFromResource(R.xml.tunnel_adv_idle_client, idleCategory);
        }

        @Override
        protected void advancedClientHttp() {
            addPreferencesFromResource(R.xml.tunnel_adv_client_http, advanced);
        }

        @Override
        protected void advancedClientProxy() {
            addPreferencesFromResource(R.xml.tunnel_adv_client_proxy, advanced);
        }

        @Override
        protected void advancedOther() {
            addPreferencesFromResource(R.xml.tunnel_adv_other, advanced);
        }
    }

    /**
     * http://stackoverflow.com/a/20806812
     *
     * @param id the Preferences XML to load
     * @param newParent the parent PreferenceGroup to add the new Preferences to.
     */
    private void addPreferencesFromResource (int id, PreferenceGroup newParent) {
        PreferenceScreen screen = getPreferenceScreen ();
        int last = screen.getPreferenceCount ();
        addPreferencesFromResource (id);
        while (screen.getPreferenceCount () > last) {
            Preference p = screen.getPreference (last);
            screen.removePreference (p); // decreases the preference count
            newParent.addPreference (p);
        }
    }
}
