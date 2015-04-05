package net.i2p.android.router.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import net.i2p.android.apps.EepGetFetcher;
import net.i2p.android.router.BuildConfig;
import net.i2p.android.router.util.AppCache;
import net.i2p.android.router.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *  Usage:  content://net.i2p.android.router/NONCE/ENCODED-SCHEME/ENCODED-AUTHORITY/ENCODED_PATH + QUERY_MARKER + ENCODED-QUERY
 *  Where NONCE is set at instantiation
 *  Do not include the fragment.
 *
 *  http://www.techjini.com/blog/2009/01/10/android-tip-1-contentprovider-accessing-local-file-system-from-webview-showing-image-in-webview-using-content/
 *
 * ===================
 *
 *  quote:
 *  http://stackoverflow.com/questions/4616675/how-does-a-contentresolver-locate-the-corresponding-contentprovider
 *
 *  There is no way to see if the ContentProvider is running. It is started and stopped automatically
 *  by ContentResolver as needed. When you start making requests for a specific contentAuthority,
 *  the associated provider will be started if it isn't already running. It will be stopped automatically
 *  by ContentResolver, some time later once it has sat idle and it looks like it might not be needed for a while.
 *
 */
public class CacheProvider extends ContentProvider {

    // FIXME not persistent, use SharedPrefs
    /** content:// Uri to absolute path of the file */
    private SharedPreferences _sharedPrefs;

    private static final String SHARED_PREFS = "net.i2p.android.router.provider.CacheProvider";
    //private static final String NONCE = Integer.toString(Math.abs((new java.util.Random()).nextInt()));
    private static final String NONCE = "0";
    private static final String SCHEME = "content";
    public static final String AUTHORITY = BuildConfig.APPLICATION_ID + ".provider";
    /** includes the nonce */
    public static final Uri CONTENT_URI = Uri.parse(SCHEME + "://" + AUTHORITY + '/' + NONCE);

    /** the database keys */
    public static final String DATA = "_data";
    public static final String CURRENT_BASE = "currentBase";

    private static final String QUERY_MARKER = "!!QUERY!!";

    private static final String ERROR_HEADER = "<html><head><title>Not Found</title></head><body>";
    private static final String ERROR_URL = "<p>Unable to load URL: ";
    private static final String ERROR_ROUTER = "<p>Your router does not appear to be up.</p>";
    private static final String ERROR_FOOTER = "</body></html>";

    /**
     *  Generate a cache content (resource) URI for a given URI key
     *  If the key is already a content URI, canonicalize it
     *  by twizzling the query if necessary
     *
     *  @param key must contain a scheme, authority and path
     *  @return null on error
     */
    public static Uri getContentUri(Uri key) {
        String s = key.getScheme();
        String a = key.getEncodedAuthority();
        String p = key.getEncodedPath();
        if (s == null || a == null || p == null)
            return null;
        String q = key.getEncodedQuery();

        // canonicalize resource URI
        if (s.equals(SCHEME)) {
            if (q == null || !a.equals(AUTHORITY))
                return key;
            if (p.contains(QUERY_MARKER)) {
                Util.d("Key contains both queries ?!? " + key);
                return null;
            }
            // twizzle query
            StringBuilder buf = new StringBuilder(128);
            buf.append(s).append("://")
               .append(a);
            if (!p.startsWith("/"))
                buf.append('/');
            buf.append(p);
            buf.append(QUERY_MARKER).append(q);
            return Uri.parse(buf.toString());
        }

        // convert http URI to resource
        StringBuilder buf = new StringBuilder(128);
        buf.append(CONTENT_URI).append('/')
           .append(s).append('/')
           .append(a);
        if (!p.startsWith("/"))
            buf.append('/');
        buf.append(p);
        if (q != null)
            buf.append(QUERY_MARKER).append(q);
        return Uri.parse(buf.toString());
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Util.d("CacheProvider open " + uri);

        // if uri is malformed and we have a current base, rectify it
        uri = rectifyContentUri(getCurrentBase(), uri);

        // map the resource URI to a local file URI and return it if it exists
        String filePath = get(uri);
        if (filePath != null) {
            try {
                File file = new File(filePath);
                if (file.exists())
                    Util.d("CacheProvider returning " + file);
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            } catch (FileNotFoundException fnfe) {
                Util.d("CacheProvider not found", fnfe);
                remove(uri);
            }
        }
        Util.d("CacheProvider not in cache " + uri);

        Uri newUri = getI2PUri(uri);
        Util.d("CacheProvider fetching: " + newUri);
        return eepFetch(newUri);
    }

