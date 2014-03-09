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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import net.i2p.router.RouterContext;
import net.i2p.stat.FrequencyStat;
import net.i2p.stat.Rate;
import net.i2p.stat.RateStat;
import net.i2p.stat.StatManager;
import net.i2p.util.LogManager;
import net.i2p.util.OrderedProperties;

public class SettingsActivity extends PreferenceActivity {
    // Actions for legacy settings
    private static final String ACTION_PREFS_NET = "net.i2p.android.router.PREFS_NET";
    private static final String ACTION_PREFS_GRAPHS = "net.i2p.android.router.PREFS_GRAPHS";
    private static final String ACTION_PREFS_LOGGING = "net.i2p.android.router.PREFS_LOGGING";
    private static final String ACTION_PREFS_ADVANCED = "net.i2p.android.router.PREFS_ADVANCED";

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (action != null) {
            if (ACTION_PREFS_NET.equals(action)) {
                addPreferencesFromResource(R.xml.settings_net);
            } else if (ACTION_PREFS_GRAPHS.equals(action)){
                addPreferencesFromResource(R.xml.settings_graphs);
                RouterContext ctx = getRouterContext();
                if (ctx != null)
                    setupGraphSettings(this, getPreferenceScreen(), ctx);
            } else if (ACTION_PREFS_LOGGING.equals(action)) {
                addPreferencesFromResource(R.xml.settings_logging);
                RouterContext ctx = getRouterContext();
                if (ctx != null)
                    setupLoggingSettings(this, getPreferenceScreen(), ctx);
            } else if (ACTION_PREFS_ADVANCED.equals(action)) {
                addPreferencesFromResource(R.xml.settings_advanced);
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // Load the legacy preferences headers
            addPreferencesFromResource(R.xml.settings_headers_legacy);
        }
    }

    protected static RouterContext getRouterContext() {
        List<RouterContext> contexts = RouterContext.listContexts();
        if ( !((contexts == null) || (contexts.isEmpty())) ) {
            return contexts.get(0);
        }
        return null;
    }

    protected static void setupGraphSettings(Context context, PreferenceScreen ps, RouterContext ctx) {
        if (ctx != null) {
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
        } else {
            PreferenceCategory noRouter = new PreferenceCategory(context);
            noRouter.setTitle(R.string.router_not_running);
            ps.addPreference(noRouter);
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
        } else {
            PreferenceCategory noRouter = new PreferenceCategory(context);
            noRouter.setTitle(R.string.router_not_running);
            ps.addPreference(noRouter);
        }
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
    protected void onPause() {
        // TODO: Rewrite this code to fix default setting
        // Copy prefs
        Properties props = new OrderedProperties();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // List to store stats for graphing
        List<String> statSummaries = new ArrayList<String>();

        // List to store Log settings
        Map<String, String> logSettings = new HashMap<String, String>();

        Map<String, ?> all = preferences.getAll();
        Iterator<String> iterator = all.keySet().iterator();
        // get values from the Map and make them strings.
        // This loop avoids needing to convert each one, or even know it's type, or if it exists yet.
        while (iterator.hasNext()) {
            String x = iterator.next();
            // special exception, we must invert the bool for this property only.
            if(x.equals("router.hiddenMode")) {
                String string = all.get(x).toString();
                String what="true";
                if(string.equals(what)) {
                    what="false";
                }
                props.setProperty(x, what);
            } else if ( x.startsWith("stat.summaries.")) {
                String stat = x.substring("stat.summaries.".length());
                String checked = all.get(x).toString();
                if (checked.equals("true")) {
                    statSummaries.add(stat);
                }
            } else if ( x.startsWith("logger.")) {
                logSettings.put(x, all.get(x).toString());
            } else if ( x.startsWith("i2pandroid.")) {
                // Don't save UI-related I2P Android settings in router.config
                continue;
            } else if(! x.startsWith("DO_NOT_SAVE")) {
                // Disabled?
                @SuppressWarnings("deprecation")
                Preference findPreference = findPreference(x);
                if (findPreference == null)
                    continue;
                if ( findPreference.isEnabled() ) {
                    String string = all.get(x).toString();
                    props.setProperty(x, string);
                } else {
                    String summary[] = findPreference.getSummary().toString().split("default=");
                    String defaultval = summary[summary.length - 1].trim();
                    if (defaultval.endsWith(")")) {
                        // strip the ")" off the tail end, this is the default value!
                        String string = defaultval.substring(0, defaultval.length() - 1);
                        Util.d("Resetting property '" + x + "' to default '" + string +"'");
                        props.setProperty(x, string);
                    }

                }
            }
        }
        if (statSummaries.isEmpty()) {
            props.setProperty("stat.summaries", "");
        } else {
            Iterator<String> iter = statSummaries.iterator();
            StringBuilder buf = new StringBuilder(iter.next());
            while (iter.hasNext()) {
                buf.append(",").append(iter.next());
            }
            props.setProperty("stat.summaries", buf.toString());
        }
        // Merge in new config settings, write the file.
        InitActivities init = new InitActivities(this);
        init.mergeResourceToFile(R.raw.router_config, "router.config", props);
        // Apply new config if we are running.
        List<RouterContext> contexts = RouterContext.listContexts();
        if ( !((contexts == null) || (contexts.isEmpty())) ) {
            RouterContext _context = contexts.get(0);
            _context.router().saveConfig(props, null);

            // Merge in new log settings
            saveLoggingChanges(_context, logSettings);
        }

        // Store the settings in Android
        super.onPause();
    }

    private void saveLoggingChanges(RouterContext ctx, Map<String, String> logSettings) {
        boolean shouldSave = false;

        for (String key : logSettings.keySet()) {
            if ("logger.defaultLevel".equals(key)) {
                String defaultLevel = logSettings.get(key);
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
            if ("net".equals(settings)) {
                addPreferencesFromResource(R.xml.settings_net);
            } else if ("graphs".equals(settings)) {
                addPreferencesFromResource(R.xml.settings_graphs);
                RouterContext ctx = getRouterContext();
                if (ctx != null)
                    setupGraphSettings(getActivity(), getPreferenceScreen(), ctx);
            } else if ("logging".equals(settings)) {
                addPreferencesFromResource(R.xml.settings_logging);
                RouterContext ctx = getRouterContext();
                if (ctx != null)
                    setupLoggingSettings(getActivity(), getPreferenceScreen(), ctx);
            } else if ("advanced".equals(settings)) {
                addPreferencesFromResource(R.xml.settings_advanced);
            }
        }
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SettingsFragment.class.getName().equals(fragmentName);
    }
}
