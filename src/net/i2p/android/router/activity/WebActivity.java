package net.i2p.android.router.activity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import net.i2p.android.router.R;

public class WebActivity extends I2PActivityBase {

    private I2PWebViewClient _wvClient;

    final static String HTML_RESOURCE_ID = "html_resource_id";
    private static final String WARNING = "Warning - " +
               "any non-I2P links visited in this window are fetched over the regular internet and are " +
               "not anonymous. I2P pages may not load images or CSS.";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web);
        TextView tv = (TextView) findViewById(R.id.browser_status);
        tv.setText(WARNING);
        WebView wv = (WebView) findViewById(R.id.browser_webview);
        _wvClient = new I2PWebViewClient(this);
        wv.setWebViewClient(_wvClient);
        wv.getSettings().setBuiltInZoomControls(true);
        // http://stackoverflow.com/questions/2369310/webview-double-tap-zoom-not-working-on-a-motorola-droid-a855
        wv.getSettings().setUseWideViewPort(true);
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri != null) {
            //wv.getSettings().setLoadsImagesAutomatically(true);
            //wv.loadUrl(uri.toString());
            // go thru the client so .i2p will work too
            _wvClient.shouldOverrideUrlLoading(wv, uri.toString());
        } else {
            wv.getSettings().setLoadsImagesAutomatically(false);
            int id = intent.getIntExtra(HTML_RESOURCE_ID, 0);
            // no default, so restart should keep previous view
            if (id != 0)
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
            _wvClient.cancelAll();
            wv.stopLoading();
            if (wv.canGoBack()) {
                // TODO go into history, get url and call shouldOverrideUrlLoading()
                // so we have control ??? But then back won't work right
                wv.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        WebView wv = (WebView) findViewById(R.id.browser_webview);
        switch (item.getItemId()) {
        case R.id.menu_reload:
            _wvClient.cancelAll();
            wv.stopLoading();
            String url = wv.getUrl();
            Uri uri = Uri.parse(url);
            if ("data".equals(uri.getScheme())) {
                // welcome page... or just do nothing ?
                wv.reload();
            } else {
                // wv.reload() doesn't call shouldOverrideUrlLoading(), so do it this way
                _wvClient.deleteCurrentPageCache(wv);
                _wvClient.shouldOverrideUrlLoading(wv, url);
            }
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

}
