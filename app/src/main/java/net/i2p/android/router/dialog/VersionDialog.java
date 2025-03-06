package net.i2p.android.router.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
//import android.support.v4.app.DialogFragment;
import androidx.fragment.app.DialogFragment;
//import android.support.v7.app.AlertDialog;
import androidx.appcompat.app.AlertDialog;

import net.i2p.android.I2PActivityBase;
import net.i2p.android.router.MainFragment;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;

public class VersionDialog extends DialogFragment {
    public static final String DIALOG_TYPE = "dialog_type";
    public static final int DIALOG_NEW_INSTALL = 0;
    public static final int DIALOG_NEW_VERSION = 1;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle SavedInstanceState) {
        final String currentVersion = Util.getOurVersion(getActivity());
        Dialog rv;
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        int id = getArguments().getInt(DIALOG_TYPE);
        switch(id) {
            case DIALOG_NEW_INSTALL:
                b.setMessage(R.string.welcome_new_install)
                 .setCancelable(false)
                 .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        I2PActivityBase ab = (I2PActivityBase) getActivity();
                        ab.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        dialog.dismiss();
                    }
                }).setNegativeButton(R.string.label_release_notes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        I2PActivityBase ab = (I2PActivityBase) getActivity();
                        ab.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        dialog.dismiss();
                        TextResourceDialog f = new TextResourceDialog();
                        Bundle args = new Bundle();
                        args.putInt(TextResourceDialog.TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
                        f.setArguments(args);
                        getActivity().getSupportFragmentManager()
                                     .beginTransaction()
                                     .replace(R.id.main_fragment, f)
                                     .addToBackStack(null)
                                     .commit();
                    }
                });
                rv = b.create();
                break;

            case DIALOG_NEW_VERSION:
            default:
                b.setMessage(getResources().getString(R.string.welcome_new_version) +
                             " " + currentVersion)
                 .setCancelable(true)
                 .setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        I2PActivityBase ab = (I2PActivityBase) getActivity();
                        ab.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        dialog.dismiss();
                    }
                }).setNegativeButton(R.string.label_release_notes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        I2PActivityBase ab = (I2PActivityBase) getActivity();
                        ab.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        dialog.dismiss();
                        TextResourceDialog f = new TextResourceDialog();
                        Bundle args = new Bundle();
                        args.putInt(TextResourceDialog.TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
                        f.setArguments(args);
                        getActivity().getSupportFragmentManager()
                                     .beginTransaction()
                                     .replace(R.id.main_fragment, f)
                                     .addToBackStack(null)
                                     .commit();
                    }
                });

                rv = b.create();
                break;
        }
        return rv;
    }
}
