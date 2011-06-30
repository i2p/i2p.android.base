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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.i2p.android.apps.EepGetFetcher;
import net.i2p.android.router.provider.CacheProvider;
import net.i2p.android.router.util.AppCache;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataHelper;
import net.i2p.util.EepGet;

class I2PWebViewClient extends WebViewClient {

    private BGLoad _lastTask;

    // TODO add some inline style
    private static final String CONTENT = "content";
    private static final String HEADER = "<html><head></head><body>";
    private static final String FOOTER = "</body></html>";
    private static final String ERROR_URL = "<p>Unable to load URL: ";
    private static final String ERROR_ROUTER = "<p>Your router does not appear to be up.</p>";

    public I2PWebViewClient(Context ctx) {
        super();
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Util.e("Should override? " + url);
        view.stopLoading();

            Uri uri = Uri.parse(url);
            if (CONTENT.equals(uri.getScheme())) {
                try {
                    //reverse back to a i2p URI so we can load it here and not in ContentProvider
                    uri = CacheProvider.getI2PUri(uri);
                    url = uri.toString();
                    Util.e("Reversed content uri back to " + url);
                    AppCache.getInstance(view.getContext()).removeCacheFile(uri);
                } catch (FileNotFoundException fnfe) {}
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
                BGLoad task = new BackgroundEepLoad(view, h);
                _lastTask = task;
                task.execute(url);
            } else {
                if (s.equals(CONTENT)) {
                    if (h.equals(CacheProvider.AUTHORITY)) {
                        if (!url.startsWith(CacheProvider.CONTENT_URI.toString()))
                            Util.e("Content URI bad nonce, FIXME: " + url);
                    } else {
                        Util.e("Content URI but not for us?? " + url);
                    }

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
                    Uri content = AppCache.getInstance(_view.getContext()).addCacheFile(uri);
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
