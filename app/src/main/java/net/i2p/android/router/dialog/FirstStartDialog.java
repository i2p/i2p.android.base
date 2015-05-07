package net.i2p.android.router.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import net.i2p.android.router.R;
import net.i2p.android.router.util.I2Patterns;

public class FirstStartDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater li = LayoutInflater.from(getActivity());
        View view = li.inflate(R.layout.fragment_dialog_first_start, null);

        TextView tv = (TextView)view.findViewById(R.id.url_faq);
        Linkify.addLinks(tv, I2Patterns.I2P_WEB_URL, "http://");
        tv = (TextView)view.findViewById(R.id.url_irc_i2p);
        Linkify.addLinks(tv, I2Patterns.IRC_URL, "irc://");

        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle(R.string.first_start_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });
        return b.create();
    }
}
