package net.i2p.android.router.activity;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import net.i2p.android.router.R;
import net.i2p.android.apps.NewsFetcher;

public class NewsActivity extends I2PActivityBase {

    private long _lastChanged;

    // TODO add some inline style
    private static final String HEADER = "<html><head></head><body>";
    private static final String FOOTER = "</body></html>";
    private static final String ERROR_EEPSITE = HEADER + "Sorry, eepsites not yet supported" + FOOTER;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.news);
        WebView wv = (WebView) findViewById(R.id.news_webview);
        wv.setWebViewClient(new NewsWebViewClient());
    }

    @Override
    public void onResume()
    {
        super.onResume();
        NewsFetcher nf = NewsFetcher.getInstance();
        if (nf != null) {
            // always update the text
            TextView tv = (TextView) findViewById(R.id.news_status);
            tv.setText(nf.status().replace("&nbsp;", " "));
        }

        // only update the webview if we need to
        File newsFile = new File(_myDir, "docs/news.xml");
        boolean newsExists = newsFile.exists();
        if (_lastChanged > 0 && ((!newsExists) || newsFile.lastModified() < _lastChanged))
            return;
        _lastChanged = System.currentTimeMillis();

        WebView wv = (WebView) findViewById(R.id.news_webview);

        InputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
        byte buf[] = new byte[1024];
        try {
            if (newsExists) {
                out.write(HEADER.getBytes());
                in = new FileInputStream(newsFile);
            } else {
                in = getResources().openRawResource(R.raw.initialnews_html);
            }
            
            int read = 0;
            while ( (read = in.read(buf)) != -1)
                out.write(buf, 0, read);

            if (newsExists)
                out.write(FOOTER.getBytes());
            
        } catch (IOException ioe) {
            System.err.println("news error " + ioe);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        WebView wv = (WebView) findViewById(R.id.news_webview);
        if ((keyCode == KeyEvent.KEYCODE_BACK) && wv.canGoBack()) {
            wv.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private static class NewsWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            System.err.println("Should override? " + url);
            try {
                URI uri = new URI(url);
                String s = uri.getScheme();
                if (s == null)
                    return false;
                s = s.toLowerCase();
                if (!(s.equals("http") || s.equals("https")))
                    return false;
                String h = uri.getHost();
                if (h == null)
                    return false;
                h = h.toLowerCase();
                if (h.endsWith(".i2p")) {
                    // if (s.equals("https")
                    //    return false;
                    view.loadData(ERROR_EEPSITE, "text/html", "UTF-8");
                } else {
                    view.loadUrl(url);
                }
                return true;
            } catch (URISyntaxException use) {
                return false;
            }
        }
    }
}
