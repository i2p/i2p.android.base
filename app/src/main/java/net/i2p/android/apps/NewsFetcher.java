package net.i2p.android.apps;

import android.content.Context;

import net.i2p.android.router.NewsActivity;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Notifications;
import net.i2p.crypto.SU3File;
import net.i2p.data.DataHelper;
import net.i2p.router.RouterContext;
import net.i2p.router.news.NewsEntry;
import net.i2p.router.news.NewsMetadata;
import net.i2p.router.news.NewsXMLParser;
import net.i2p.util.EepGet;
import net.i2p.util.FileUtil;
import net.i2p.util.Log;
import net.i2p.util.ReusableGZIPInputStream;
import net.i2p.util.SecureFileOutputStream;
import net.i2p.util.RFC822Date;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.List;

/**
 * From router console, simplified since we don't deal with router versions
 * or updates.
 */
public class NewsFetcher implements Runnable, EepGet.StatusListener {
    private final Context mCtx;
    private final RouterContext _context;
    private final Notifications _notif;
    private final Log _log;
    private long _lastFetch;
    private long _lastUpdated;
    private String _lastModified;
    private boolean _invalidated;
    private File _newsFile;
    private File _tempFile;
    private static NewsFetcher _instance;
    private volatile boolean _isRunning = true;
    private Thread _thread;

    public static /*final */ NewsFetcher getInstance() {
        return _instance;
    }

    public static /* final */ synchronized NewsFetcher getInstance(
            Context context, RouterContext ctx, Notifications notif) {
        if (_instance != null)
            return _instance;
        _instance = new NewsFetcher(context, ctx, notif);
        return _instance;
    }

    private static final String NEWS_DIR = "docs";
    private static final String NEWS_FILE = "news.xml";
    private static final String TEMP_NEWS_FILE = "news.xml.temp";

    /**
     * Changed in 0.9.11 to the b32 for psi.i2p, run by psi.
     * We may be able to change it to psi.i2p in a future release after
     * the hostname propagates.
     *
     * @since 0.7.14 not configurable
     */
    private static final String BACKUP_NEWS_URL_SU3 = "http://avviiexdngd32ccoy4kuckvc3mkf53ycvzbz6vz75vzhv4tbpk5a.b32.i2p/news.su3";
    private static final String PROP_LAST_CHECKED = "router.newsLastChecked";
    private static final String PROP_REFRESH_FREQUENCY = "router.newsRefreshFrequency";
    private static final String DEFAULT_REFRESH_FREQUENCY = 24 * 60 * 60 * 1000 + "";
    private static final String PROP_NEWS_URL = "router.newsURL";
    public static final String DEFAULT_NEWS_URL_SU3 = "http://echelon.i2p/news/news.su3";

    private NewsFetcher(Context context, RouterContext ctx, Notifications notif) {
        mCtx = context;
        _context = ctx;
        _notif = notif;
        _context.addShutdownTask(new Shutdown());
        _log = ctx.logManager().getLog(NewsFetcher.class);
        try {
            String last = ctx.getProperty(PROP_LAST_CHECKED);
            if (last != null)
                _lastFetch = Long.parseLong(last);
        } catch (NumberFormatException nfe) {
        }
        File newsDir = new File(_context.getRouterDir(), NEWS_DIR);
        // isn't already there on android
        newsDir.mkdir();
        _newsFile = new File(newsDir, NEWS_FILE);
        _tempFile = new File(_context.getTempDir(), TEMP_NEWS_FILE);
        updateLastFetched();
    }

    private void updateLastFetched() {
        if (_newsFile.exists()) {
            if (_lastUpdated == 0)
                _lastUpdated = _newsFile.lastModified();
            if (_lastFetch == 0)
                _lastFetch = _lastUpdated;
            if (_lastModified == null)
                _lastModified = RFC822Date.to822Date(_lastFetch);
        } else {
            _lastUpdated = 0;
            _lastFetch = 0;
            _lastModified = null;
        }
    }

    public String status() {
        StringBuilder buf = new StringBuilder(128);
        long now = _context.clock().now();
        if (_lastUpdated > 0) {
            buf.append(mCtx.getString(R.string.news_last_updated,
                    DataHelper.formatDuration2(now - _lastUpdated)))
                    .append('\n');
        }
        if (_lastFetch > _lastUpdated) {
            buf.append(mCtx.getString(R.string.news_last_checked,
                    DataHelper.formatDuration2(now - _lastFetch)));
        }
        return buf.toString();
    }

    // Runnable

    private static final long INITIAL_DELAY = 5 * 60 * 1000;
    private static final long RUN_DELAY = 30 * 60 * 1000;