    /**
     *  Generate an i2p URI for a content URI
     *
     *  @param uri must contain a scheme, authority and path with nonce etc. as defined above
     *  @return non-null
     *  @throws java.io.FileNotFoundException on error
     */
    public static Uri getI2PUri(Uri uri) throws FileNotFoundException {
        String resPath = uri.getEncodedPath();
        if (resPath == null)
            throw new FileNotFoundException("Bad uri no path? " + uri);
        String[] segs = resPath.split("/", 5);
        // first seg is empty since string starts with /
        String nonce = segs.length > 1 ? segs[1] : null;
        String scheme = segs.length > 2 ? segs[2] : null;
        String host = segs.length > 3 ? segs[3].toLowerCase() : null;
        String realPath = segs.length > 4 ? segs[4] : "";
        String query = uri.getEncodedQuery();
        if (query == null) {
            int marker = realPath.indexOf(QUERY_MARKER);
            if (marker >= 0) {
                query = realPath.substring(marker + QUERY_MARKER.length());
                realPath = realPath.substring(0, marker);
            }
        }
        String debug = "CacheProvider nonce: " + nonce + " scheme: " + scheme + " host: " + host + " realPath: " + realPath + " query: " + query;
        Util.d(debug);
        if ((!NONCE.equals(nonce)) ||
            (!"http".equals(scheme)) ||
            (host == null) ||
            (!host.endsWith(".i2p")))
            throw new FileNotFoundException(debug);
        String i2pUri = scheme + "://" + host + '/' + realPath;
        if (query != null)
            i2pUri += '?' + query;
        return Uri.parse(i2pUri);
    }

    /**
     *  Rectify a malformed content uri using the current base content uri.
     *  Any query in uri is also canonicalized.
     *
     *  @param base a valid content base uri e.g. content://net.i2p.android.router/0/http/bar.i2p/baz/baf.html
     *             if null, uri is returned.
     *  @param uri a malformed content uri e.g. content://net.i2p.android.router/foo.html
     *  @return a valid content uri e.g. content://net.i2p.android.router/0/http/bar.i2p/foo.html,
     *          or the original uri on error, or if no rectification needed
     */
    public static Uri rectifyContentUri(Uri base, Uri uri) {
        Util.d("rectifyContentUri  base: " + base + " and uri: " + uri);
        if (base == null)
            return uri;
        if (!SCHEME.equals(base.getScheme()))
            return uri;
        if (!AUTHORITY.equals(base.getEncodedAuthority()))
            return uri;
        String basePath = base.getEncodedPath();
        if (basePath == null)
            return uri;
        String[] segs = basePath.split("/", 5);
        if (segs.length < 3)
            return uri;
        // first seg is empty since string starts with /
        if (!segs[1].equals(NONCE))
            return uri;
        if (!segs[2].equals("http"))
            return uri;
        String host = segs[3];
        if (!SCHEME.equals(uri.getScheme()))
            return uri;
        if (!AUTHORITY.equals(uri.getEncodedAuthority()))
            return uri;
        String path = uri.getEncodedPath();
        if (path != null && (path.startsWith(NONCE + '/') || path.startsWith('/' + NONCE + '/')))
            return uri;
        String query = uri.getEncodedQuery();
        StringBuilder buf = new StringBuilder(128);
        buf.append(SCHEME).append("://")
           .append(AUTHORITY).append('/')
           .append(NONCE).append("/http/")
           .append(host);
        if (path == null || !path.startsWith("/"))
            buf.append('/');
        if (path != null)
            buf.append(path);
        if (query != null)
            buf.append(QUERY_MARKER).append(query);
        Util.d("rectified from base: " + base + " and uri: " + uri + " to: " + buf);
        return Uri.parse(buf.toString());
    }

