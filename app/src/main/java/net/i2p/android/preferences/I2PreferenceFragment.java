package net.i2p.android.preferences;

import android.widget.Toast;

import net.i2p.I2PAppContext;
import net.i2p.android.preferences.util.CustomPreferenceFragment;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import net.i2p.router.RouterContext;

import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * A PreferenceFragment that handles saving router settings.
 */
public abstract class I2PreferenceFragment extends CustomPreferenceFragment {
    @Override
    public void onPause() {
        List<Properties> lProps = Util.getPropertiesFromPreferences(getActivity());
        Properties props = lProps.get(0);
        Properties propsToRemove = lProps.get(1);
        Properties logSettings = lProps.get(2);

        Set toRemove = propsToRemove.keySet();

        boolean restartRequired = Util.checkAndCorrectRouterConfig(getActivity(), props, toRemove);

        // Apply new config if we are running.
        RouterContext rCtx = Util.getRouterContext();
        if (rCtx != null) {
            rCtx.router().saveConfig(props, toRemove);

            // Merge in new log settings
            saveLoggingChanges(rCtx, logSettings);
        } else {
            // Merge in new config settings, write the file.
            Util.mergeResourceToFile(getActivity(),
                    Util.getFileDir(getActivity()),
                    "router.config", R.raw.router_config, props, toRemove);

            // Merge in new log settings
            saveLoggingChanges(I2PAppContext.getGlobalContext(), logSettings);
        }

        // Store the settings in Android
        super.onPause();

        if (restartRequired)
            Toast.makeText(getActivity(), R.string.settings_router_restart_required, Toast.LENGTH_LONG).show();
    }

    private void saveLoggingChanges(I2PAppContext ctx, Properties logSettings) {
        boolean shouldSave = false;

        for (Object key : logSettings.keySet()) {
            if ("logger.defaultLevel".equals(key)) {
                String defaultLevel = (String) logSettings.get(key);
                String oldDefault = ctx.logManager().getDefaultLimit();
                if (!defaultLevel.equals(oldDefault)) {
                    shouldSave = true;
                    ctx.logManager().setDefaultLimit(defaultLevel);
                }
            }
        }

        if (shouldSave) {
            ctx.logManager().saveConfig();
        }
    }
}
