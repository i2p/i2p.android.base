package net.i2p.android.router.activity;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataHelper;
import net.i2p.util.FileUtil;
import net.i2p.util.OrderedProperties;

//  Wouldn't this be better as a private class in MainActivity?

class InitActivities {

    private final Context ctx;
    private final String myDir;
    private final String _ourVersion;

    private static final String CONFIG_FILE = "android.config";
    private static final String PROP_NEW_INSTALL = "i2p.newInstall";
    private static final String PROP_NEW_VERSION = "i2p.newVersion";
    private static final String PROP_INSTALLED_VERSION = "i2p.version";

    public InitActivities(Context c) {
        ctx = c;
        // This needs to be changed so that we can have an alternative place
        myDir = c.getFilesDir().getAbsolutePath();
        _ourVersion = Util.getOurVersion(c);
    }

    void debugStuff() {
        Util.i("java.io.tmpdir" + ": " + System.getProperty("java.io.tmpdir"));
        Util.i("java.vendor" + ": " + System.getProperty("java.vendor"));
        Util.i("java.version" + ": " + System.getProperty("java.version"));
        Util.i("os.arch" + ": " + System.getProperty("os.arch"));
        Util.i("os.name" + ": " + System.getProperty("os.name"));
        Util.i("os.version" + ": " + System.getProperty("os.version"));
        Util.i("user.dir" + ": " + System.getProperty("user.dir"));
        Util.i("user.home" + ": " + System.getProperty("user.home"));
        Util.i("user.name" + ": " + System.getProperty("user.name"));
        Util.i("getFilesDir()" + ": " + myDir);
        Util.i("max mem" + ": " + DataHelper.formatSize(Runtime.getRuntime().maxMemory()));
        Util.i("Package" + ": " + ctx.getPackageName());
        Util.i("Version" + ": " + _ourVersion);
        Util.i("MODEL" + ": " + Build.MODEL);
        Util.i("DISPLAY" + ": " + Build.DISPLAY);
        Util.i("VERSION" + ": " + Build.VERSION.RELEASE);
        Util.i("SDK" + ": " + Build.VERSION.SDK);
    }

