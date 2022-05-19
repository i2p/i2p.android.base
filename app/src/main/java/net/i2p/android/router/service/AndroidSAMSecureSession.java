package net.i2p.android.router.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import net.i2p.sam.*;
import net.i2p.sam.SAMException;

import java.util.Properties;

/**
 * Implements SAMSecureSessionInterface on Android platforms using a Toast
 * as the interactive channel.
 * 
 * @since 1.8.0
 */
public class AndroidSAMSecureSession implements SAMSecureSessionInterface {
    private final Context mCtx;

    public AndroidSAMSecureSession(Context ctx) {
        mCtx = ctx;
    }

    private class SAMSecureRunnable implements Runnable {
        private int result = -1;

        @Override
        public void run() {
            AlertDialog.Builder builder = new AlertDialog.Builder(mCtx.getApplicationContext());

            builder.setTitle(mCtx.getResources().getString(R.string.settings_confirm_sam));
            builder.setMessage(mCtx.getResources().getString(R.string.settings_confirm_sam));

            builder.setPositiveButton(mCtx.getResources().getString(R.string.settings_confirm_allow_sam), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    // set result=true and close the dialog
                    result = 1;
                    dialog.dismiss();
                }
            });

            builder.setNegativeButton(mCtx.getResources().getString(R.string.settings_confirm_deny_sam), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // set result=false
                    result = 0;
                    dialog.dismiss();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
        }

        public boolean isResult() {
            for (int i=0;i<60;i++) {
                Util.i("Waiting on user to approve SAM connection");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return false;
                }
            }
            if (result == 0)
                return false;
            if (result == 1)
                return true;
            return false;
        }
    }
    public Context getApplicationContext() {
        return mCtx.getApplicationContext();
    }

    @Override
    public boolean approveOrDenySecureSession(Properties i2cpProps, Properties props) throws SAMException {
        Handler handler = new Handler(Looper.getMainLooper());
        SAMSecureRunnable ssr = new SAMSecureRunnable();
        handler.post(ssr);
        return ssr.isResult();
    }
}
