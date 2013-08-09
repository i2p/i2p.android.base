package net.i2p.android.router.fragment;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import net.i2p.android.apps.NewsFetcher;
import net.i2p.android.router.R;

public class NewsFragment extends I2PFragmentBase {

    private I2PWebViewClient _wvClient;
    private long _lastChanged;

    private static final String WARNING = "Warning - while the news is fetched over I2P, " +
               "any non-I2P links visited in this window are fetched over the regular internet and are " +
               "not anonymous.\n";


    // TODO add some inline style
    private static final String HEADER = "<html><head></head><body>";
    private static final String FOOTER = "</body></html>";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.news, container, false);
        WebView wv = (WebView) v.findViewById(R.id.news_webview);
        wv.getSettings().setLoadsImagesAutomatically(false);
        // http://stackoverflow.com/questions/2369310/webview-double-tap-zoom-not-working-on-a-motorola-droid-a855
        wv.getSettings().setUseWideViewPort(true);
        _wvClient = new I2PWebViewClient();
        wv.setWebViewClient(_wvClient);
        wv.getSettings().setBuiltInZoomControls(true);
        return v;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        NewsFetcher nf = NewsFetcher.getInstance();
        if (nf != null) {
            // always update the text
            TextView tv = (TextView) getActivity().findViewById(R.id.news_status);
            tv.setText(WARNING + nf.status().replace("&nbsp;", " "));
        }

        // only update the webview if we need to
        File newsFile = new File(_myDir, "docs/news.xml");
        boolean newsExists = newsFile.exists();
        if (_lastChanged > 0 && ((!newsExists) || newsFile.lastModified() < _lastChanged))
            return;
        _lastChanged = System.currentTimeMillis();

        WebView wv = (WebView) getActivity().findViewById(R.id.news_webview);

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

            int read;
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
    public boolean onBackPressed() {
        WebView wv = (WebView) getActivity().findViewById(R.id.news_webview);
        _wvClient.cancelAll();
        wv.stopLoading();
        if (wv.canGoBack()) {
            wv.goBack();
            return true;
        }
        return false;
    }
}