    void initialize() {

        if (checkNewVersion()) {
            Properties props = new Properties();
            props.setProperty("i2p.dir.temp", myDir + "/tmp");
            props.setProperty("i2p.dir.pid", myDir + "/tmp");
            // Time disabled in default router.config
            // But lots of time problems on Android, not all carriers support NITZ
            // and there was no NTP before 3.0. Tablets should be fine?
            // Phones in airplane mode with wifi enabled still a problem.
            // Deactivated phones in airplane mode definatly won't have correct time.
            if (Build.VERSION.SDK_INT < 11)  // Honeycomb 3.0
                props.setProperty("time.disabled", "false");
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

            File docsDir = new File(myDir, "docs");
            docsDir.mkdir();
            copyResourceToFile(R.raw.ahelper_conflict_header_ht, "docs/ahelper-conflict-header.ht");
            copyResourceToFile(R.raw.ahelper_new_header_ht, "docs/ahelper-new-header.ht");
            copyResourceToFile(R.raw.auth_header_ht, "docs/auth-header.ht");
            copyResourceToFile(R.raw.denied_header_ht, "docs/denied-header.ht");
            copyResourceToFile(R.raw.dnf_header_ht, "docs/dnf-header.ht");
            copyResourceToFile(R.raw.dnfb_header_ht, "docs/dnfb-header.ht");
            copyResourceToFile(R.raw.dnfh_header_ht, "docs/dnfh-header.ht");
            copyResourceToFile(R.raw.dnfp_header_ht, "docs/dnfp-header.ht");
            copyResourceToFile(R.raw.localhost_header_ht, "docs/localhost-header.ht");
            copyResourceToFile(R.raw.noproxy_header_ht, "docs/noproxy-header.ht");
            copyResourceToFile(R.raw.protocol_header_ht, "docs/protocol-header.ht");

            File cssDir = new File(docsDir, "themes/console/light");
            cssDir.mkdirs();
            //copyResourceToFile(R.raw.console_css, "docs/themes/console/light/console.css");
            //copyResourceToFile(R.raw.android_css, "docs/themes/console/light/android.css");

            File imgDir = new File(docsDir, "themes/console/images");
            imgDir.mkdir();
            copyResourceToFile(R.drawable.i2plogo, "docs/themes/console/images/i2plogo.png");
            copyResourceToFile(R.drawable.itoopie_sm, "docs/themes/console/images/itoopie_sm.png");
            //copyResourceToFile(R.drawable.outbound, "docs/themes/console/images/outbound.png");
            //copyResourceToFile(R.drawable.inbound, "docs/themes/console/images/inbound.png");

            File img2Dir = new File(cssDir, "images");
            img2Dir.mkdir();
            //copyResourceToFile(R.drawable.header, "docs/themes/console/light/images/header.png");

            File certDir = new File(myDir, "certificates");
            certDir.mkdir();
            File certificates = new File(myDir, "certificates");
            File[] allcertificates = certificates.listFiles();
            if ( allcertificates != null) {
                for (int i = 0; i < allcertificates.length; i++) {
                    File f = allcertificates[i];
                    Util.i("Deleting old certificate file/dir " + f);
                    FileUtil.rmdir(f, false);
                }
            }
            unzipResourceToDir(R.raw.certificates_zip, "certificates");
        }

        // Set up the locations so settings can find them
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

        Util.i("Creating file " + f + " from resource");
        byte buf[] = new byte[4096];
        try {
            // Context methods
            in = ctx.getResources().openRawResource(resID);
            out = new FileOutputStream(new File(myDir, f));

            int read;
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
     *  @param f relative to base dir
     */
    private void unzipResourceToDir(int resID, String f) {
        InputStream in = null;
        FileOutputStream out = null;
        ZipInputStream zis = null;

        Util.i("Creating files in '" + myDir + "/" + f + "/' from resource");
        try {
            // Context methods
            in = ctx.getResources().openRawResource(resID);
            zis = new ZipInputStream((in));
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                out = null;
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                    }
                    String filename = ze.getName();
                    Util.i("Creating file " + myDir + "/" + f +"/" + filename + " from resource");
                    byte[] bytes = baos.toByteArray();
                    out = new FileOutputStream(new File(myDir + "/" + f +"/" + filename));
                    out.write(bytes);
                } catch (IOException ioe) {
                } finally {
                    if (out != null) { try { out.close(); } catch (IOException ioe) {} out = null; }
                }
            }
        } catch (IOException ioe) {
        } catch (Resources.NotFoundException nfe) {
        } finally {
            if (in != null) try { in.close(); } catch (IOException ioe) {}
            if (out != null) try { out.close(); } catch (IOException ioe) {}
            if (zis != null) try { zis.close(); } catch (IOException ioe) {}
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
    public void mergeResourceToFile(int resID, String f, Properties overrides) {
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
                Util.i("Merging resource into file " + f);
            } catch (IOException ioe) {
                Util.i("Creating file " + f + " from resource");
            }

            // override user settings
            DataHelper.loadProps(props,  in);

            if (overrides != null)
                props.putAll(overrides);
            File path = new File(myDir, f);
            DataHelper.storeProps(props, path);
            Util.i("Saved " + props.size() +" properties in " + f);
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
            Util.i("Looks like a new install");
        } finally {
            if (fin != null) try { fin.close(); } catch (IOException ioe) {}
        }

        String oldVersion = props.getProperty(PROP_INSTALLED_VERSION);
        boolean newInstall = oldVersion == null;
        boolean newVersion = !_ourVersion.equals(oldVersion);

        if (newVersion) {
            Util.i("New version " + _ourVersion);
            props.setProperty(PROP_INSTALLED_VERSION, _ourVersion);
            try {
                DataHelper.storeProps(props, ctx.getFileStreamPath(CONFIG_FILE));
            } catch (IOException ioe) {
                Util.i("Failed to write " + CONFIG_FILE);
            }
        }
        return newVersion;
    }
}
