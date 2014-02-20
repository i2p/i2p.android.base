package net.i2p.android.router.dialog;

import net.i2p.android.router.I2PActivityBase;
import net.i2p.android.router.LicenseActivity;
import net.i2p.android.router.MainFragment;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class VersionDialog extends DialogFragment {
    public static final String DIALOG_TYPE = "dialog_type";
    public static final int DIALOG_NEW_INSTALL = 0;
    public static final int DIALOG_NEW_VERSION = 1;

    @Override
    public Dialog onCreateDialog(Bundle SavedInstanceState) {
        final String currentVersion = Util.getOurVersion(getActivity());
        Dialog rv = null;
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        int id = getArguments().getInt(DIALOG_TYPE);
        switch(id) {
            case DIALOG_NEW_INSTALL:
                b.setMessage(R.string.welcome_new_install)
                 .setCancelable(false)
                 .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        I2PActivityBase ab = (I2PActivityBase) getActivity();
                        ab.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        dialog.dismiss();
                    }
                }).setNeutralButton(R.string.label_release_notes, new DialogInterface.OnClickListener() {

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
                }).setNegativeButton(R.string.label_licenses, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        I2PActivityBase ab = (I2PActivityBase) getActivity();
                        ab.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        dialog.dismiss();
                        Intent intent = new Intent(getActivity(), LicenseActivity.class);
                        startActivity(intent);
                    }
                });
                rv = b.create();
                break;

            case DIALOG_NEW_VERSION:
                b.setMessage(getResources().getString(R.string.welcome_new_version) +
                             " " + currentVersion)
                 .setCancelable(true)
                 .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        I2PActivityBase ab = (I2PActivityBase) getActivity();
                        ab.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        try {
                            dialog.dismiss();
                        } catch(Exception e) {
                        }
                    }
                }).setNegativeButton(R.string.label_release_notes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        I2PActivityBase ab = (I2PActivityBase) getActivity();
                        ab.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        try {
                            dialog.dismiss();
                        } catch(Exception e) {
                        }
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
