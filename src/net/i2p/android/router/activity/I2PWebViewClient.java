package net.i2p.android.router.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.*;
import net.i2p.android.apps.EepGetFetcher;
import net.i2p.android.router.provider.CacheProvider;
import net.i2p.android.router.util.AppCache;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataHelper;
import net.i2p.util.EepGet;

class I2PWebViewClient extends WebViewClient {

    private BGLoad _lastTask;
    /** save it here so we can dismiss it in onPageFinished() */
    private ProgressDialog _lastDialog;

    // TODO add some inline style
    private static final String CONTENT = "content";
    private static final String HEADER = "<html><head></head><body>";
    private static final String FOOTER = "</body></html>";
    private static final String ERROR_URL = "<p>Unable to load URL: ";
    private static final String ERROR_ROUTER = "<p>Your router (or the HTTP proxy) does not appear to be running.</p>";

    public I2PWebViewClient(Context ctx) {
        super();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Util.e("Should override? " + url);
        view.stopLoading();

        Uri uri = Uri.parse(url);

        if (CONTENT.equals(uri.getScheme())) {
            // Fix up top-level links like <a href="/foo">
            // take the host from the current uri and the path from the new uri
            String currentUrl = view.getUrl();
            Uri currentUri = Uri.parse(currentUrl);
            uri = CacheProvider.rectifyContentUri(currentUri, uri);

            //reverse back to a i2p URI so we can load it here and not in ContentProvider
            try {
                uri = CacheProvider.getI2PUri(uri);
                Util.e("Reversed content uri back to " + uri);
            } catch (FileNotFoundException fnfe) {}
            url = uri.toString();
        }


            String s = uri.getScheme();
            if (s == null) {
                fail(view, "Bad URL " + url);
                return true;
            }
            s = s.toLowerCase();
            if (!(s.equals("http") || s.equals("https") ||
                  s.equals(CONTENT))) {
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
                view.getSettings().setLoadsImagesAutomatically(true);
                ///////// API 8
                // Otherwise hangs waiting for CSS
                view.getSettings().setBlockNetworkLoads(false);
                _lastDialog = new ProgressDialog(view.getContext());
                BGLoad task = new BackgroundEepLoad(view, h, _lastDialog);
                _lastTask = task;
                task.execute(url);
            } else {
                if (s.equals(CONTENT)) {

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

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        Util.e("OPS URL: " + url);
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        Util.e("OPF URL: " + url);
        ProgressDialog d = _lastDialog;
        if (d != null && d.isShowing()) {
            try {
                // throws IAE - not attached to window manager - on screen rotation
                // isShowing() may cover it though.
                d.dismiss();
            } catch (Exception e) {}
        }
        super.onPageFinished(view, url);
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        Util.e("ORHAR URL: " + host);
        super.onReceivedHttpAuthRequest(view, handler, host, realm);
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

    /**
     *  This should always be a content url
     */
    void deleteCurrentPageCache(WebView view) {
        String url = view.getUrl();
        Uri uri = Uri.parse(url);
        if (CONTENT.equals(uri.getScheme())) {
            try {
                //reverse back to a i2p URI so we can delete it from the AppCache
                uri = CacheProvider.getI2PUri(uri);
                Util.e("clearing AppCache entry for current page " + uri);
                AppCache.getInstance(view.getContext()).removeCacheFile(uri);
            } catch (FileNotFoundException fnfe) {
                // this actually only deletes the row in the provider,
                // not the actual file, but it will be overwritten in the reload.
                Util.e("clearing provider entry for current page " + url);
                view.getContext().getContentResolver().delete(uri, null, null);
            }
        }
    }

    private abstract static class BGLoad extends AsyncTask<String, Integer, Integer> implements DialogInterface.OnCancelListener {
        protected final WebView _view;
        protected final ProgressDialog _dialog;

        public BGLoad(WebView view, ProgressDialog dialog) {
            _view = view;
            if (dialog != null)
                dialog.setCancelable(true);
            _dialog = dialog;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (_dialog != null && _dialog.isShowing()) {
                // since webkit is just going to sit there...
                try {
                    _dialog.setTitle("Downloading...");
                    _dialog.setMessage("...CSS and images...");
                } catch (Exception e) {}
            }
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
            super(view, null);
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

        @Override
        protected void onProgressUpdate(Integer... progress) {
            //if (isCancelled())
            //    return;

            
            //if (progress[0].intValue() < 0) {
            //    _dialog = ProgressDialog.show(_view.getContext(), "Loading", "some url");
            //    _dialog.setOnCancelListener(this);
            //    _dialog.setCancelable(true);
            //}
        }


    }

    // http://stackoverflow.com/questions/3961589/android-webview-and-loaddata
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";

    private static class BackgroundEepLoad extends BGLoad implements EepGet.StatusListener {
        private final String _host;
        private int _total;

        public BackgroundEepLoad(WebView view, String host, ProgressDialog dialog) {
            super(view, dialog);
            _host = host;
        }

        protected Integer doInBackground(String... urls) {
            String url = urls[0];
            Uri uri = Uri.parse(url);
            File cacheFile = AppCache.getInstance(_view.getContext()).getCacheFile(uri);
            if (cacheFile.exists()) {
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
                // 1 means show the cache toast message
                return Integer.valueOf(1);
            }

            publishProgress(Integer.valueOf(-1));
            //EepGetFetcher fetcher = new EepGetFetcher(url);
            OutputStream out = null;
            try {
                out = AppCache.getInstance(_view.getContext()).createCacheFile(uri);
                // write error to stream
                EepGetFetcher fetcher = new EepGetFetcher(url, out, true);
                fetcher.addStatusListener(this);
                boolean success = fetcher.fetch();
                if (isCancelled()) {
                    Util.e("Fetch cancelled for " + url);
                    return Integer.valueOf(0);
                }
                try { out.close(); } catch (IOException ioe) {}
                if (success) {
                    // store in cache, get content URL, and load that way
                    // Set as current base
                    Uri content = AppCache.getInstance(_view.getContext()).addCacheFile(uri, true);
                    if (content != null) {
                        Util.e("Stored cache in " + content);
                    } else {
                        AppCache.getInstance(_view.getContext()).removeCacheFile(uri);
                        Util.e("cache create error");
                        return Integer.valueOf(0);
                    }
                    Util.e("loading data, base URL: " + uri + " content URL: " + content);
                    try {
                        _view.loadUrl(content.toString());
                    } catch (Exception exc) {
                        // CalledFromWrongThreadException
                        cancel(false);
                    }
                    Util.e("Fetch failed for " + url);
                } else {
                    // Load the error message in as a string, delete the file
                    String t = fetcher.getContentType();
                    String e = fetcher.getEncoding();
                    String msg;
                    int statusCode = fetcher.getStatusCode();
                    if (statusCode < 0) {
                        msg = HEADER + ERROR_URL + "<a href=\"" + url + "\">" + url +
                              "</a></p>" + ERROR_ROUTER + FOOTER;
                    } else if (cacheFile.length() <= 0) {
                        msg = HEADER + ERROR_URL + "<a href=\"" + url + "\">" + url +
                              "</a> No data returned, error code: " + statusCode +
                              "</p>" + FOOTER;
                    } else {
                        InputStream fis = null;
                        try {
                            fis = new FileInputStream(cacheFile);
                            byte[] data = new byte[(int) cacheFile.length()];
                            DataHelper.read(fis, data);
                            msg = new String(data, e);
                        } catch (IOException ioe) {
                            Util.e("WVC", ioe);
                            msg = HEADER + "I/O error" + FOOTER;
                        } finally {
                              if (fis != null) try { fis.close(); } catch (IOException ioe) {}
                        }
                    }
                    AppCache.getInstance(_view.getContext()).removeCacheFile(uri);
                    try {
                         Util.e("loading error data URL: " + url);
                        _view.loadDataWithBaseURL(url, msg, t, e, url);
                    } catch (Exception exc) {
                        // CalledFromWrongThreadException
                        cancel(false);
                    }
                }
            } catch (IOException ioe) {
                    Util.e("IOE for " + url, ioe);
            } finally {
                if (out != null) try { out.close(); } catch (IOException ioe) {}
            }
            return Integer.valueOf(0);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            if (isCancelled())
                return;
            int prog = progress[0].intValue();
            if (prog < 0) {
                _dialog.setTitle("Contacting...");
                _dialog.setMessage(_host);
                _dialog.setIndeterminate(true);
                _dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                _dialog.setOnCancelListener(this);
                _dialog.show();
            } else if (prog == 0 && _total > 0) {
                _dialog.setTitle("Downloading...");
                _dialog.setMessage("...from " + _host);
                _dialog.setIndeterminate(false);
                _dialog.setMax(_total);
                _dialog.setProgress(0);
            } else if (_total > 0) {
                // so it isn't at 100% while loading images and CSS
                _dialog.setProgress(Math.min(prog, _total * 99 / 100));
            } else if (prog > 0) {
                // ugly, need custom
                _dialog.setTitle("Downloading...");
                _dialog.setMessage("...from " + _host + ": " + DataHelper.formatSize(prog) + 'B');
                //_dialog.setProgress(prog);
            } else {
                // nothing
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result.equals(Integer.valueOf(1))) {
                Toast toast = Toast.makeText(_view.getContext(), "Loading from cache, click settings to reload", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
            super.onPostExecute(result);
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
