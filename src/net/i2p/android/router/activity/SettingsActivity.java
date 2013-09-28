package net.i2p.android.router.activity;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import net.i2p.router.RouterContext;
import net.i2p.util.OrderedProperties;

public class SettingsActivity extends PreferenceActivity {
    // Actions for legacy settings
    private static final String ACTION_PREFS_NET = "net.i2p.android.router.PREFS_NET";
    private static final String ACTION_PREFS_ADVANCED = "net.i2p.android.router.PREFS_ADVANCED";

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        if (action != null && action.equals(ACTION_PREFS_NET)) {
            addPreferencesFromResource(R.xml.settings_net);
        } else if (action != null && action.equals(ACTION_PREFS_ADVANCED)) {
            addPreferencesFromResource(R.xml.settings_advanced);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // Load the legacy preferences headers
            addPreferencesFromResource(R.xml.settings_headers_legacy);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.settings_headers, target);
    }

    @Override
    protected void onPause() {
        // TODO: Rewrite this code to fix default setting and reduce duplication
        // Copy prefs
        Properties props = new OrderedProperties();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

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
                        Util.i("Resetting property '" + x + "' to default '" + string +"'");
                        props.setProperty(x, string);
                    }

                }
            }
        }
        // Merge in new config settings, write the file.
        InitActivities init = new InitActivities(this);
        init.mergeResourceToFile(R.raw.router_config, "router.config", props);
        // Apply new config if we are running.
        List<RouterContext> contexts = RouterContext.listContexts();
        if ( !((contexts == null) || (contexts.isEmpty())) ) {
            RouterContext _context = contexts.get(0);
            _context.router().saveConfig(props, null);
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String settings = getArguments().getString("settings");
            if ("net".equals(settings)) {
                addPreferencesFromResource(R.xml.settings_net);
            } else if ("advanced".equals(settings)) {
                addPreferencesFromResource(R.xml.settings_advanced);
            }
        }

        @Override
        public void onPause() {
            // TODO: Rewrite this code to fix default setting and reduce duplication
            // Copy prefs
            Properties props = new OrderedProperties();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

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
                } else if(! x.startsWith("DO_NOT_SAVE")) {
                    // Disabled?
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
                            Util.i("Resetting property '" + x + "' to default '" + string +"'");
                            props.setProperty(x, string);
                        }

                    }
                }
            }
            // Merge in new config settings, write the file.
            InitActivities init = new InitActivities(getActivity());
            init.mergeResourceToFile(R.raw.router_config, "router.config", props);
            // Apply new config if we are running.
            List<RouterContext> contexts = RouterContext.listContexts();
            if ( !((contexts == null) || (contexts.isEmpty())) ) {
                RouterContext _context = contexts.get(0);
                _context.router().saveConfig(props, null);
            }
            super.onPause();
        }
    }
}
