package net.i2p.android.router.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import net.i2p.I2PAppContext;
import net.i2p.util.OrderedProperties;

public abstract class Util {
    private static final boolean _isEmulator = Build.MODEL.equals("sdk");

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

    public static boolean isConnected(Context ctx) {
        // emulator always returns null NetworkInfo
        if (_isEmulator)
            return true;
        NetworkInfo current = getNetworkInfo(ctx);
        return current != null && current.isConnected();
    }

    public static NetworkInfo getNetworkInfo(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo current = cm.getActiveNetworkInfo();
        return current;
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
                String what="true";
                if(string.equals(what)) {
                    what="false";
                }
                routerProps.setProperty(x, what);
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
}
