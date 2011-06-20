package net.i2p.android.router.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;

import net.i2p.android.apps.EepGetFetcher;
import net.i2p.util.EepGet;

class I2PWebViewClient extends WebViewClient {

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
                Toast toast = Toast.makeText(view.getContext(), "Bad URL " + url, Toast.LENGTH_SHORT);
                return true;
            }
            s = s.toLowerCase();
            if (!(s.equals("http") || s.equals("https")))
                return false;
            String h = uri.getHost();
            if (h == null) {
                Toast toast = Toast.makeText(view.getContext(), "Bad URL " + url, Toast.LENGTH_SHORT);
                return true;
            }

            h = h.toLowerCase();
            if (h.endsWith(".i2p")) {
                // if (s.equals("https")
                //    return false;
                view.getSettings().setLoadsImagesAutomatically(false);
                ///////// API 8
                // Otherwise hangs waiting for CSS
                view.getSettings().setBlockNetworkLoads(true);
                //view.loadData(ERROR_EEPSITE, "text/html", "UTF-8");
                (new BackgroundEepLoad(view, h)).execute(url);
            } else {
                view.getSettings().setLoadsImagesAutomatically(true);
                ///////// API 8
                view.getSettings().setBlockNetworkLoads(false);
                //view.loadUrl(url);
                (new BackgroundLoad(view)).execute(url);
            }
            return true;
        } catch (URISyntaxException use) {
            return false;
        }
    }

    private static class BackgroundLoad extends AsyncTask<String, Integer, Integer> {
        private final WebView _view;
        private ProgressDialog _dialog;

        public BackgroundLoad(WebView view) {
            _view = view;
        }

        protected Integer doInBackground(String... urls) {
            publishProgress(Integer.valueOf(-1));
            _view.loadUrl(urls[0]);
            return Integer.valueOf(0);
        }

        protected void onProgressUpdate(Integer... progress) {
            if (progress[0].intValue() < 0) {
                _dialog = ProgressDialog.show(_view.getContext(), "Loading", "some url");
                _dialog.setCancelable(true);
            }
        }

        protected void onPostExecute(Integer result) {
            if (_dialog != null)
                _dialog.dismiss();
        }
    }

    // http://stackoverflow.com/questions/3961589/android-webview-and-loaddata
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";

    private static class BackgroundEepLoad extends AsyncTask<String, Integer, Integer> implements EepGet.StatusListener {
        private final WebView _view;
        private final String _host;
        private ProgressDialog _dialog;
        private int _total;
        private String _data;

        public BackgroundEepLoad(WebView view, String host) {
            _view = view;
            _host = host;
        }

        protected Integer doInBackground(String... urls) {
            String url = urls[0];
            publishProgress(Integer.valueOf(-1));
            EepGetFetcher fetcher = new EepGetFetcher(url);
            fetcher.addStatusListener(this);
            boolean success = fetcher.fetch();
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
            _view.loadDataWithBaseURL(url, d, t, e, url);
            return Integer.valueOf(0);
        }

        protected void onProgressUpdate(Integer... progress) {
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

        protected void onPostExecute(Integer result) {
            if (_dialog != null)
                _dialog.dismiss();
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
