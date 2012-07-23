package net.i2p.android.apps;

import java.io.File;
import net.i2p.data.DataHelper;
import net.i2p.router.RouterContext;
import net.i2p.router.util.RFC822Date;
import net.i2p.util.EepGet;
import net.i2p.util.FileUtil;
import net.i2p.util.Log;
import net.i2p.util.Translate;

/**
 * From router console, simplified since we don't deal with router versions
 * or updates.
 */
public class NewsFetcher implements Runnable, EepGet.StatusListener {
    private final RouterContext _context;
    private final Log _log;
    private long _lastFetch;
    private long _lastUpdated;
    private String _lastModified;
    private boolean _invalidated;
    private File _newsFile;
    private File _tempFile;
    private static NewsFetcher _instance;

    public static final NewsFetcher getInstance() { 
        return _instance;
    }

    public static final synchronized NewsFetcher getInstance(RouterContext ctx) { 
        if (_instance != null)
            return _instance;
        _instance = new NewsFetcher(ctx);
        return _instance;
    }

    private static final String NEWS_DIR = "docs";
    private static final String NEWS_FILE = "news.xml";
    private static final String TEMP_NEWS_FILE = "news.xml.temp";
    /** @since 0.7.14 not configurable */
    private static final String BACKUP_NEWS_URL = "http://www.i2p2.i2p/_static/news/news.xml";
    private static final String PROP_LAST_CHECKED = "router.newsLastChecked";
    private static final String PROP_REFRESH_FREQUENCY = "router.newsRefreshFrequency";
    private static final String DEFAULT_REFRESH_FREQUENCY = 24*60*60*1000 + "";
    private static final String PROP_NEWS_URL = "router.newsURL";
    private static final String DEFAULT_NEWS_URL = "http://echelon.i2p/i2p/news.xml";
    
    private NewsFetcher(RouterContext ctx) {
        _context = ctx;
        _log = ctx.logManager().getLog(NewsFetcher.class);
        try {
            String last = ctx.getProperty(PROP_LAST_CHECKED);
            if (last != null)
                _lastFetch = Long.parseLong(last);
        } catch (NumberFormatException nfe) {}
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
             buf.append(Translate.getString("News last updated {0} ago.",
                                           DataHelper.formatDuration2(now - _lastUpdated),
                                           _context, "foo"))
                .append('\n');
         }
         if (_lastFetch > _lastUpdated) {
             buf.append(Translate.getString("News last checked {0} ago.",
                                           DataHelper.formatDuration2(now - _lastFetch),
                                           _context, "foo"));
         }
         return buf.toString();
    }
    
    private static final long INITIAL_DELAY = 5*60*1000;
    private static final long RUN_DELAY = 30*60*1000;

    @SuppressWarnings("SleepWhileInLoop")
    public void run() {
        try {
            Thread.sleep(INITIAL_DELAY);
        } catch (InterruptedException ie) {
            return;
        }
        while (true) {
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
     *  Call this when changing news URLs to force an update next time the timer fires.
     *  @since 0.8.7
     */
    void invalidateNews() {
        _lastModified = null;
        _invalidated = true;
    }

    public void fetchNews() {
        String newsURL = _context.getProperty(PROP_NEWS_URL, DEFAULT_NEWS_URL);
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
                get = new EepGet(_context, true, proxyHost, proxyPort, 0, _tempFile.getAbsolutePath(), BACKUP_NEWS_URL, true, null, _lastModified);
                get.addStatusListener(this);
                if (get.fetch())
                    _lastModified = get.getLastModified();
            }
        } catch (Throwable t) {
            _log.error("Error fetching the news", t);
        }
    }
    
    public void attemptFailed(String url, long bytesTransferred, long bytesRemaining, int currentAttempt, int numRetries, Exception cause) {
        // ignore
    }
    public void bytesTransferred(long alreadyTransferred, int currentWrite, long bytesTransferred, long bytesRemaining, String url) {
        // ignore
    }
    public void transferComplete(long alreadyTransferred, long bytesTransferred, long bytesRemaining, String url, String outputFile, boolean notModified) {
        if (_log.shouldLog(Log.INFO))
            _log.info("News fetched from " + url + " with " + (alreadyTransferred+bytesTransferred));
        
        long now = _context.clock().now();
        if (_tempFile.exists()) {
            boolean copied = FileUtil.copy(_tempFile.getAbsolutePath(), _newsFile.getAbsolutePath(), true);
            if (copied) {
                _lastUpdated = now;
                _tempFile.delete();
                // notify somebody?
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
    
    public void transferFailed(String url, long bytesTransferred, long bytesRemaining, int currentAttempt) {
        if (_log.shouldLog(Log.WARN))
            _log.warn("Failed to fetch the news from " + url);
        _tempFile.delete();
    }
    public void headerReceived(String url, int attemptNum, String key, String val) {}
    public void attempting(String url) {}
}
