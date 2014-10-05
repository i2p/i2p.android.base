package net.i2p.android.router.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import net.i2p.I2PAppContext;
import net.i2p.data.DataHelper;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.transport.TransportManager;
import net.i2p.util.OrderedProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class Util {
    public static String getOurVersion(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        String us = ctx.getPackageName();
        try {
            PackageInfo pi = pm.getPackageInfo(us, 0);
            //System.err.println("VersionCode" + ": " + pi.versionCode);
            // http://doandroids.com/blogs/2010/6/10/android-classloader-dynamic-loading-of/
            //_apkPath = pm.getApplicationInfo(us, 0).sourceDir;
            //System.err.println("APK Path" + ": " + _apkPath);
            if (pi.versionName != null)
                return pi.versionName;
        } catch (Exception e) {}
        return "??";
    }

    /**
     * Get the active RouterContext.
     *
     * @return the active RouterContext, or null
     */
    public static RouterContext getRouterContext() {
        List<RouterContext> contexts = RouterContext.listContexts();
        if ( !((contexts == null) || (contexts.isEmpty())) ) {
            return contexts.get(0);
        }
        return null;
    }

    private static final String ANDROID_TAG = "I2P";

    public static void e(String m) {
        e(m, null);
    }

    /**
     *  Log to the context logger if available (which goes to the console buffer
     *  and to logcat), else just to logcat.
     */
    public static void e(String m, Throwable t) {
        I2PAppContext ctx = I2PAppContext.getCurrentContext();
        if (ctx != null)
            ctx.logManager().getLog(Util.class).error(m, t);
        else if (android.util.Log.isLoggable(ANDROID_TAG, android.util.Log.ERROR)) {
            if (t != null)
                android.util.Log.e(ANDROID_TAG, m + ' ' + t + ' ' + android.util.Log.getStackTraceString(t));
            else
                android.util.Log.e(ANDROID_TAG, m);
        }
    }

    public static void w(String m) {
        w(m, null);
    }

    public static void w(String m, Throwable t) {
        I2PAppContext ctx = I2PAppContext.getCurrentContext();
        if (ctx != null)
            ctx.logManager().getLog(Util.class).warn(m, t);
        else if (android.util.Log.isLoggable(ANDROID_TAG, android.util.Log.WARN)) {
            if (t != null)
                android.util.Log.w(ANDROID_TAG, m + ' ' + t + ' ' + android.util.Log.getStackTraceString(t));
            else
                android.util.Log.w(ANDROID_TAG, m);
        }
    }

    public static void i(String m) {
        i(m, null);
    }

    public static void i(String m, Throwable t) {
        I2PAppContext ctx = I2PAppContext.getCurrentContext();
        if (ctx != null)
            ctx.logManager().getLog(Util.class).info(m, t);
        else if (android.util.Log.isLoggable(ANDROID_TAG, android.util.Log.INFO)) {
            if (t != null)
                android.util.Log.i(ANDROID_TAG, m + ' ' + t + ' ' + android.util.Log.getStackTraceString(t));
            else
                android.util.Log.i(ANDROID_TAG, m);
        }
    }
    public static void d(String m) {
        d(m, null);
    }

    public static void d(String m, Throwable t) {
        I2PAppContext ctx = I2PAppContext.getCurrentContext();
        if (ctx != null)
            ctx.logManager().getLog(Util.class).debug(m, t);
        else if (android.util.Log.isLoggable(ANDROID_TAG, android.util.Log.DEBUG)) {
            if (t != null)
                android.util.Log.d(ANDROID_TAG, m + ' ' + t + ' ' + android.util.Log.getStackTraceString(t));
            else
                android.util.Log.d(ANDROID_TAG, m);
        }
    }

    public static List<Properties> getPropertiesFromPreferences(Context context) {
        List<Properties> pList = new ArrayList<Properties>();

        // Copy prefs
        Properties routerProps = new OrderedProperties();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // List to store stats for graphing
        List<String> statSummaries = new ArrayList<String>();

        // List to store Log settings
        Properties logSettings = new OrderedProperties();

        Map<String, ?> all = preferences.getAll();
        Iterator<String> iterator = all.keySet().iterator();
        // get values from the Map and make them strings.
        // This loop avoids needing to convert each one, or even know it's type, or if it exists yet.
        while (iterator.hasNext()) {
            String x = iterator.next();
            if ( x.startsWith("i2pandroid.")) // Skip over UI-related I2P Android settings
                continue;
            else if ( x.startsWith("stat.summaries.")) {
                String stat = x.substring("stat.summaries.".length());
                String checked = all.get(x).toString();
                if (checked.equals("true")) {
                    statSummaries.add(stat);
                }
            } else if ( x.startsWith("logger.")) {
                logSettings.put(x, all.get(x).toString());
            } else if (
                    x.equals("router.hiddenMode") ||
                    x.equals("i2cp.disableInterface")) {
                // special exception, we must invert the bool for these properties only.
                String string = all.get(x).toString();
                String inverted = Boolean.toString(!Boolean.parseBoolean(string));
                routerProps.setProperty(x, inverted);
            } else {
                String string = all.get(x).toString();
                routerProps.setProperty(x, string);
            }
        }
        if (statSummaries.isEmpty()) {
            routerProps.setProperty("stat.summaries", "");
        } else {
            Iterator<String> iter = statSummaries.iterator();
            StringBuilder buf = new StringBuilder(iter.next());
            while (iter.hasNext()) {
                buf.append(",").append(iter.next());
            }
            routerProps.setProperty("stat.summaries", buf.toString());
        }

        pList.add(routerProps);
        pList.add(logSettings);

        return pList;
    }

    // propName -> defaultValue
    private static HashMap<String, Boolean> booleanOptionsRequiringRestart = new HashMap<String, Boolean>();
    private static HashMap<String, String> stringOptionsRequiringRestart = new HashMap<String, String>();
    static {
        HashMap<String, Boolean> boolToAdd = new HashMap<String, Boolean>();
        HashMap<String, String> strToAdd = new HashMap<String, String>();

        boolToAdd.put(TransportManager.PROP_ENABLE_UPNP, true);
        boolToAdd.put(TransportManager.PROP_ENABLE_NTCP, true);
        boolToAdd.put(TransportManager.PROP_ENABLE_UDP, true);
        boolToAdd.put(Router.PROP_HIDDEN, false);

        booleanOptionsRequiringRestart.putAll(boolToAdd);
        stringOptionsRequiringRestart.putAll(strToAdd);
    }
    /**
     * This function performs two tasks:
     * <ul><li>
     * The Properties object is modified to ensure that all options are valid
     * for the current state of the Android device (e.g. what type of network
     * the device is connected to).
     * </li><li>
     * The Properties object is checked to determine whether any options have
     * changed that will require a router restart.
     * </li></ul>
     *
     * @param props a Properties object containing the router.config
     * @return true if the router needs to be restarted.
     */
    public static boolean checkAndCorrectRouterConfig(Context context, Properties props) {
        // Disable UPnP on mobile networks, ignoring user's configuration
        if (Connectivity.isConnectedMobile(context)) {
            props.setProperty(TransportManager.PROP_ENABLE_UPNP, Boolean.toString(false));
        }

        // Now check if a restart is required
        boolean restartRequired = false;
        RouterContext rCtx = getRouterContext();
        if (rCtx != null) {
            for (Map.Entry<String, Boolean> option : booleanOptionsRequiringRestart.entrySet()) {
                String propName = option.getKey();
                boolean defaultValue = option.getValue();
                restartRequired |= (
                        Boolean.parseBoolean(props.getProperty(propName, Boolean.toString(defaultValue))) !=
                                (defaultValue ? rCtx.getBooleanPropertyDefaultTrue(propName) : rCtx.getBooleanProperty(propName))
                );
            }
            if (!restartRequired) { // Cut out now if we already know the answer
                for (Map.Entry<String, String> option : stringOptionsRequiringRestart.entrySet()) {
                    String propName = option.getKey();
                    String defaultValue = option.getValue();
                    restartRequired |= props.getProperty(propName, defaultValue).equals(
                            rCtx.getProperty(propName, defaultValue));
                }
            }
        }
        return restartRequired;
    }

    public static String getFileDir(Context context) {
        // This needs to be changed so that we can have an alternative place
        return context.getFilesDir().getAbsolutePath();
    }

    /**
     *  Write properties to a file. If the file does not exist, it is created.
     *  If the properties already exist in the file, they are updated.
     *
     *  @param dir the file directory
     *  @param file relative to dir
     *  @param props properties to set
     */
    public static void writePropertiesToFile(Context ctx, String dir, String file, Properties props) {
        mergeResourceToFile(ctx, dir, file, 0, props);
    }

    /**
     *  Load defaults from resource, then add props from settings, and write back.
     *  If resID is 0, defaults are not written over the existing file content.
     *
     *  @param dir the file directory
     *  @param file relative to dir
     *  @param resID the ID of the default resource, or 0
     *  @param userProps local properties or null
     */
    public static void mergeResourceToFile(Context ctx, String dir, String file, int resID, Properties userProps) {
        InputStream fin = null;
        InputStream in = null;

        try {
            Properties props = new OrderedProperties();
            try {
                fin = new FileInputStream(new File(dir, file));
                DataHelper.loadProps(props, fin);
                if (resID > 0)
                    Util.d("Merging resource into file " + file);
                else
                    Util.d("Merging properties into file " + file);
            } catch (IOException ioe) {
                if (resID > 0)
                    Util.d("Creating file " + file + " from resource");
                else
                    Util.d("Creating file " + file + " from properties");
            }

            // write in default settings
            if (resID > 0)
                in = ctx.getResources().openRawResource(resID);
            if (in != null)
                DataHelper.loadProps(props,  in);

            // override with user settings
            if (userProps != null)
                props.putAll(userProps);

            File path = new File(dir, file);
            DataHelper.storeProps(props, path);
            Util.d("Saved " + props.size() +" properties in " + file);
        } catch (IOException ioe) {
        } catch (Resources.NotFoundException nfe) {
        } finally {
            if (in != null) try { in.close(); } catch (IOException ioe) {}
            if (fin != null) try { fin.close(); } catch (IOException ioe) {}
        }
    }
}
