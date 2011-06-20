package net.i2p.android.apps;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

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


    public EepGetFetcher(String url) {
        _context = I2PAppContext.getGlobalContext();
        _log = _context.logManager().getLog(EepGetFetcher.class);
        _url = url;
        _file = new File(_context.getTempDir(), "eepget-" + _context.random().nextLong());
        _eepget = new EepGet(_context, true, "localhost", 4444, 0, -1, MAX_LEN,
                             _file.getAbsolutePath(), null, url,
                             true, null, null, null);
        _eepget.addStatusListener(this);
    }
    
    public boolean fetch() {
        _success = _eepget.fetch();
        return _success;
    }

    /**
     *  @return non-null
     */
    public String getContentType() {
        if (!_success)
            return "text/plain";
        String rv = _eepget.getContentType();
        if (rv == null)
            return "text/html";
        return rv;
    }

    /**
     *  @return non-null
     */
    public String getEncoding() {
        String type = _eepget.getContentType();
        String rv;
        if (type == null || !type.startsWith("text"))
            rv = "ISO-8859-1";
        else
            rv = "UTF-8";
        return rv;
    }

    /**
     *  FIXME we don't get the proxy error pages this way
     */
    public String getData() {
        String rv;
        if (!_file.exists()) {
            rv = "Fetch failed";
        } else if (_file.length() <= 0) {
            rv = "Fetch failed";
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
