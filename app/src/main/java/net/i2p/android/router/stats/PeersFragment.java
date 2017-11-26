package net.i2p.android.router.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import java.io.IOException;
import java.io.StringWriter;

import net.i2p.android.router.I2PFragmentBase;
import net.i2p.android.router.R;
import net.i2p.android.router.web.I2PWebViewClient;
import net.i2p.router.CommSystemFacade;

public class PeersFragment extends I2PFragmentBase {

    private I2PWebViewClient _wvClient;

    // TODO add some inline style
    private static final String HEADER = "<html><head></head><body>";
    private static final String FOOTER = "</body></html>";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.peers, container, false);
        WebView wv = (WebView) v.findViewById(R.id.peers_webview);
        wv.getSettings().setLoadsImagesAutomatically(true); // was false
        // http://stackoverflow.com/questions/2369310/webview-double-tap-zoom-not-working-on-a-motorola-droid-a855
        wv.getSettings().setUseWideViewPort(true);
        _wvClient = new I2PWebViewClient(this);
        wv.setWebViewClient(_wvClient);
        wv.getSettings().setBuiltInZoomControls(true);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    public void update() {
        WebView wv = (WebView) getActivity().findViewById(R.id.peers_webview);
        wv.clearHistory(); // fixes having to hit back.
        CommSystemFacade comm = getCommSystem();
        String data;
        if (comm != null) {
            StringWriter out = new StringWriter(32*1024);
            out.append(HEADER);
            try {
                comm.renderStatusHTML(out, "http://thiswontwork.i2p/peers", 0);
                out.append(FOOTER);
                data = out.toString().replaceAll("/themes", "themes");
            } catch (IOException ioe) {
                data = HEADER + "Error: " + ioe + FOOTER;
            }
        } else {
            data = HEADER + "No peer data available. The router is not running." + FOOTER;
        }
        try {
            // wv.loadData(data, "text/html", "UTF-8");
            // figure out a way to get /themes/console/images/outbound.png to load
            // String url = "file://" + _myDir + "/docs/";
            String url = "file:///android_asset/";
            wv.loadDataWithBaseURL(url, data, "text/html", "UTF-8", url);
        } catch (Exception e) {
        }
    }

    public boolean onBackPressed() {
        WebView wv = (WebView) getActivity().findViewById(R.id.peers_webview);
        _wvClient.cancelAll();
        wv.stopLoading();

        // We do not want to go back, or keep history... There is no need to.
        // What we DO want to do is exit!
        //if (wv.canGoBack()) {
        //    wv.goBack();
        //    return true;
        //}
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_web_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_reload:
            update();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
