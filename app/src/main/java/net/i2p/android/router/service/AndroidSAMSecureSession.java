package net.i2p.android.router.service;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;

import net.i2p.android.router.R;
import net.i2p.sam.*;
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
    @RequiresApi(api = Build.VERSION_CODES.P)
    public boolean getSAMUserInput() {
        final int[] approve = {-1};
        //ContextCompat.getMainExecutor(mCtx)
        mCtx.getMainExecutor().execute(new Runnable() {
            @Override
            public void run() {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                approve[0] = 1;
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                approve[0] = 0;
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
                builder.setMessage(mCtx.getResources().getString(R.string.settings_confirm_sam)).setPositiveButton(mCtx.getResources().getString(R.string.settings_confirm_allow_sam), dialogClickListener)
                        .setNegativeButton(mCtx.getResources().getString(R.string.settings_confirm_deny_sam), dialogClickListener).show();
            }
        });
        //wait until we have input
        while (approve[0] == -1) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return false;
            }
        }

        return (approve[0]==0) ? false : true;
    }
}
