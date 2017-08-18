package net.i2p.android.router.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import net.i2p.I2PAppContext;
import net.i2p.android.preferences.GraphsPreferenceFragment;
import net.i2p.android.router.I2PConstants;
import net.i2p.android.router.R;
import net.i2p.android.router.service.State;
import net.i2p.data.DataHelper;
import net.i2p.data.router.RouterAddress;
import net.i2p.data.router.RouterInfo;
import net.i2p.router.CommSystemFacade;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.transport.TransportManager;
import net.i2p.router.transport.TransportUtil;
import net.i2p.router.transport.udp.UDPTransport;
import net.i2p.util.OrderedProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class Util implements I2PConstants {
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
        } catch (Exception e) {
        }
        return "??";
    }

    /**
     * Get the active RouterContext.
     *
     * @return the active RouterContext, or null
     */
    public static RouterContext getRouterContext() {
        List<RouterContext> contexts = RouterContext.listContexts();
        if (!((contexts == null) || (contexts.isEmpty()))) {
            return contexts.get(0);
        }
        return null;
    }

    private static final String ANDROID_TAG = "I2P";

    public static void e(String m) {
        e(m, null);
    }

    /**
     * Log to the context logger if available (which goes to the console buffer
     * and to logcat), else just to logcat.
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

    /**
     * copied from various private components
     */
    final static String PROP_I2NP_NTCP_PORT = "i2np.ntcp.port";
    final static String PROP_I2NP_NTCP_AUTO_PORT = "i2np.ntcp.autoport";

    public static List<Properties> getPropertiesFromPreferences(Context context) {
        List<Properties> pList = new ArrayList<>();

        // Copy prefs
        Properties routerProps = new OrderedProperties();

        // List to store stats for graphing
        List<String> statSummaries = new ArrayList<>();

        // Properties to remove
        Properties toRemove = new OrderedProperties();

        // List to store Log settings
        Properties logSettings = new OrderedProperties();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> all = preferences.getAll();
        // get values from the Map and make them strings.
        // This loop avoids needing to convert each one, or even know it's type, or if it exists yet.
        for (String x : all.keySet()) {
            if (x.startsWith("stat.summaries.")) {
                String stat = x.substring("stat.summaries.".length());
                String checked = all.get(x).toString();
                if (checked.equals("true")) {
                    statSummaries.add(stat);
                }
            } else if (x.startsWith("logger.")) {
                logSettings.put(x, all.get(x).toString());
            } else if (
                    x.equals("router.hiddenMode") ||
                            x.equals("i2cp.disableInterface")) {
                // special exception, we must invert the bool for these properties only.
                String string = all.get(x).toString();
                String inverted = Boolean.toString(!Boolean.parseBoolean(string));
                routerProps.setProperty(x, inverted);
            } else if (x.equals(context.getString(R.string.PREF_LANGUAGE))) {
                String language[] = TextUtils.split(all.get(x).toString(), "_");

                if (language[0].equals(context.getString(R.string.DEFAULT_LANGUAGE))) {
                    toRemove.setProperty("routerconsole.lang", "");
                    toRemove.setProperty("routerconsole.country", "");
                } else {
                    routerProps.setProperty("routerconsole.lang", language[0].toLowerCase());
                    if (language.length == 2)
                        routerProps.setProperty("routerconsole.country", language[1].toUpperCase());
                    else
                        toRemove.setProperty("routerconsole.country", "");
                }
            } else if (!x.startsWith(ANDROID_PREF_PREFIX)) { // Skip over UI-related I2P Android settings
                String string = all.get(x).toString();
                routerProps.setProperty(x, string);
            }
        }
        if (statSummaries.isEmpty()) {
            // If the graph preferences have not yet been seen, they should be the default
            if (preferences.getBoolean(GraphsPreferenceFragment.GRAPH_PREFERENCES_SEEN, false))
                routerProps.setProperty("stat.summaries", "");
            else
                toRemove.setProperty("stat.summaries", "");
        } else {
            Iterator<String> iter = statSummaries.iterator();
            StringBuilder buf = new StringBuilder(iter.next());
            while (iter.hasNext()) {
                buf.append(",").append(iter.next());
            }
            routerProps.setProperty("stat.summaries", buf.toString());
        }

        // See net.i2p.router.web.ConfigNetHandler.saveChanges()
        int udpPort = Integer.parseInt(routerProps.getProperty(UDPTransport.PROP_INTERNAL_PORT, "-1"));
        if (udpPort <= 0)
            routerProps.remove(UDPTransport.PROP_INTERNAL_PORT);
        int ntcpPort = Integer.parseInt(routerProps.getProperty(PROP_I2NP_NTCP_PORT, "-1"));
        boolean ntcpAutoPort = Boolean.parseBoolean(
                routerProps.getProperty(PROP_I2NP_NTCP_AUTO_PORT, "true"));
        if (ntcpPort <= 0 || ntcpAutoPort) {
            routerProps.remove(PROP_I2NP_NTCP_PORT);
            toRemove.setProperty(PROP_I2NP_NTCP_PORT, "");
        }

        pList.add(routerProps);
        pList.add(toRemove);
        pList.add(logSettings);

        return pList;
    }

    // propName -> defaultValue
    private static HashMap<String, Boolean> booleanOptionsRequiringRestart = new HashMap<>();
    private static HashMap<String, String> stringOptionsRequiringRestart = new HashMap<>();

    static {
        HashMap<String, Boolean> boolToAdd = new HashMap<>();
        HashMap<String, String> strToAdd = new HashMap<>();

        boolToAdd.put(TransportManager.PROP_ENABLE_UPNP, true);
        boolToAdd.put(TransportManager.PROP_ENABLE_NTCP, true);
        boolToAdd.put(TransportManager.PROP_ENABLE_UDP, true);
        boolToAdd.put(PROP_I2NP_NTCP_AUTO_PORT, true);
        boolToAdd.put(Router.PROP_HIDDEN, false);

        strToAdd.put(UDPTransport.PROP_INTERNAL_PORT, "-1");
        strToAdd.put(PROP_I2NP_NTCP_PORT, "-1");

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
     * @param props    a Properties object containing the router.config
     * @param toRemove a Collection of properties that will be removed
     * @return true if the router needs to be restarted.
     */
    public static boolean checkAndCorrectRouterConfig(Context context, Properties props, Collection<String> toRemove) {
        // Disable UPnP on mobile networks, ignoring user's configuration
        // TODO disabled until changes elsewhere are finished
        //if (Connectivity.isConnectedMobile(context)) {
        //    props.setProperty(TransportManager.PROP_ENABLE_UPNP, Boolean.toString(false));
        //}

        // Now check if a restart is required
        boolean restartRequired = false;
        RouterContext rCtx = getRouterContext();
        if (rCtx != null) {
            for (Map.Entry<String, Boolean> option : booleanOptionsRequiringRestart.entrySet()) {
                String propName = option.getKey();
                boolean defaultValue = option.getValue();
                boolean currentValue = defaultValue ? rCtx.getBooleanPropertyDefaultTrue(propName) : rCtx.getBooleanProperty(propName);
                boolean newValue = Boolean.parseBoolean(props.getProperty(propName, Boolean.toString(defaultValue)));
                restartRequired |= (currentValue != newValue);
            }
            if (!restartRequired) { // Cut out now if we already know the answer
                for (Map.Entry<String, String> option : stringOptionsRequiringRestart.entrySet()) {
                    String propName = option.getKey();
                    String defaultValue = option.getValue();
                    String currentValue = rCtx.getProperty(propName, defaultValue);
                    String newValue = props.getProperty(propName, defaultValue);
                    restartRequired |= !currentValue.equals(newValue);
                }
            }
        }
        return restartRequired;
    }

    public static String getFileDir(Context context) {
        // This needs to be changed so that we can have an alternative place
        File f = context.getFilesDir();
        if (f == null) {
            // https://code.google.com/p/android/issues/detail?id=8886
            // Seems to be a race condition; try again.
            // Supposedly only in pre-4.4 devices, but this was observed on a
            // Samsung Galaxy Grand Prime (grandprimeve3g), 1024MB RAM, Android 5.1
            f = context.getFilesDir();
        }
        return f.getAbsolutePath();
    }

    /**
     * Write properties to a file. If the file does not exist, it is created.
     * If the properties already exist in the file, they are updated.
     *
     * @param dir   the file directory
     * @param file  relative to dir
     * @param props properties to set
     */
    public static void writePropertiesToFile(Context ctx, String dir, String file, Properties props) {
        mergeResourceToFile(ctx, dir, file, 0, props, null);
    }

    /**
     * Load defaults from resource, then add props from settings, and write back.
     * If resID is 0, defaults are not written over the existing file content.
     *
     * @param dir       the file directory
     * @param file      relative to dir
     * @param resID     the ID of the default resource, or 0
     * @param userProps local properties or null
     * @param toRemove  properties to remove, or null
     */
    public static void mergeResourceToFile(Context ctx, String dir, String file, int resID,
                                           Properties userProps, Collection<String> toRemove) {
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
                DataHelper.loadProps(props, in);

            // override with user settings
            if (userProps != null)
                props.putAll(userProps);
            if (toRemove != null) {
                for (String key : toRemove) {
                    props.remove(key);
                }
            }

            File path = new File(dir, file);
            DataHelper.storeProps(props, path);
            Util.d("Saved " + props.size() + " properties in " + file);
        } catch (IOException ioe) {
        } catch (Resources.NotFoundException nfe) {
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException ioe) {
            }
            if (fin != null) try {
                fin.close();
            } catch (IOException ioe) {
            }
        }
    }

    public static boolean isStopping(State state) {
        return state == State.STOPPING ||
                state == State.MANUAL_STOPPING ||
                state == State.MANUAL_QUITTING;
    }

    public static boolean isStopped(State state) {
        return state == State.STOPPED ||
                state == State.MANUAL_STOPPED ||
                state == State.MANUAL_QUITTED ||
                state == State.WAITING;
    }

    public static class NetStatus {
        public enum Level {
            ERROR,
            WARN,
            INFO,
        }

        public final Level level;
        public final String status;

        public NetStatus(Level level, String status) {
            this.level = level;
            this.status = status;
        }
    }

    public static NetStatus getNetStatus(Context ctx, RouterContext rCtx) {
        if (rCtx.commSystem().isDummy())
            return new NetStatus(NetStatus.Level.INFO, ctx.getString(R.string.vm_comm_system));
        if (rCtx.router().getUptime() > 60 * 1000 && (!rCtx.router().gracefulShutdownInProgress()) &&
                !rCtx.clientManager().isAlive())  // not a router problem but the user should know
            return new NetStatus(NetStatus.Level.ERROR, ctx.getString(R.string.net_status_error_i2cp));
        // Warn based on actual skew from peers, not update status, so if we successfully offset
        // the clock, we don't complain.
        //if (!rCtx.clock().getUpdatedSuccessfully())
        long skew = rCtx.commSystem().getFramedAveragePeerClockSkew(33);
        // Display the actual skew, not the offset
        if (Math.abs(skew) > 30 * 1000)
            return new NetStatus(NetStatus.Level.ERROR,
                    ctx.getString(R.string.net_status_error_skew,
                            DataHelper.formatDuration2(Math.abs(skew))
                                    .replace("&minus;", "-")
                                    .replace("&nbsp;", " ")));
        if (rCtx.router().isHidden())
            return new NetStatus(NetStatus.Level.INFO, ctx.getString(R.string.hidden));
        RouterInfo routerInfo = rCtx.router().getRouterInfo();
        if (routerInfo == null)
            return new NetStatus(NetStatus.Level.INFO, ctx.getString(R.string.testing));

        CommSystemFacade.Status status = rCtx.commSystem().getStatus();
        switch (status) {
            case OK:
            case IPV4_OK_IPV6_UNKNOWN:
            case IPV4_OK_IPV6_FIREWALLED:
            case IPV4_UNKNOWN_IPV6_OK:
            case IPV4_DISABLED_IPV6_OK:
            case IPV4_SNAT_IPV6_OK:
                RouterAddress ra = routerInfo.getTargetAddress("NTCP");
                if (ra == null)
                    return new NetStatus(NetStatus.Level.INFO, toStatusString(ctx, status));
                byte[] ip = ra.getIP();
                if (ip == null)
                    return new NetStatus(NetStatus.Level.ERROR, ctx.getString(R.string.net_status_error_unresolved_tcp));
                // TODO set IPv6 arg based on configuration?
                if (TransportUtil.isPubliclyRoutable(ip, true))
                    return new NetStatus(NetStatus.Level.INFO, toStatusString(ctx, status));
                return new NetStatus(NetStatus.Level.ERROR, ctx.getString(R.string.net_status_error_private_tcp));

            case IPV4_SNAT_IPV6_UNKNOWN:
            case DIFFERENT:
                return new NetStatus(NetStatus.Level.ERROR, ctx.getString(R.string.symmetric_nat));

            case REJECT_UNSOLICITED:
            case IPV4_DISABLED_IPV6_FIREWALLED:
                if (routerInfo.getTargetAddress("NTCP") != null)
                    return new NetStatus(NetStatus.Level.WARN, ctx.getString(R.string.net_status_warn_firewalled_inbound_tcp));
                // fall through...
            case IPV4_FIREWALLED_IPV6_OK:
            case IPV4_FIREWALLED_IPV6_UNKNOWN:
                if (rCtx.netDb().floodfillEnabled())
                    return new NetStatus(NetStatus.Level.WARN, ctx.getString(R.string.net_status_warn_firewalled_floodfill));
                //if (rCtx.router().getRouterInfo().getCapabilities().indexOf('O') >= 0)
                //    return _("WARN-Firewalled and Fast");
                return new NetStatus(NetStatus.Level.INFO, toStatusString(ctx, status));

            case DISCONNECTED:
                return new NetStatus(NetStatus.Level.INFO, ctx.getString(R.string.net_status_info_disconnected));

            case HOSED:
                return new NetStatus(NetStatus.Level.ERROR, ctx.getString(R.string.net_status_error_udp_port));

            case UNKNOWN:
            case IPV4_UNKNOWN_IPV6_FIREWALLED:
            case IPV4_DISABLED_IPV6_UNKNOWN:
            default:
                ra = routerInfo.getTargetAddress("SSU");
                if (ra == null && rCtx.router().getUptime() > 5 * 60 * 1000) {
                    if (rCtx.commSystem().countActivePeers() <= 0)
                        return new NetStatus(NetStatus.Level.ERROR, ctx.getString(R.string.net_status_error_no_active_peers));
                    else if (rCtx.getProperty(ctx.getString(R.string.PROP_I2NP_NTCP_HOSTNAME)) == null ||
                            rCtx.getProperty(ctx.getString(R.string.PROP_I2NP_NTCP_PORT)) == null)
                        return new NetStatus(NetStatus.Level.ERROR, ctx.getString(R.string.net_status_error_udp_disabled_tcp_not_set));
                    else
                        return new NetStatus(NetStatus.Level.WARN, ctx.getString(R.string.net_status_warn_firewalled_udp_disabled));
                }
                return new NetStatus(NetStatus.Level.INFO, toStatusString(ctx, status));
        }
    }

    private static String toStatusString(Context ctx, CommSystemFacade.Status status) {
        String ipv4Status = "";
        String ipv6Status = "";
        switch (status) {
            case OK:
                return ctx.getString(android.R.string.ok);
            case IPV4_OK_IPV6_UNKNOWN:
                ipv4Status = ctx.getString(android.R.string.ok);
                ipv6Status = ctx.getString(R.string.testing);
                break;
            case IPV4_OK_IPV6_FIREWALLED:
                ipv4Status = ctx.getString(android.R.string.ok);
                ipv6Status = ctx.getString(R.string.firewalled);
                break;
            case IPV4_UNKNOWN_IPV6_OK:
                ipv4Status = ctx.getString(R.string.testing);
                ipv6Status = ctx.getString(android.R.string.ok);
                break;
            case IPV4_FIREWALLED_IPV6_OK:
                ipv4Status = ctx.getString(R.string.firewalled);
                ipv6Status = ctx.getString(android.R.string.ok);
                break;
            case IPV4_DISABLED_IPV6_OK:
                ipv4Status = ctx.getString(R.string.disabled);
                ipv6Status = ctx.getString(android.R.string.ok);
                break;
            case IPV4_SNAT_IPV6_OK:
                ipv4Status = ctx.getString(R.string.symmetric_nat);
                ipv6Status = ctx.getString(android.R.string.ok);
                break;
            case DIFFERENT:
                return ctx.getString(R.string.symmetric_nat);
            case IPV4_SNAT_IPV6_UNKNOWN:
                ipv4Status = ctx.getString(R.string.symmetric_nat);
                ipv6Status = ctx.getString(R.string.testing);
                break;
            case IPV4_FIREWALLED_IPV6_UNKNOWN:
                ipv4Status = ctx.getString(R.string.firewalled);
                ipv6Status = ctx.getString(R.string.testing);
                break;
            case REJECT_UNSOLICITED:
                return ctx.getString(R.string.firewalled);
            case IPV4_UNKNOWN_IPV6_FIREWALLED:
                ipv4Status = ctx.getString(R.string.testing);
                ipv6Status = ctx.getString(R.string.firewalled);
                break;
            case IPV4_DISABLED_IPV6_UNKNOWN:
                ipv4Status = ctx.getString(R.string.disabled);
                ipv6Status = ctx.getString(R.string.testing);
                break;
            case IPV4_DISABLED_IPV6_FIREWALLED:
                ipv4Status = ctx.getString(R.string.disabled);
                ipv6Status = ctx.getString(R.string.firewalled);
                break;
            case UNKNOWN:
                return ctx.getString(R.string.testing);
            default:
                return status.toStatusString();
        }

        return ctx.getString(R.string.net_status_ipv4_ipv6, ipv4Status, ipv6Status);
    }

    public static String formatSize(double size) {
        return formatSize(size, 0);
    }

    public static String formatSpeed(double size) {
        return formatSize(size, 1);
    }

    public static String formatSize(double size, int baseScale) {
        int scale;
        for (int i = 0; i < baseScale; i++) {
            size /= 1024.0D;
        }
        for (scale = baseScale; size >= 1024.0D; size /= 1024.0D) {
            ++scale;
        }

        // control total width
        DecimalFormat fmt;
        if (size >= 1000) {
            fmt = new DecimalFormat("#0");
        } else if (size >= 100) {
            fmt = new DecimalFormat("#0.0");
        } else {
            fmt = new DecimalFormat("#0.00");
        }

        String str = fmt.format(size);
        switch (scale) {
            case 1:
                return str + "K";
            case 2:
                return str + "M";
            case 3:
                return str + "G";
            case 4:
                return str + "T";
            case 5:
                return str + "P";
            case 6:
                return str + "E";
            case 7:
                return str + "Z";
            case 8:
                return str + "Y";
            default:
                return str + "";
        }
    }
}
