package net.i2p.android.router.activity;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.webkit.WebView;
import java.io.IOException;
import java.io.StringWriter;
import net.i2p.android.router.R;
import net.i2p.android.router.service.RouterService;
import net.i2p.router.CommSystemFacade;

public class PeersActivity extends I2PActivityBase {

    private I2PWebViewClient _wvClient;

    // TODO add some inline style
    private static final String HEADER = "<html><head></head><body>";
    private static final String FOOTER = "</body></html>";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.peers);
        WebView wv = (WebView) findViewById(R.id.peers_webview);
        wv.getSettings().setLoadsImagesAutomatically(false);
        // http://stackoverflow.com/questions/2369310/webview-double-tap-zoom-not-working-on-a-motorola-droid-a855
        wv.getSettings().setUseWideViewPort(true);
        _wvClient = new I2PWebViewClient(this);
        wv.setWebViewClient(_wvClient);
        wv.getSettings().setBuiltInZoomControls(true);
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
        WebView wv = (WebView) findViewById(R.id.peers_webview);
        CommSystemFacade comm = getCommSystem();
        String data;
        if (comm != null) {
            StringWriter out = new StringWriter(32*1024);
            out.append(HEADER);
            try {
                comm.renderStatusHTML(out, "http://thiswontwork.i2p/peers", 0);
                out.append(FOOTER);
                data = out.toString();
            } catch (IOException ioe) {
                data = HEADER + "Error: " + ioe + FOOTER;
            }
        } else {
            data = HEADER + "No peer data available. The router is not running." + FOOTER;
        }
        try {
            wv.loadData(data, "text/html", "UTF-8");
            // figure out a way to get /themes/console/images/outbound.png to load
            //String url = "file://" + _myDir + "/docs/";
            //wv.loadDataWithBaseURL(url, data, "text/html", "UTF-8", url);
        } catch (Exception e) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        WebView wv = (WebView) findViewById(R.id.peers_webview);
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            _wvClient.cancelAll();
            wv.stopLoading();
            if (wv.canGoBack()) {
                wv.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        WebView wv = (WebView) findViewById(R.id.peers_webview);
        switch (item.getItemId()) {
        case R.id.menu_reload:
            update();
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
}
