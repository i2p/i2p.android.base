package net.i2p.android.apps;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.i2p.I2PAppContext;
import net.i2p.data.DataHelper;
import net.i2p.util.EepGet;
import net.i2p.util.Log;

/**
 *  EepGet and return as a string. 256KB max.
 */
public class EepGetFetcher implements EepGet.StatusListener {

    private final I2PAppContext _context;
    private final Log _log;
    private final String _url;
    private final EepGet _eepget;
    private final File _file;
    private boolean _success;

    private static final long MAX_LEN = 256*1024;

    private static final String ERROR_HEADER = "<html><head><title>Not Found</title></head><body>";
    private static final String ERROR_URL = "<p>Unable to load URL: ";
    private static final String ERROR_ROUTER = "<p>Your router (or the HTTP proxy) does not appear to be running.</p>";
    private static final String ERROR_FOOTER = "</body></html>";

    /**
     *  Writes to temp file, call getData()
     *  to get the data as a String.
     *  Temp file sticks around forever.
     */
    public EepGetFetcher(String url) {
        _context = I2PAppContext.getGlobalContext();
        _log = _context.logManager().getLog(EepGetFetcher.class);
        _url = url;
        _file = new File(_context.getTempDir(), "eepget-" + _context.random().nextLong());
        _eepget = new EepGet(_context, true, "localhost", 4444, 0, -1, MAX_LEN,
                             _file.getAbsolutePath(), null, url,
                             true, null, null, null);
        // use new 0.8.8 feature
        _eepget.setWriteErrorToOutput();
        //_eepget.addStatusListener(this);
    }

    /**
     *  Writes to output stream
     */
    public EepGetFetcher(String url, OutputStream out, boolean writeErrorToStream) {
        _context = I2PAppContext.getGlobalContext();
        _log = _context.logManager().getLog(EepGetFetcher.class);
        _url = url;
        _file = null;
        _eepget = new EepGet(_context, true, "localhost", 4444, 0, -1, MAX_LEN,
                             null, out, url,
                             true, null, null, null);
        if (writeErrorToStream)
            _eepget.setWriteErrorToOutput();
    }

    public void addStatusListener(EepGet.StatusListener l) {
        _eepget.addStatusListener(l);
    }

    public boolean fetch() {
        _success = _eepget.fetch();
        return _success;
    }

    /**
     *  @return non-null
     */
    public String getContentType() {
        if (_eepget.getStatusCode() < 0)
            return "text/html";
        String rv = _eepget.getContentType();
        if (rv == null)
            return "text/html";
        int semi = rv.indexOf(";");
        if (semi > 0)
            rv = rv.substring(0, semi);
        return rv.toLowerCase();
    }

    /**
     *  @return non-null
     */
    public String getEncoding() {
        if (_eepget.getStatusCode() < 0)
            return "UTF-8";
        String type = _eepget.getContentType();
        String rv;
        if (type == null || !type.startsWith("text"))
            rv = "ISO-8859-1";
        else
            rv = "UTF-8";
        return rv;
    }

    /**
     *  @return -1 if nothing back from server
     */
    public int getStatusCode() {
        return _eepget.getStatusCode();
    }

    /**
     *  Only for the constructor without the output stream
     *  Only call ONCE!
     *  FIXME we don't get the proxy error pages this way
     */
    public String getData() {
        String rv;
        int statusCode = _eepget.getStatusCode();
        if (statusCode < 0) {
            rv = ERROR_HEADER + ERROR_URL + "<a href=\"" + _url + "\">" + _url +
                 "</a></p>" + ERROR_ROUTER + ERROR_FOOTER;
            _file.delete();
        } else if (_file.length() <= 0) {
            rv = ERROR_HEADER + ERROR_URL + "<a href=\"" + _url + "\">" + _url +
                 "</a> No data returned, error code: " + statusCode +
                 "</p>" + ERROR_FOOTER;
            _file.delete();
        } else {
            InputStream fis = null;
            try {
                fis = new FileInputStream(_file);
                byte[] data = new byte[(int) _file.length()];
                DataHelper.read(fis, data);
                rv = new String(data, getEncoding());
            } catch (IOException ioe) {
                _log.error("fetcher", ioe);
                rv = "I/O error";
            } finally {
                if (fis != null) try { fis.close(); } catch (IOException ioe) {}
                _file.delete();
            }
        }
        return rv;
    }

    public void attemptFailed(String url, long bytesTransferred, long bytesRemaining, int currentAttempt, int numRetries, Exception cause) {}

    public void bytesTransferred(long alreadyTransferred, int currentWrite, long bytesTransferred, long bytesRemaining, String url) {}

    public void transferComplete(long alreadyTransferred, long bytesTransferred, long bytesRemaining, String url, String outputFile, boolean notModified) {}

    public void transferFailed(String url, long bytesTransferred, long bytesRemaining, int currentAttempt) {}

    public void headerReceived(String url, int attemptNum, String key, String val) {}

    public void attempting(String url) {}
}
