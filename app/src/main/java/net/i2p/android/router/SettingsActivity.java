package net.i2p.android.router;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.i2p.I2PAppContext;
import net.i2p.android.router.service.StatSummarizer;
import net.i2p.android.router.util.PortPreference;
import net.i2p.android.router.util.Util;
import net.i2p.router.RouterContext;
import net.i2p.stat.FrequencyStat;
import net.i2p.stat.Rate;
import net.i2p.stat.RateStat;
import net.i2p.stat.StatManager;
import net.i2p.util.LogManager;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;

public class SettingsActivity extends PreferenceActivity {
    // Actions for legacy settings
    private static final String ACTION_PREFS_NET = "net.i2p.android.router.PREFS_NET";
    public static final String ACTION_PREFS_GRAPHS = "net.i2p.android.router.PREFS_GRAPHS";
    private static final String ACTION_PREFS_LOGGING = "net.i2p.android.router.PREFS_LOGGING";
    private static final String ACTION_PREFS_ADVANCED = "net.i2p.android.router.PREFS_ADVANCED";

    private Toolbar mToolbar;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (action != null) {
            switch (action) {
                case ACTION_PREFS_NET:
                    addPreferencesFromResource(R.xml.settings_net);
                    break;
                case ACTION_PREFS_GRAPHS:
                    addPreferencesFromResource(R.xml.settings_graphs);
                    setupGraphSettings(this, getPreferenceScreen(), Util.getRouterContext());
                    break;
                case ACTION_PREFS_LOGGING:
                    addPreferencesFromResource(R.xml.settings_logging);
                    setupLoggingSettings(this, getPreferenceScreen(), Util.getRouterContext());
                    break;
                case ACTION_PREFS_ADVANCED:
                    addPreferencesFromResource(R.xml.settings_advanced);
                    setupAdvancedSettings(this, getPreferenceScreen());
                    break;
            }
        } else {
            // Load any properties that the router might have changed on us.
            setupPreferences(this, Util.getRouterContext());

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                // Load the legacy preferences headers
                addPreferencesFromResource(R.xml.settings_headers_legacy);
            }
        }

        mToolbar.setTitle(getTitle());
    }

    protected static void setupPreferences(Context context, RouterContext ctx) {
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
                // before SettingsActivity loads its Preferences.
                editor.commit();
            }
        }
    }

    protected static void setupGraphSettings(Context context, PreferenceScreen ps, RouterContext ctx) {
        if (ctx == null) {
            PreferenceCategory noRouter = new PreferenceCategory(context);
            noRouter.setTitle(R.string.router_not_running);
            ps.addPreference(noRouter);
        } else if (StatSummarizer.instance() == null) {
            PreferenceCategory noStats = new PreferenceCategory(context);
            noStats.setTitle(R.string.stats_not_ready);
            ps.addPreference(noStats);
        } else {
            StatManager mgr = ctx.statManager();
            Map<String, SortedSet<String>> all = mgr.getStatsByGroup();
            for (String group : all.keySet()) {
                SortedSet<String> stats = all.get(group);
                if (stats.size() == 0) continue;
                PreferenceCategory groupPrefs = new PreferenceCategory(context);
                groupPrefs.setKey("stat.groups." + group);
                groupPrefs.setTitle(group);
                ps.addPreference(groupPrefs);
                for (String stat : stats) {
                    String key;
                    String description;
                    boolean canBeGraphed = false;
                    boolean currentIsGraphed = false;
                    RateStat rs = mgr.getRate(stat);
                    if (rs != null) {
                        description = rs.getDescription();
                        long period = rs.getPeriods()[0]; // should be the minimum
                        key = stat + "." + period;
                        if (period <= 10*60*1000) {
                            Rate r = rs.getRate(period);
                            canBeGraphed = r != null;
                            if (canBeGraphed) {
                                currentIsGraphed = r.getSummaryListener() != null;
                            }
                        }
                    } else {
                        FrequencyStat fs = mgr.getFrequency(stat);
                        if (fs != null) {
                            key = stat;
                            description = fs.getDescription();
                            // FrequencyStats cannot be graphed, but can be logged.
                            // XXX: Should log settings be here as well, or in a
                            // separate settings menu?
                        } else {
                            Util.e("Stat does not exist?!  [" + stat + "]");
                            continue;
                        }
                    }
                    CheckBoxPreference statPref = new CheckBoxPreference(context);
                    statPref.setKey("stat.summaries." + key);
                    statPref.setTitle(stat);
                    statPref.setSummary(description);
                    statPref.setEnabled(canBeGraphed);
                    statPref.setChecked(currentIsGraphed);
                    groupPrefs.addPreference(statPref);
                }
            }
        }
    }

    protected static void setupLoggingSettings(Context context, PreferenceScreen ps, RouterContext ctx) {
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
            PreferenceCategory noRouter = new PreferenceCategory(context);
            noRouter.setTitle(R.string.router_not_running);
            ps.addPreference(noRouter);
            */
        }
    }

    protected static void setupAdvancedSettings(final Context context, PreferenceScreen ps) {
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        // The resource com.android.internal.R.bool.preferences_prefer_dual_pane
        // has different definitions based upon screen size. At present, it will
        // be true for -sw720dp devices, false otherwise. For your curiosity, in
        // Nexus 7 it is false.
        loadHeadersFromResource(R.xml.settings_headers, target);
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.activity_settings,
                (ViewGroup) getWindow().getDecorView().getRootView(), false);

        mToolbar = (Toolbar) contentView.findViewById(R.id.main_toolbar);
        mToolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        ViewGroup contentWrapper = (ViewGroup) contentView.findViewById(R.id.content_wrapper);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);

        getWindow().setContentView(contentView);
    }

    @Override
    protected void onPause() {
        List<Properties> lProps = Util.getPropertiesFromPreferences(this);
        Properties props = lProps.get(0);
        Properties propsToRemove = lProps.get(1);
        Properties logSettings = lProps.get(2);

        Set toRemove = propsToRemove.keySet();

        boolean restartRequired = Util.checkAndCorrectRouterConfig(this, props, toRemove);

        // Apply new config if we are running.
        RouterContext rCtx = Util.getRouterContext();
        if (rCtx != null) {
            rCtx.router().saveConfig(props, toRemove);

            // Merge in new log settings
            saveLoggingChanges(rCtx, logSettings);
        } else {
            // Merge in new config settings, write the file.
            Util.mergeResourceToFile(this, Util.getFileDir(this), "router.config", R.raw.router_config, props, toRemove);

            // Merge in new log settings
            saveLoggingChanges(I2PAppContext.getGlobalContext(), logSettings);
        }

        // Store the settings in Android
        super.onPause();

        if (restartRequired)
            Toast.makeText(this, R.string.settings_router_restart_required, Toast.LENGTH_LONG).show();
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String settings = getArguments().getString("settings");
            switch (settings) {
                case "net":
                    addPreferencesFromResource(R.xml.settings_net);
                    break;
                case "graphs":
                    addPreferencesFromResource(R.xml.settings_graphs);
                    setupGraphSettings(getActivity(), getPreferenceScreen(), Util.getRouterContext());
                    break;
                case "logging":
                    addPreferencesFromResource(R.xml.settings_logging);
                    setupLoggingSettings(getActivity(), getPreferenceScreen(), Util.getRouterContext());
                    break;
                case "advanced":
                    addPreferencesFromResource(R.xml.settings_advanced);
                    setupAdvancedSettings(getActivity(), getPreferenceScreen());
                    break;
            }
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SettingsFragment.class.getName().equals(fragmentName);
    }
}
