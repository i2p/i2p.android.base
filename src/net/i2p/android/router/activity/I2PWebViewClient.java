package net.i2p.android.router.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.Gravity;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;

import net.i2p.android.apps.EepGetFetcher;
import net.i2p.android.router.util.Util;
import net.i2p.util.EepGet;

class I2PWebViewClient extends WebViewClient {

    private BGLoad _lastTask;

    // TODO add some inline style
    private static final String HEADER = "<html><head></head><body>";
    private static final String FOOTER = "</body></html>";
    private static final String ERROR_EEPSITE = HEADER + "Sorry, eepsites not yet supported" + FOOTER;

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        System.err.println("Should override? " + url);
        view.stopLoading();
        try {
            URI uri = new URI(url);
            String s = uri.getScheme();
            if (s == null) {
                Toast toast = Toast.makeText(view.getContext(), "Bad URL " + url, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return true;
            }
            s = s.toLowerCase();
            if (!(s.equals("http") || s.equals("https")))
                return false;
            String h = uri.getHost();
            if (h == null) {
                Toast toast = Toast.makeText(view.getContext(), "Bad URL " + url, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return true;
            }

            if (!Util.isConnected(view.getContext())) {
                Toast toast = Toast.makeText(view.getContext(), "No Internet connection is available", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return true;
            }

            h = h.toLowerCase();
            if (h.endsWith(".i2p")) {
                if (!s.equals("http")) {
                    Toast toast = Toast.makeText(view.getContext(), "Bad URL " + url, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return true;
                }

                // TODO check that the router is up and we have client tunnels both ways

                // strip trailing junk
                int hash = url.indexOf("#");
                if (hash > 0)
                    url = url.substring(0, hash);
                view.getSettings().setLoadsImagesAutomatically(false);
                ///////// API 8
                // Otherwise hangs waiting for CSS
                view.getSettings().setBlockNetworkLoads(true);
                //view.loadData(ERROR_EEPSITE, "text/html", "UTF-8");
                BGLoad task = new BackgroundEepLoad(view, h);
                _lastTask = task;
                task.execute(url);
            } else {
                view.getSettings().setLoadsImagesAutomatically(true);
                ///////// API 8
                view.getSettings().setBlockNetworkLoads(false);
                //view.loadUrl(url);
                BGLoad task = new BackgroundLoad(view);
                _lastTask = task;
                task.execute(url);
            }
            return true;
        } catch (URISyntaxException use) {
            return false;
        }
    }

/******
  API 11 :(

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {

    }

******/

    void cancelAll() {
        BGLoad task = _lastTask;
        if (task != null) {
            System.err.println("Cancelling fetches");
            task.cancel(true);
        }
    }

    private abstract static class BGLoad extends AsyncTask<String, Integer, Integer> implements DialogInterface.OnCancelListener {
        protected final WebView _view;
        protected ProgressDialog _dialog;

        public BGLoad(WebView view) {
            _view = view;
        }

        @Override
        protected void onPostExecute(Integer result) {
            dismiss();
        }

        @Override
        protected void onCancelled() {
            dismiss();
        }

        private void dismiss() {
            if (_dialog != null && _dialog.isShowing()) {
                try {
                    // throws IAE - not attached to window manager - on screen rotation
                    // isShowing() may cover it though.
                    _dialog.dismiss();
                } catch (Exception e) {}
            }
        }

        /** cancel listener */
        public void onCancel(DialogInterface dialog) {
            cancel(true);
        }
    }


    private static class BackgroundLoad extends BGLoad {

        public BackgroundLoad(WebView view) {
            super(view);
        }

        protected Integer doInBackground(String... urls) {
            publishProgress(Integer.valueOf(-1));
            try {
                _view.loadUrl(urls[0]);
            } catch (Exception e) {
                // CalledFromWrongThreadException
                cancel(false);
            }
            return Integer.valueOf(0);
        }

        protected void onProgressUpdate(Integer... progress) {
            if (isCancelled())
                return;
            if (progress[0].intValue() < 0) {
                _dialog = ProgressDialog.show(_view.getContext(), "Loading", "some url");
                _dialog.setOnCancelListener(this);
                _dialog.setCancelable(true);
            }
        }


    }

    // http://stackoverflow.com/questions/3961589/android-webview-and-loaddata
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";

    private static class BackgroundEepLoad extends BGLoad implements EepGet.StatusListener {
        private final String _host;
        private int _total;
        private String _data;

        public BackgroundEepLoad(WebView view, String host) {
            super(view);
            _host = host;
        }

        protected Integer doInBackground(String... urls) {
            String url = urls[0];
            publishProgress(Integer.valueOf(-1));
            EepGetFetcher fetcher = new EepGetFetcher(url);
            fetcher.addStatusListener(this);
            boolean success = fetcher.fetch();
            if (isCancelled()) {
                System.err.println("Fetch cancelled for " + url);
                return Integer.valueOf(0);
            }
            if (!success)
                System.err.println("Fetch failed for " + url);
            String t = fetcher.getContentType();
            String d = fetcher.getData();
            int len = d.length();
            // http://stackoverflow.com/questions/3961589/android-webview-and-loaddata
            if (success && t.startsWith("text/html") && !d.startsWith("<?xml"))
                d = XML_HEADER + d;
            String e = fetcher.getEncoding();
            System.err.println("Len: " + len + " type: \"" + t + "\" encoding: \"" + e + '"');
            if (isCancelled()) {
                System.err.println("Fetch cancelled for " + url);
                return Integer.valueOf(0);
            }
            try {
                _view.loadDataWithBaseURL(url, d, t, e, url);
            } catch (Exception exc) {
                // CalledFromWrongThreadException
                cancel(false);
            }
            return Integer.valueOf(0);
        }

        protected void onProgressUpdate(Integer... progress) {
            if (isCancelled())
                return;
            int prog = progress[0].intValue();
            if (prog < 0) {
                // Can't change style on the fly later, results in an NPE in setMax()
                //_dialog = ProgressDialog.show(_view.getContext(), "Fetching...", "from " + _host);
                ProgressDialog d = new ProgressDialog(_view.getContext());
                d.setCancelable(true);
                d.setTitle("Fetching...");
                d.setMessage("...from " + _host);
                d.setIndeterminate(true);
                d.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                d.show();
                d.setOnCancelListener(this);
                _dialog = d;
            } else if (prog == 0 && _total > 0) {
                _dialog.setTitle("Downloading");
                _dialog.setIndeterminate(false);
                _dialog.setMax(_total);
                _dialog.setProgress(0);
            } else if (_total > 0) {
                _dialog.setProgress(prog);
            } else {
                // nothing
            }
        }

        // EepGet callbacks

        public void attemptFailed(String url, long bytesTransferred, long bytesRemaining, int currentAttempt, int numRetries, Exception cause) {}

        public void bytesTransferred(long alreadyTransferred, int currentWrite, long bytesTransferred, long bytesRemaining, String url) {
            publishProgress(Integer.valueOf(Math.max(0, (int) (alreadyTransferred + bytesTransferred))));
        }

        public void transferComplete(long alreadyTransferred, long bytesTransferred, long bytesRemaining, String url, String outputFile, boolean notModified) {}

        public void transferFailed(String url, long bytesTransferred, long bytesRemaining, int currentAttempt) {}

        public void headerReceived(String url, int attemptNum, String key, String val) {
            if (key.equalsIgnoreCase("Content-Length")) {
                try {
                    _total = Integer.parseInt(val.trim());
                    publishProgress(Integer.valueOf(0));
                } catch (NumberFormatException nfe) {}
            }
        }

        public void attempting(String url) {}
    }
}
