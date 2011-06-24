package net.i2p.android.router.service;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataHelper;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.RouterLaunch;
import net.i2p.util.FileUtil;
import net.i2p.util.NativeBigInteger;
import net.i2p.util.OrderedProperties;

class Init {

    private final Context ctx;
    private final String myDir;
    private final String _ourVersion;

    private static final String CONFIG_FILE = "android.config";
    private static final String PROP_NEW_INSTALL = "i2p.newInstall";
    private static final String PROP_NEW_VERSION = "i2p.newVersion";
    private static final String PROP_INSTALLED_VERSION = "i2p.version";

    public Init(Context c) {
        ctx = c;
        myDir = c.getFilesDir().getAbsolutePath();
        _ourVersion = Util.getOurVersion(c);
    }

    void debugStuff() {
        System.err.println("java.io.tmpdir" + ": " + System.getProperty("java.io.tmpdir"));
        System.err.println("java.vendor" + ": " + System.getProperty("java.vendor"));
        System.err.println("java.version" + ": " + System.getProperty("java.version"));
        System.err.println("os.arch" + ": " + System.getProperty("os.arch"));
        System.err.println("os.name" + ": " + System.getProperty("os.name"));
        System.err.println("os.version" + ": " + System.getProperty("os.version"));
        System.err.println("user.dir" + ": " + System.getProperty("user.dir"));
        System.err.println("user.home" + ": " + System.getProperty("user.home"));
        System.err.println("user.name" + ": " + System.getProperty("user.name"));
        System.err.println("getFilesDir()" + ": " + myDir);
        System.err.println("max mem" + ": " + DataHelper.formatSize(Runtime.getRuntime().maxMemory()));
        System.err.println("Package" + ": " + ctx.getPackageName());
        System.err.println("Version" + ": " + _ourVersion);
        System.err.println("MODEL" + ": " + Build.MODEL);
        System.err.println("DISPLAY" + ": " + Build.DISPLAY);
        System.err.println("VERSION" + ": " + Build.VERSION.RELEASE);
        System.err.println("SDK" + ": " + Build.VERSION.SDK);
    }

    void initialize() {
        if (checkNewVersion()) {
            Properties props = new Properties();
            props.setProperty("i2p.dir.temp", myDir + "/tmp");
            props.setProperty("i2p.dir.pid", myDir + "/tmp");
            mergeResourceToFile(R.raw.router_config, "router.config", props);
            mergeResourceToFile(R.raw.logger_config, "logger.config", null);
            mergeResourceToFile(R.raw.i2ptunnel_config, "i2ptunnel.config", null);
            // FIXME this is a memory hog to merge this way
            mergeResourceToFile(R.raw.hosts_txt, "hosts.txt", null);
            mergeResourceToFile(R.raw.more_hosts_txt, "hosts.txt", null);
            copyResourceToFile(R.raw.blocklist_txt, "blocklist.txt");
            File abDir = new File(myDir, "addressbook");
            abDir.mkdir();
            copyResourceToFile(R.raw.subscriptions_txt, "addressbook/subscriptions.txt");
            mergeResourceToFile(R.raw.addressbook_config_txt, "addressbook/config.txt", null);
        }

        deleteOldFiles();

        // Set up the locations so Router and WorkingDir can find them
        System.setProperty("i2p.dir.base", myDir);
        System.setProperty("i2p.dir.config", myDir);
        System.setProperty("wrapper.logfile", myDir + "/wrapper.log");
    }

    /**
     *  @param f relative to base dir
     */
    private void copyResourceToFile(int resID, String f) {
        InputStream in = null;
        FileOutputStream out = null;

        System.err.println("Creating file " + f + " from resource");
        byte buf[] = new byte[4096];
        try {
            // Context methods
            in = ctx.getResources().openRawResource(resID);
            out = new FileOutputStream(new File(myDir, f));
            
            int read = 0;
            while ( (read = in.read(buf)) != -1)
                out.write(buf, 0, read);
            
        } catch (IOException ioe) {
        } catch (Resources.NotFoundException nfe) {
        } finally {
            if (in != null) try { in.close(); } catch (IOException ioe) {}
            if (out != null) try { out.close(); } catch (IOException ioe) {}
        }
    }
    
    /**
     *  Load defaults from resource,
     *  then add props from file,
     *  and write back
     *  For now, do it backwards so we can override with new apks.
     *  When we have user configurable stuff, switch it back.
     *
     *  @param f relative to base dir
     *  @param props local overrides or null
     */
    private void mergeResourceToFile(int resID, String f, Properties overrides) {
        InputStream in = null;
        InputStream fin = null;

        byte buf[] = new byte[4096];
        try {
            in = ctx.getResources().openRawResource(resID);
            Properties props = new OrderedProperties();
            // keep user settings
            //DataHelper.loadProps(props,  in);
            
            try {
                fin = new FileInputStream(new File(myDir, f));
                DataHelper.loadProps(props,  fin);
                System.err.println("Merging resource into file " + f);
            } catch (IOException ioe) {
                System.err.println("Creating file " + f + " from resource");
            }

            // override user settings
            DataHelper.loadProps(props,  in);

            if (overrides != null)
                props.putAll(overrides);
            File path = new File(myDir, f);
            DataHelper.storeProps(props, path);
            System.err.println("Saved " + props.size() +" properties in " + f);
        } catch (IOException ioe) {
        } catch (Resources.NotFoundException nfe) {
        } finally {
            if (in != null) try { in.close(); } catch (IOException ioe) {}
            if (fin != null) try { fin.close(); } catch (IOException ioe) {}
        }
    }
    
    /**
     *  Check for new version.
     *  FIXME we could just use shared prefs for this instead of storing in a file
     *  @return true if new version
     */
    private boolean checkNewVersion() {
        Properties props = new Properties();
        
        InputStream fin = null;
        try {
            fin = ctx.openFileInput(CONFIG_FILE);
            DataHelper.loadProps(props,  fin);
        } catch (IOException ioe) {
            System.err.println("Looks like a new install");
        } finally {
            if (fin != null) try { fin.close(); } catch (IOException ioe) {}
        }

        String oldVersion = props.getProperty(PROP_INSTALLED_VERSION);
        boolean newInstall = oldVersion == null;
        boolean newVersion = !_ourVersion.equals(oldVersion);

        if (newVersion) {
            System.err.println("New version " + _ourVersion);
            props.setProperty(PROP_INSTALLED_VERSION, _ourVersion);
            try {
                DataHelper.storeProps(props, ctx.getFileStreamPath(CONFIG_FILE));
            } catch (IOException ioe) {
                System.err.println("Failed to write " + CONFIG_FILE);
            }
        }
        return newVersion;
    }

    private void deleteOldFiles() {
        (new File(myDir, "wrapper.log")).delete();
        File tmp = new File(myDir, "tmp");
        File[] files = tmp.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                System.err.println("Deleting old file/dir " + f);
                FileUtil.rmdir(f, false);
            }
        }
    }
}
