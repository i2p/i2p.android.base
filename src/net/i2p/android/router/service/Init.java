package net.i2p.android.router.service;

import android.content.Context;
import java.io.File;
import net.i2p.android.router.util.Util;
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

    void initialize() {

        deleteOldFiles();

        // Set up the locations so Router and WorkingDir can find them
        // We do this again here, in the event settings were changed.
        System.setProperty("i2p.dir.base", myDir);
        System.setProperty("i2p.dir.config", myDir);
        System.setProperty("wrapper.logfile", myDir + "/wrapper.log");
    }

    private void deleteOldFiles() {
        File tmp = new File(myDir, "tmp");
        File[] files = tmp.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                Util.d("Deleting old file/dir " + f);
                FileUtil.rmdir(f, false);
            }
        }
    }
}
