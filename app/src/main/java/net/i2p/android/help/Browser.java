package net.i2p.android.help;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Browser implements Comparable<Browser> {
    public final String packageName;
    public final CharSequence label;
    public final Drawable icon;
    public final boolean isKnown;
    public final boolean isSupported;
    public final boolean isRecommended;

    private boolean isInstalled;
    /**
     * A browser that we don't know about.
     *
     * @param pm      the PackageManager used to find the browser
     * @param browser the browser
     */
    public Browser(PackageManager pm, ResolveInfo browser) {
        this(
                browser.activityInfo.packageName,
                browser.loadLabel(pm),
                browser.loadIcon(pm),
                true, false, false, false
        );
    }

    /**
     * A browser that we know about.
     *
     * @param pm        the PackageManager used to find the browser
     * @param browser   the browser
     * @param supported can this browser be used with I2P?
     */
    public Browser(PackageManager pm, ResolveInfo browser, boolean supported, boolean recommended) {
        this(
                browser.activityInfo.packageName,
                browser.loadLabel(pm),
                browser.loadIcon(pm),
                true, true, supported, recommended
        );
    }

    public Browser(String pn, CharSequence l, Drawable ic, boolean i, boolean k, boolean s, boolean r) {
        packageName = pn;
        label = l;
        icon = ic;
        isInstalled = i;
        isKnown = k;
        isSupported = s;
        isRecommended = r;
    }

    @Override
    public int compareTo(@NonNull Browser browser) {
        // Sort order: supported -> unknown -> unsupported
        int a = getOrder(this);
        int b = getOrder(browser);

        if (a < b)
            return -1;
        else if (a > b)
            return 1;

        return label.toString().compareTo(browser.label.toString());
    }

    private static int getOrder(Browser browser) {
        if (browser.isKnown) {
            if (browser.isRecommended)
                return 0;
            else if (browser.isSupported)
                return 1;
            else
                return 3;
        } else
            return 2;
    }

    public boolean isInstalled(Context context){
        if (isInstalled) {
            return true;
        }
        // Find all installed browsers that listen for ".i2p"
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://stats.i2p"));

        final PackageManager pm = context.getPackageManager();
        List<ResolveInfo> installedBrowsers = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo browser : installedBrowsers) {
            if (browser.activityInfo.packageName.equals(packageName)) {
                isInstalled = true;
                break;
            }
        }
        return isInstalled;
    }
}
