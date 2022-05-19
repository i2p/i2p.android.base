package net.i2p.android.router.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;

import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
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

    public Activity getActivity(){
        Context aCtx = mCtx.getApplicationContext();
        return getActivity(mCtx);
    }

    private Activity getActivity(Context baseContext)
    {
        if (baseContext == null)
        {
            Util.e("Base Context is Null");
            return null;
        }
        else if (baseContext instanceof ContextWrapper)
        {
            if (baseContext instanceof Activity)
            {
                Util.i("Base Context is Activity");
                return (Activity) baseContext;
            }
            else
            {
                Util.i("Recursively seeking Main Context");
                return getActivity(((ContextWrapper) baseContext).getBaseContext());
            }
        }

        return null;
    }
    @Override
    public boolean getSAMUserInput() {
        final int[] approve = {-1};
        Activity activity = getActivity();

        activity.runOnUiThread(new Runnable() {
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
                Util.i("Waiting on user to approve SAM connection");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Util.e("Interrupted SAM approval, failing closed", e);
                return false;
            }
        }

        return (approve[0]==0) ? false : true;
    }
}