    public void run() {
        _thread = Thread.currentThread();
        try {
            Thread.sleep(INITIAL_DELAY);
        } catch (InterruptedException ie) {
            return;
        }
        while (_isRunning && _context.router().isAlive()) {
            if (shouldFetchNews()) {
                fetchNews();
            }
            try {
                Thread.sleep(RUN_DELAY);
            } catch (InterruptedException ie) {
                break;
            }
        }
    }

    private boolean shouldFetchNews() {
        if (_invalidated)
            return true;
        updateLastFetched();
        String freq = _context.getProperty(PROP_REFRESH_FREQUENCY,
                DEFAULT_REFRESH_FREQUENCY);
        try {
            long ms = Long.parseLong(freq);
            if (ms <= 0)
                return false;

            if (_lastFetch + ms < _context.clock().now()) {
                return true;
            } else {
                if (_log.shouldLog(Log.DEBUG))
                    _log.debug("Last fetched " + DataHelper.formatDuration(_context.clock().now() - _lastFetch) + " ago");
                return false;
            }
        } catch (NumberFormatException nfe) {
            if (_log.shouldLog(Log.ERROR))
                _log.error("Invalid refresh frequency: " + freq);
            return false;
        }
    }

    /**
     * Call this when changing news URLs to force an update next time the timer fires.
     *
     * @since 0.8.7
     */
    void invalidateNews() {
        _lastModified = null;
        _invalidated = true;
    }

    public void fetchNews() {
        String newsURL = _context.getProperty(PROP_NEWS_URL, DEFAULT_NEWS_URL_SU3);
        String proxyHost = "127.0.0.1";
        int proxyPort = 4444;
        if (_tempFile.exists())
            _tempFile.delete();

        try {
            // EepGet get = null;
            EepGet get = new EepGet(_context, true, proxyHost, proxyPort, 0, _tempFile.getAbsolutePath(), newsURL, true, null, _lastModified);
            get.addStatusListener(this);
            if (get.fetch()) {
                _lastModified = get.getLastModified();
                _invalidated = false;
            } else {
                // backup news location - always proxied
                _tempFile.delete();
                get = new EepGet(_context, true, proxyHost, proxyPort, 0, _tempFile.getAbsolutePath(), BACKUP_NEWS_URL_SU3, true, null, _lastModified);
                get.addStatusListener(this);
                if (get.fetch())
                    _lastModified = get.getLastModified();
            }
        } catch (Throwable t) {
            _log.error("Error fetching the news", t);
        }
    }

    // EepGet.StatusListener

    public void bytesTransferred(long alreadyTransferred, int currentWrite, long bytesTransferred, long bytesRemaining, String url) {
        // ignore
    }

    public void transferComplete(long alreadyTransferred, long bytesTransferred, long bytesRemaining, String url, String outputFile, boolean notModified) {
        if (_log.shouldLog(Log.INFO))
            _log.info("News fetched from " + url + " with " + (alreadyTransferred + bytesTransferred));

        long now = _context.clock().now();
        if (_tempFile.exists() && _tempFile.length() > 0) {
            File from;
            if (url.endsWith(".su3")) {
                try {
                    from = processSU3();
                } catch (IOException ioe) {
                    _log.error("Failed to extract the news file", ioe);
                    _tempFile.delete();
                    return;
                }
            } else {
                from = _tempFile;
            }
            boolean copied = FileUtil.rename(from, _newsFile);
            _tempFile.delete();
            if (copied) {
                _lastUpdated = now;

                // Notify user
                _notif.notify(mCtx.getString(R.string.news_updated),
                        mCtx.getString(R.string.view_news),
                        NewsActivity.class);
            } else {
                if (_log.shouldLog(Log.ERROR))
                    _log.error("Failed to copy the news file!");
            }
        } else {
            if (_log.shouldLog(Log.WARN))
                _log.warn("Transfer complete, but no file? - probably 304 Not Modified");
        }
        _lastFetch = now;
        _context.router().setConfigSetting(PROP_LAST_CHECKED, "" + now);
        _context.router().saveConfig();
    }

    public void attemptFailed(String url, long bytesTransferred, long bytesRemaining, int currentAttempt, int numRetries, Exception cause) {
        // ignore
    }

    public void transferFailed(String url, long bytesTransferred, long bytesRemaining, int currentAttempt) {
        if (_log.shouldLog(Log.WARN))
            _log.warn("Failed to fetch the news from " + url);
        _tempFile.delete();
    }

    public void headerReceived(String url, int attemptNum, String key, String val) {
    }

    public void attempting(String url) {
    }

    private class Shutdown implements Runnable {
        public void run() {
            _isRunning = false;
            if (_thread != null)
                _thread.interrupt();
        }
    }

    //
    // SU3 handlers
    //

