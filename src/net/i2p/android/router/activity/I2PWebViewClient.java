package net.i2p.android.router.activity;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.URI;
import java.net.URISyntaxException;

class I2PWebViewClient extends WebViewClient {

    // TODO add some inline style
    private static final String HEADER = "<html><head></head><body>";
    private static final String FOOTER = "</body></html>";
    private static final String ERROR_EEPSITE = HEADER + "Sorry, eepsites not yet supported" + FOOTER;

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
