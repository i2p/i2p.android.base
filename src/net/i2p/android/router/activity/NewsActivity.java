package net.i2p.android.router.activity;

import android.content.res.Resources;
import android.os.Bundle;
import android.webkit.WebView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import net.i2p.android.router.R;

public class NewsActivity extends I2PActivityBase {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news);
        WebView wv = (WebView) findViewById(R.id.news_webview);
        if (wv == null) {
            System.err.println("No webview resource!");
            return;
        }

        InputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        byte buf[] = new byte[1024];
        try {
            in = getResources().openRawResource(R.raw.initialnews_html);
            
            int read = 0;
            while ( (read = in.read(buf)) != -1)
                out.write(buf, 0, read);
            
        } catch (IOException ioe) {
        } catch (Resources.NotFoundException nfe) {
        } finally {
            if (in != null) try { in.close(); } catch (IOException ioe) {}
        }
        try {
            String news = out.toString("UTF-8");
            wv.loadData(news, "text/html", "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
    }
}
