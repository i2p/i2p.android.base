package net.i2p.android.router.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import net.i2p.I2PAppContext;
import net.i2p.util.Log;

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
            ctx.logManager().getLog(Util.class).log(Log.ERROR, m, t);
        else if (t != null)
            android.util.Log.e(ANDROID_TAG, m + ' ' + t + ' ' + android.util.Log.getStackTraceString(t));
        else
            android.util.Log.e(ANDROID_TAG, m);
    }

    public static void w(String m) {
        w(m, null);
    }

    public static void w(String m, Throwable t) {
        I2PAppContext ctx = I2PAppContext.getCurrentContext();
        if (ctx != null)
            ctx.logManager().getLog(Util.class).log(Log.WARN, m, t);
        else if (t != null)
            android.util.Log.w(ANDROID_TAG, m + ' ' + t + ' ' + android.util.Log.getStackTraceString(t));
        else
            android.util.Log.w(ANDROID_TAG, m);
    }

    public static void i(String m) {
        i(m, null);
    }

    public static void i(String m, Throwable t) {
        I2PAppContext ctx = I2PAppContext.getCurrentContext();
        if (ctx != null)
            ctx.logManager().getLog(Util.class).log(Log.INFO, m, t);
        else if (t != null)
            android.util.Log.i(ANDROID_TAG, m + ' ' + t + ' ' + android.util.Log.getStackTraceString(t));
        else
            android.util.Log.i(ANDROID_TAG, m);
    }
    public static void d(String m) {
        d(m, null);
    }

    public static void d(String m, Throwable t) {
        I2PAppContext ctx = I2PAppContext.getCurrentContext();
        if (ctx != null)
            ctx.logManager().getLog(Util.class).log(Log.DEBUG, m, t);
        else if (t != null)
            android.util.Log.d(ANDROID_TAG, m + ' ' + t + ' ' + android.util.Log.getStackTraceString(t));
        else
            android.util.Log.d(ANDROID_TAG, m);
    }
}
