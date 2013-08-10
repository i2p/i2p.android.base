package net.i2p.android.router.fragment;

import net.i2p.android.router.R;
import net.i2p.android.router.activity.LicenseActivity;
import net.i2p.android.router.util.Util;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class VersionDialog extends DialogFragment {
    protected static final String DIALOG_TYPE = "dialog_type";
    protected static final int DIALOG_NEW_INSTALL = 0;
    protected static final int DIALOG_NEW_VERSION = 1;

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
                        I2PFragmentBase fb = (I2PFragmentBase) getActivity()
                                .getSupportFragmentManager()
                                .findFragmentById(R.id.main_content);
                        fb.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        dialog.cancel();
                    }
                }).setNeutralButton(R.string.label_release_notes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        I2PFragmentBase fb = (I2PFragmentBase) getActivity()
                                .getSupportFragmentManager()
                                .findFragmentById(R.id.main_content);
                        fb.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        dialog.cancel();
                        TextResourceFragment f = new TextResourceFragment();
                        Bundle args = new Bundle();
                        args.putInt(TextResourceFragment.TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
                        f.setArguments(args);
                        getActivity().getSupportFragmentManager()
                                     .beginTransaction()
                                     .replace(R.id.main_content, f)
                                     .addToBackStack(null)
                                     .commit();
                    }
                }).setNegativeButton(R.string.label_licenses, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        I2PFragmentBase fb = (I2PFragmentBase) getActivity()
                                .getSupportFragmentManager()
                                .findFragmentById(R.id.main_content);
                        fb.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        dialog.cancel();
                        Intent intent = new Intent(getActivity(), LicenseActivity.class);
                        startActivity(intent);
                    }
                });
                rv = b.create();
                break;

            case DIALOG_NEW_VERSION:
                b.setMessage(R.string.welcome_new_version + " " + currentVersion)
                 .setCancelable(true)
                 .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        I2PFragmentBase fb = (I2PFragmentBase) getActivity()
                                .getSupportFragmentManager()
                                .findFragmentById(R.id.main_content);
                        fb.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        try {
                            dialog.dismiss();
                        } catch(Exception e) {
                        }
                    }
                }).setNegativeButton(R.string.label_release_notes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        I2PFragmentBase fb = (I2PFragmentBase) getActivity()
                                .getSupportFragmentManager()
                                .findFragmentById(R.id.main_content);
                        fb.setPref(MainFragment.PREF_INSTALLED_VERSION, currentVersion);
                        try {
                            dialog.dismiss();
                        } catch(Exception e) {
                        }
                        TextResourceFragment f = new TextResourceFragment();
                        Bundle args = new Bundle();
                        args.putInt(TextResourceFragment.TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
                        f.setArguments(args);
                        getActivity().getSupportFragmentManager()
                                     .beginTransaction()
                                     .replace(R.id.main_content, f)
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
