package net.i2p.android.router.service;

import android.content.Context;

import net.i2p.android.router.util.Util;
import net.i2p.util.FileUtil;

import java.io.File;

class Init {

    private final String myDir;

    public Init(Context c) {
        myDir = c.getFilesDir().getAbsolutePath();
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
            for (File f : files) {
                Util.d("Deleting old file/dir " + f);
                FileUtil.rmdir(f, false);
            }
        }
    }
}
