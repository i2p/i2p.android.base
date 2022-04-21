package net.i2p.android.router.service;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

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
    public boolean getSAMUserInput() {
        final boolean[] approve = {false};
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        approve[0] = true;
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        approve[0] = false;
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(mCtx);
        builder.setMessage(mCtx.getResources().getString(R.string.settings_confirm_sam)).setPositiveButton(mCtx.getResources().getString(R.string.settings_confirm_allow_sam), dialogClickListener)
                .setNegativeButton(mCtx.getResources().getString(R.string.settings_confirm_deny_sam), dialogClickListener).show();

        return approve[0];
    }
}
