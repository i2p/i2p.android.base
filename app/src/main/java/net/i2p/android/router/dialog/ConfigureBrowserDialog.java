package net.i2p.android.router.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import net.i2p.android.help.BrowserConfigActivity;
import net.i2p.android.help.HelpActivity;
import net.i2p.android.router.R;
import net.i2p.android.router.util.I2Patterns;

public class ConfigureBrowserDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle(R.string.configure_browser_title)
                .setMessage(R.string.configure_browser_for_i2p)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        Intent hi = new Intent(getActivity(), BrowserConfigActivity.class);
                        startActivity(hi);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        return b.create();
    }
}
