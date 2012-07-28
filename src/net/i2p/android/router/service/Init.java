package net.i2p.android.router.service;

import android.content.Context;
import android.os.Build;
import java.io.File;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataHelper;
import net.i2p.util.FileUtil;

class Init {

    private final Context ctx;
    private final String myDir;
    private final String _ourVersion;

    public Init(Context c) {
        ctx = c;
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

        deleteOldFiles();

        // Set up the locations so Router and WorkingDir can find them
        System.setProperty("i2p.dir.base", myDir);
        System.setProperty("i2p.dir.config", myDir);
        System.setProperty("wrapper.logfile", myDir + "/wrapper.log");
    }

    private void deleteOldFiles() {
        (new File(myDir, "wrapper.log")).delete();
        File tmp = new File(myDir, "tmp");
        File[] files = tmp.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                Util.i("Deleting old file/dir " + f);
                FileUtil.rmdir(f, false);
            }
        }
    }
}
