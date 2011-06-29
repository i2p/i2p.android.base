package net.i2p.android.router.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

import net.i2p.android.apps.EepGetFetcher;
import net.i2p.android.router.provider.CacheProvider;
import net.i2p.android.router.util.AppCache;
import net.i2p.android.router.util.Util;
import net.i2p.util.EepGet;

class I2PWebViewClient extends WebViewClient {

    private BGLoad _lastTask;

    // TODO add some inline style
    private static final String HEADER = "<html><head></head><body>";
    private static final String FOOTER = "</body></html>";
    private static final String ERROR_EEPSITE = HEADER + "Sorry, eepsites not yet supported" + FOOTER;

    public I2PWebViewClient(Context ctx) {
        super();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Util.e("Should override? " + url);
        view.stopLoading();

            Uri uri = Uri.parse(url);
            String s = uri.getScheme();
            if (s == null) {
                fail(view, "Bad URL " + url);
                return true;
            }
            s = s.toLowerCase();
            if (!(s.equals("http") || s.equals("https") ||
                  s.equals("content"))) {
                Util.e("Not loading URL " + url);
                return false;
            }
            String h = uri.getHost();
            if (h == null) {
                fail(view, "Bad URL " + url);
                return true;
            }

            if (!Util.isConnected(view.getContext())) {
                fail(view, "No Internet connection is available");
                return true;
            }

            h = h.toLowerCase();
            if (h.endsWith(".i2p")) {
                if (!s.equals("http")) {
                    fail(view, "Bad URL " + url);
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
                if (s.equals("content")) {
                    // canonicalize to append query to path
                    // because the resolver doesn't send a query to the provider
                    Uri canon = CacheProvider.getContentUri(uri);
                    if (canon == null) {
                        fail(view, "Bad URL " + url);
                        return true;
                    }
                    url = canon.toString();
                }
                view.getSettings().setLoadsImagesAutomatically(true);
                ///////// API 8
                view.getSettings().setBlockNetworkLoads(false);
                //view.loadUrl(url);
                BGLoad task = new BackgroundLoad(view);
                _lastTask = task;
                Util.e("Fetching via web or resource: " + url);
                task.execute(url);
            }
            return true;
    }

    private static void fail(View v, String s) {
        Util.e("Fail toast: " + s);
        Toast toast = Toast.makeText(v.getContext(), s, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    @Override
    public void onLoadResource(WebView view, String url) {
        Util.e("OLR URL: " + url);
        super.onLoadResource(view, url);
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        Util.e("ORE " + errorCode + " Desc: " + description + " URL: " + failingUrl);
        super.onReceivedError(view, errorCode, description, failingUrl);
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
            Util.e("Cancelling fetches");
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

        public BackgroundEepLoad(WebView view, String host) {
            super(view);
            _host = host;
        }

        protected Integer doInBackground(String... urls) {
            String url = urls[0];
            Uri uri = Uri.parse(url);
            if (AppCache.getInstance(_view.getContext()).getCacheFile(uri).exists()) {
                Uri resUri = AppCache.getInstance(_view.getContext()).getCacheUri(uri);
                Util.e("Loading " + url + " from resource cache " + resUri);
                _view.getSettings().setLoadsImagesAutomatically(true);
                _view.getSettings().setBlockNetworkLoads(false);
                try {
                    _view.loadUrl(resUri.toString());
                } catch (Exception e) {
                    // CalledFromWrongThreadException
                    cancel(false);
                }
                return Integer.valueOf(0);
            }

            publishProgress(Integer.valueOf(-1));
            EepGetFetcher fetcher = new EepGetFetcher(url);
            fetcher.addStatusListener(this);
            boolean success = fetcher.fetch();
            if (isCancelled()) {
                Util.e("Fetch cancelled for " + url);
                return Integer.valueOf(0);
            }
            if (!success)
                Util.e("Fetch failed for " + url);
            String t = fetcher.getContentType();
            String d = fetcher.getData();
            int len = d.length();
            // http://stackoverflow.com/questions/3961589/android-webview-and-loaddata
            if (success && t.startsWith("text/html") && !d.startsWith("<?xml"))
                d = XML_HEADER + d;
            String e = fetcher.getEncoding();
            Util.e("Len: " + len + " type: \"" + t + "\" encoding: \"" + e + '"');
            if (isCancelled()) {
                Util.e("Fetch cancelled for " + url);
                return Integer.valueOf(0);
            }
            String history = url;
            if (success) {
                OutputStream out = null;
                try {
                    out = AppCache.getInstance(_view.getContext()).createCacheFile(uri);
                    out.write(d.getBytes(e));
                    Uri content = AppCache.getInstance(_view.getContext()).addCacheFile(uri);
                    if (content != null)
                        history = content.toString();
                    Util.e("Stored cache in " + history);
                } catch (Exception ex) {
                    AppCache.getInstance(_view.getContext()).removeCacheFile(uri);
                    Util.e("cache create error", ex);
                } finally {
                    if (out != null) try { out.close(); } catch (IOException ioe) {}
                }
            } else {
                history = url;
            }
            try {
                Util.e("loading data, base URL: " + url + " history URL: " + history);
                _view.loadDataWithBaseURL(url, d, t, e, history);
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