    /**
     *  Process the fetched su3 news file _tempFile.
     *  Handles 3 types of contained files: xml.gz (preferred), xml, and html (old format fake xml)
     *
     *  @return the temp file contining the HTML-format news.xml
     *  @since 0.9.20
     */
    private File processSU3() throws IOException {
        SU3File su3 = new SU3File(_context, _tempFile);
        // real xml, maybe gz, maybe not
        File to1 = new File(_context.getTempDir(), "tmp-" + _context.random().nextInt() + ".xml");
        // real xml
        File to2 = new File(_context.getTempDir(), "tmp2-" + _context.random().nextInt() + ".xml");
        try {
            su3.verifyAndMigrate(to1);
            int type = su3.getFileType();
            if (su3.getContentType() != SU3File.CONTENT_NEWS)
                throw new IOException("bad content type: " + su3.getContentType());
            if (type == SU3File.TYPE_HTML)
                return to1;
            if (type != SU3File.TYPE_XML && type != SU3File.TYPE_XML_GZ)
                throw new IOException("bad file type: " + type);
            File xml;
            if (type == SU3File.TYPE_XML_GZ) {
                gunzip(to1, to2);
                xml = to2;
                to1.delete();
            } else {
                xml = to1;
            }
            NewsXMLParser parser = new NewsXMLParser(_context);
            parser.parse(xml);
            xml.delete();
            NewsMetadata data = parser.getMetadata();
            List<NewsEntry> entries = parser.getEntries();
            String sudVersion = su3.getVersionString();
            String signingKeyName = su3.getSignerString();
            File to3 = new File(_context.getTempDir(), "tmp3-" + _context.random().nextInt() + ".xml");
            outputOldNewsXML(data, entries, sudVersion, signingKeyName, to3);
            return to3;
        } finally {
            to2.delete();
        }
    }

    /**
     *  Gunzip the file
     *
     *  @since 0.9.20
     */
    private static void gunzip(File from, File to) throws IOException {
        ReusableGZIPInputStream in = ReusableGZIPInputStream.acquire();
        OutputStream out = null;
        try {
            in.initialize(new FileInputStream(from));
            out = new SecureFileOutputStream(to);
            byte buf[] = new byte[4096];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
        } finally {
            if (out != null) try {
                out.close();
            } catch (IOException ioe) {}
            ReusableGZIPInputStream.release(in);
        }
    }

    /**
     *  Output in the old format.
     *
     *  @since 0.9.20
     */
    private void outputOldNewsXML(NewsMetadata data, List<NewsEntry> entries,
                                  String sudVersion, String signingKeyName, File to) throws IOException {
        NewsMetadata.Release latestRelease = data.releases.get(0);
        Writer out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(to), "UTF-8"));
            out.write("<!--\n");
            // update metadata in old format
            out.write("<i2p.release ");
            if (latestRelease.i2pVersion != null)
                out.write(" version=\"" + latestRelease.i2pVersion + '"');
            if (latestRelease.minVersion != null)
                out.write(" minVersion=\"" + latestRelease.minVersion + '"');
            if (latestRelease.minJavaVersion != null)
                out.write(" minJavaVersion=\"" + latestRelease.minJavaVersion + '"');
            String su3Torrent = "";
            String su2Torrent = "";
            for (NewsMetadata.Update update : latestRelease.updates) {
                if (update.torrent != null) {
                    if ("su3".equals(update.type))
                        su3Torrent = update.torrent;
                    else if ("su2".equals(update.type))
                        su2Torrent = update.torrent;
                }
            }
            if (!su2Torrent.isEmpty())
                out.write(" su2Torrent=\"" + su2Torrent + '"');
            if (!su3Torrent.isEmpty())
                out.write(" su3Torrent=\"" + su3Torrent + '"');
            out.write("/>\n");
            // su3 and feed metadata for debugging
            out.write("** News version:\t" + DataHelper.stripHTML(sudVersion) + '\n');
            out.write("** Signed by:\t" + signingKeyName + '\n');
            out.write("** Feed:\t" + DataHelper.stripHTML(data.feedTitle) + '\n');
            out.write("** Feed ID:\t" + DataHelper.stripHTML(data.feedID) + '\n');
            out.write("** Feed Date:\t" + (new Date(data.feedUpdated)) + '\n');
            out.write("-->\n");
            if (entries == null)
                return;
            for (NewsEntry e : entries) {
                if (e.title == null || e.content == null)
                    continue;
                out.write("<!-- Entry Date: " + (new Date(e.updated)) + " -->\n");
                out.write("<h3>");
                out.write(e.title);
                out.write("</h3>\n");
                out.write(e.content);
                out.write("\n\n");
            }
        } finally {
            if (out != null) try {
                out.close();
            } catch (IOException ioe) {}
        }
    }
}
