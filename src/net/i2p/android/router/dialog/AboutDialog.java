package net.i2p.android.router.dialog;

import net.i2p.android.router.R;
import net.i2p.android.router.util.I2Patterns;
import net.i2p.android.router.util.Util;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class AboutDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle SavedInstanceState) {
        LayoutInflater li = LayoutInflater.from(getActivity());
        View view = li.inflate(R.layout.fragment_dialog_about, null);

        final String currentVersion = Util.getOurVersion(getActivity());
        TextView tv = (TextView)view.findViewById(R.id.about_version);
        tv.setText(currentVersion);

        tv = (TextView)view.findViewById(R.id.url_project);
        Linkify.addLinks(tv, I2Patterns.I2P_WEB_URL, "http://");
        tv = (TextView)view.findViewById(R.id.url_android_bugs);
        Linkify.addLinks(tv, I2Patterns.I2P_WEB_URL, "http://");
        tv = (TextView)view.findViewById(R.id.url_android_volunteer);
        Linkify.addLinks(tv, I2Patterns.I2P_WEB_URL, "http://");
        tv = (TextView)view.findViewById(R.id.url_donate);
        Linkify.addLinks(tv, I2Patterns.I2P_WEB_URL, "http://");

        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setTitle(R.string.menu_about)
        .setView(view);
        return b.create();
    }
}
