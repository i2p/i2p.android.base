package net.i2p.android.router.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import net.i2p.android.router.R;

public class WebActivity extends I2PActivityBase {

    final static String HTML_RESOURCE_ID = "html_resource_id";
    private static final String WARNING = "Warning - while the welcome screen is local, web pages and " +
               "any links visited in this window are fetched over the regular internet and are " +
               "not anonymous. I2P fetches leak .i2p DNS requests and do not load images or CSS.\n";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web);
        TextView tv = (TextView) findViewById(R.id.browser_status);
        tv.setText(WARNING);
        WebView wv = (WebView) findViewById(R.id.browser_webview);
        wv.setWebViewClient(new I2PWebViewClient());
        wv.getSettings().setBuiltInZoomControls(true);
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null) {
            wv.getSettings().setLoadsImagesAutomatically(true);
            wv.loadUrl(uri.toString());
        } else {
            wv.getSettings().setLoadsImagesAutomatically(false);
            int id = intent.getIntExtra(HTML_RESOURCE_ID, R.raw.welcome_html);
            loadResource(wv, id);
        }
    }

    private void loadResource(WebView wv, int id) {
        InputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(32*1024);
        byte buf[] = new byte[4096];
        try {
            in = getResources().openRawResource(id);
            
            int read = 0;
            while ( (read = in.read(buf)) != -1)
                out.write(buf, 0, read);
            
        } catch (IOException ioe) {
            System.err.println("resource error " + ioe);
        } catch (Resources.NotFoundException nfe) {
        } finally {
            if (in != null) try { in.close(); } catch (IOException ioe) {}
        }
        try {
            String page = out.toString("UTF-8");
            wv.loadData(page, "text/html", "UTF-8");
        } catch (UnsupportedEncodingException uee) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        WebView wv = (WebView) findViewById(R.id.browser_webview);
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            wv.stopLoading();
            if (wv.canGoBack()) {
                wv.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
