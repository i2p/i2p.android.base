package net.i2p.android.preferences;

import android.os.Bundle;
import android.preference.PreferenceScreen;

import net.i2p.android.router.R;
import net.i2p.android.router.SettingsActivity;
import net.i2p.android.router.util.Util;
import net.i2p.router.RouterContext;
import net.i2p.util.LogManager;

public class LoggingPreferenceFragment extends I2PreferenceFragment {
    @Override
    public void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        addPreferencesFromResource(R.xml.settings_logging);
        setupLoggingSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((SettingsActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings_label_logging);
    }

    private void setupLoggingSettings() {
        PreferenceScreen ps = getPreferenceScreen();
        RouterContext ctx = Util.getRouterContext();
        if (ctx != null) {
            LogManager mgr = ctx.logManager();
            // Log level overrides
            /*
            StringBuilder buf = new StringBuilder(32*1024);
            Properties limits = mgr.getLimits();
            TreeSet<String> sortedLogs = new TreeSet<String>();
            for (Iterator iter = limits.keySet().iterator(); iter.hasNext(); ) {
                String prefix = (String)iter.next();
                sortedLogs.add(prefix);
            }
            for (Iterator iter = sortedLogs.iterator(); iter.hasNext(); ) {
                String prefix = (String)iter.next();
                String level = limits.getProperty(prefix);
                buf.append(prefix).append('=').append(level).append('\n');
            }
            */
            /* Don't show, there are no settings that require the router
        } else {
            PreferenceCategory noRouter = new PreferenceCategory(getActivity());
            noRouter.setTitle(R.string.router_not_running);
            ps.addPreference(noRouter);
            */
        }
    }
}
