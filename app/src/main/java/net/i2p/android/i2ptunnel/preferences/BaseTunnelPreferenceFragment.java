package net.i2p.android.i2ptunnel.preferences;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.widget.Toast;

import net.i2p.android.i2ptunnel.util.SaveTunnelTask;
import net.i2p.android.i2ptunnel.util.TunnelUtil;
import net.i2p.android.preferences.util.CustomPreferenceFragment;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.i2ptunnel.ui.TunnelConfig;

import java.util.concurrent.ExecutionException;

public abstract class BaseTunnelPreferenceFragment extends CustomPreferenceFragment {
    protected static final String ARG_TUNNEL_ID = "tunnelId";

    protected TunnelControllerGroup mGroup;
    protected int mTunnelId;

    @Override
    public void onCreatePreferences(Bundle paramBundle, String s) {
        String error;
        try {
            mGroup = TunnelControllerGroup.getInstance();
            error = mGroup == null ? getResources().getString(R.string.i2ptunnel_not_initialized) : null;
        } catch (IllegalArgumentException iae) {
            mGroup = null;
            error = iae.toString();
        }

        if (mGroup == null) {
            Toast.makeText(getActivity().getApplicationContext(),
                    error, Toast.LENGTH_LONG).show();
            getActivity().finish();
        } else if (getArguments().containsKey(ARG_TUNNEL_ID)) {
            mTunnelId = getArguments().getInt(ARG_TUNNEL_ID, 0);
            try {
                TunnelUtil.writeTunnelToPreferences(getActivity(), mGroup, mTunnelId);
            } catch (IllegalArgumentException e) {
                // Tunnel doesn't exist, or the tunnel config file could not be read
                Util.e("Could not load tunnel details", e);
                Toast.makeText(getActivity().getApplicationContext(),
                        R.string.i2ptunnel_no_tunnel_details, Toast.LENGTH_LONG).show();
                getActivity().finish();
            }
            // https://stackoverflow.com/questions/17880437/which-settings-file-does-preferencefragment-read-write
            getPreferenceManager().setSharedPreferencesName(TunnelUtil.getPreferencesFilename(mTunnelId));
            loadPreferences();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // Pre-Honeycomb: onPause() is the last method guaranteed to be called.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            saveTunnel();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Honeycomb and above: onStop() is the last method guaranteed to be called.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            saveTunnel();
    }

    private void saveTunnel() {
        if (mGroup != null) {
            TunnelConfig cfg = TunnelUtil.createConfigFromPreferences(getActivity(), mGroup, mTunnelId);
            SaveTunnelTask task = new SaveTunnelTask(mGroup, mTunnelId, cfg);
            try {
                task.execute().get();
            } catch (InterruptedException e) {
                Util.e("Interrupted while saving tunnel config", e);
            } catch (ExecutionException e) {
                Util.e("Error while saving tunnel config", e);
            }
        }
    }

    protected abstract void loadPreferences();

    /**
     * http://stackoverflow.com/a/20806812
     *
     * @param id        the Preferences XML to load
     * @param newParent the parent PreferenceGroup to add the new Preferences to.
     */
    protected void addPreferencesFromResource(int id, PreferenceGroup newParent) {
        PreferenceScreen screen = getPreferenceScreen();
        int last = screen.getPreferenceCount();
        addPreferencesFromResource(id);
        while (screen.getPreferenceCount() > last) {
            Preference p = screen.getPreference(last);
            screen.removePreference(p); // decreases the preference count
            newParent.addPreference(p);
        }
    }
}
