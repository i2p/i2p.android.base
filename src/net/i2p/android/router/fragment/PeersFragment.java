package net.i2p.android.router.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import java.io.IOException;
import java.io.StringWriter;
import net.i2p.android.router.R;
import net.i2p.android.router.service.RouterService;
import net.i2p.router.CommSystemFacade;

public class PeersFragment extends I2PFragmentBase {

    private I2PWebViewClient _wvClient;

    // TODO add some inline style
    private static final String HEADER = "<html><head></head><body>";
    private static final String FOOTER = "</body></html>";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View v = inflater.inflate(R.layout.peers, container, false);
        WebView wv = (WebView) v.findViewById(R.id.peers_webview);
        wv.getSettings().setLoadsImagesAutomatically(true); // was false
        // http://stackoverflow.com/questions/2369310/webview-double-tap-zoom-not-working-on-a-motorola-droid-a855
        wv.getSettings().setUseWideViewPort(true);
        _wvClient = new I2PWebViewClient();
        wv.setWebViewClient(_wvClient);
        wv.getSettings().setBuiltInZoomControls(true);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    /**
     *  Not bound by the time onResume() is called, so we have to do it here.
     *  If it is bound we update twice.
     */
    @Override
    protected void onRouterBind(RouterService svc) {
        update();
    }

    private void update() {
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

    @Override
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
    public boolean onOptionsItemSelected(MenuItem item) {
        WebView wv = (WebView) getActivity().findViewById(R.id.peers_webview);
        switch (item.getItemId()) {
        case R.id.menu_reload:
            update();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
