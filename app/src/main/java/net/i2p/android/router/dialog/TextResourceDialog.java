package net.i2p.android.router.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Display a raw text resource.
 * The resource ID must be passed as an extra in the intent.
 */
public class TextResourceDialog extends DialogFragment {
    public static final String TEXT_DIALOG_TITLE = "text_title";
    public final static String TEXT_RESOURCE_ID = "text_resource_id";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View v = inflater.inflate(R.layout.fragment_dialog_text_resource, null, false);
        TextView tv = (TextView) v.findViewById(R.id.text_resource_text);
        String title = getArguments().getString(TEXT_DIALOG_TITLE);
        if (title != null)
            b.setTitle(title);
        int id = getArguments().getInt(TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
        if (id == R.raw.releasenotes_txt)
            tv.setText("Release Notes for Release " + Util.getOurVersion(getActivity()) + "\n\n" +
                    getResourceAsString(id));
        else
            tv.setText(getResourceAsString(id));
        b.setView(v)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

        return b.create();
    }

    private String getResourceAsString(int id) {
        InputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
        byte buf[] = new byte[1024];
        try {
            in = getResources().openRawResource(id);

            int read;
            while ((read = in.read(buf)) != -1)
                out.write(buf, 0, read);

        } catch (IOException | Resources.NotFoundException re) {
            System.err.println("resource error " + re);
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException ioe) {
            }
        }
        try {
            return out.toString("UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
        return "";
    }
}