    private ParcelFileDescriptor eepFetch(Uri uri) throws FileNotFoundException {
        AppCache cache = AppCache.getInstance(getContext());
        OutputStream out;
        try {
            out = cache.createCacheFile(uri);
        } catch (IOException ioe) {
            throw new FileNotFoundException(ioe.toString());
        }
        // in this constructor we don't use the error output, for now
        EepGetFetcher fetcher = new EepGetFetcher(uri.toString(), out, false);
        boolean success = fetcher.fetch();
        if (success) {
            File file = cache.getCacheFile(uri);
            if (file.length() > 0) {
                // this call will insert it back to us (don't set as current base)
                Uri content = cache.addCacheFile(uri, false);
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            } else {
                 Util.d("CacheProvider Sucess but no data " + uri);
            }
        } else {
            Util.d("CacheProvider Eepget fail " + uri);
        }
        AppCache.getInstance().removeCacheFile(uri);
        throw new FileNotFoundException("eepget fail");
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Util.d("CacheProvider delete " + uri);
        boolean deleted = remove(uri);
        return deleted ? 1 : 0;
    }

    public String getType(Uri uri) {
        Util.d("CacheProvider getType " + uri);
        return "text/html";
    }

    /*
     *  _data -> String absolute path of the file (NOT a file:// URI)
     */
    public Uri insert(Uri uri, ContentValues values) {
        String fileURI = values.getAsString(DATA);
        if (fileURI != null) {
            Util.d("CacheProvider insert " + uri);
            put(uri, fileURI);
        }
        Boolean setAsCurrentBase = values.getAsBoolean(CURRENT_BASE);
        if (setAsCurrentBase != null && setAsCurrentBase) {
            Util.d("CacheProvider set current base " + uri);
            setCurrentBase(uri);
        }
        return uri;
    }

    public boolean onCreate() {
        _sharedPrefs = getContext().getSharedPreferences(SHARED_PREFS, 0);
        cleanup();
        return true;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selecctionArgs, String sortOrder) {
        Util.d("CacheProvider query " + uri);
        // TODO return a MatrixCursor with a _data entry
        return null;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    ///// Map stuff

    private void cleanup() {
        String pfx = CONTENT_URI.toString();
        List<String> toDelete = new ArrayList<>();
        Map<String, ?> map = _sharedPrefs.getAll();
        for (Map.Entry<String, ?> e : map.entrySet()) {
            String path = (String) e.getValue();
            if (!path.startsWith(pfx))
                continue;
            File f = new File(path);
            if (!f.exists())
                toDelete.add(e.getKey());
        }
        if (!toDelete.isEmpty()) {
            SharedPreferences.Editor edit = _sharedPrefs.edit();
            for (String key : toDelete) {
                edit.remove(key);
            }
            edit.commit();
        }
    }

    private String get(Uri uri) {
        return getPref(uri.toString());
    }

    private void put(Uri uri, String fileURI) {
        setPref(uri.toString(), fileURI);
    }

    /** @return may be null */
    private Uri getCurrentBase() {
        String url = getPref(CURRENT_BASE);
        if (url != null)
           return Uri.parse(url);
        return null;
    }

    private void setCurrentBase(Uri contentURI) {
        setPref(CURRENT_BASE, contentURI.toString());
    }

    /** @return true if it was removed */
    private boolean remove(Uri uri) {
        String old = getPref(uri.toString());
        boolean success = deletePref(uri.toString());
        return success && old != null;
    }

    /** @return null if not found */
    private String getPref(String pref) {
        return _sharedPrefs.getString(pref, null);
    }

    /** @return success */
    private boolean setPref(String pref, String val) {
        SharedPreferences.Editor edit = _sharedPrefs.edit();
        edit.putString(pref, val);
        return edit.commit();
    }

    /** @return success */
    private boolean deletePref(String pref) {
        SharedPreferences.Editor edit = _sharedPrefs.edit();
        edit.remove(pref);
        return edit.commit();
    }

}
