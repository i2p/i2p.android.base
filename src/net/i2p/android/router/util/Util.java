package net.i2p.android.router.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

public abstract class Util {
    private static final boolean _isEmulator = Build.MODEL.equals("sdk");

    public static String getOurVersion(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        String us = ctx.getPackageName();
        try {
            PackageInfo pi = pm.getPackageInfo(us, 0);
            System.err.println("VersionCode" + ": " + pi.versionCode);
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

}
