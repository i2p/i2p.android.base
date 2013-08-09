package net.i2p.android.router.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;

/**
 *  Display a raw text resource.
 *  The resource ID must be passed as an extra in the intent.
 */
public class TextResourceFragment extends I2PFragmentBase {

    final static String TEXT_RESOURCE_ID = "text_resource_id";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.text_resource, container, false);
        TextView tv = (TextView) v.findViewById(R.id.text_resource_text);
        tv.setMovementMethod(ScrollingMovementMethod.getInstance());
        Intent intent = getActivity().getIntent();
        int id = intent.getIntExtra(TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
        if (id == R.raw.releasenotes_txt)
            tv.setText("Release Notes for Release " + Util.getOurVersion(getActivity()) + "\n\n" +
                       getResourceAsString(id));
        else
            tv.setText(getResourceAsString(id));
        return v;
    }

    private String getResourceAsString(int id) {
        InputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
        byte buf[] = new byte[1024];
        try {
            in = getResources().openRawResource(id);
            
            int read;
            while ( (read = in.read(buf)) != -1)
                out.write(buf, 0, read);
            
        } catch (IOException ioe) {
            System.err.println("resource error " + ioe);
        } catch (Resources.NotFoundException nfe) {
            System.err.println("resource error " + nfe);
        } finally {
            if (in != null) try { in.close(); } catch (IOException ioe) {}
        }
        try {
            return out.toString("UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
        return "";
    }
}
