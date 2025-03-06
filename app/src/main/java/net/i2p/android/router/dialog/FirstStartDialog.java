package net.i2p.android.router.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
//import android.support.v4.app.DialogFragment;
import androidx.fragment.app.DialogFragment;
//import android.support.v7.app.AlertDialog;
import androidx.appcompat.app.AlertDialog;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import net.i2p.android.router.R;
import net.i2p.android.router.util.I2Patterns;

import java.util.List;

public class FirstStartDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater li = LayoutInflater.from(getActivity());
        View view = li.inflate(R.layout.fragment_dialog_first_start, null);

        TextView tv = (TextView)view.findViewById(R.id.url_faq);
        Linkify.addLinks(tv, I2Patterns.I2P_WEB_URL, "http://");

        // Find all installed browsers that listen for "irc://"
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("irc://127.0.0.1:6668/i2p"));
        final PackageManager pm = getActivity().getPackageManager();
        List<ResolveInfo> installedIrcClients = pm.queryIntentActivities(intent, 0);

        // Only linkify "irc://" if we have an app that can handle them.
        // Otherwise, the app crashes with an un-catchable ActivityNotFoundException
        // if the user clicks one of them.
        if (installedIrcClients.size() > 0) {
            tv = (TextView) view.findViewById(R.id.url_irc_i2p);
            Linkify.addLinks(tv, I2Patterns.IRC_URL, "irc://");
        }

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
