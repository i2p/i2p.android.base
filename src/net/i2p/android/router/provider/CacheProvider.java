package net.i2p.android.router.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.i2p.android.router.util.Util;

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
    private Map<Uri, String> _uriMap;

    private static final String NONCE = Integer.toString(Math.abs((new java.util.Random()).nextInt()));
    /** includes the nonce */
    public static final Uri CONTENT_URI = Uri.parse("content://net.i2p.android.router/" + NONCE);
    /** the database key */
    public static final String DATA = "_data";
    private static final String QUERY_MARKER = "!!QUERY!!";

    /**
     *  Generate a cache content URI for a given URI key
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
        Util.e("CacheProvider open " + uri);
        // map the resource URI to a local file URI and return it if it exists
        String filePath = _uriMap.get(uri);
        if (filePath != null) {
            try {
                File file = new File(filePath);
                if (file.exists())
                    Util.e("CacheProvider returning " + file);
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            } catch (FileNotFoundException fnfe) {
                Util.e("CacheProvider not found", fnfe);
                _uriMap.remove(uri);
            }
        }
        Util.e("CacheProvider not in cache " + uri);
        String resPath = uri.getEncodedPath();
        if (resPath == null)
            throw new FileNotFoundException("Bad uri no path? " + uri);
        String[] segs = resPath.split("/", 5);
        // first seg is empty since string starts with /
        String nonce = segs.length > 1 ? segs[1] : "unset";
        String scheme = segs.length > 2 ? segs[2] : "unset";
        String host = segs.length > 3 ? segs[3] : "unset";
        String realPath = segs.length > 4 ? segs[4] : "unset";
        String query = uri.getEncodedQuery();
        if (query == null) {
            int marker = realPath.indexOf(QUERY_MARKER);
            if (marker >= 0) {
                realPath = realPath.substring(0, marker);
                query = realPath.substring(marker + QUERY_MARKER.length());
            } else {
                query = "unset";
            }
        }
        String debug = "Here is where we fetch: nonce: " + nonce + "scheme: " + scheme + " host: " + host + " realPath: " + realPath + " query: " + query;
        Util.e(debug);
        // convert the encoded path to the new uri
        //load the URL with eepget
      /**
        File file = new File(path);
        ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        return parcel;
       **/
        throw new FileNotFoundException(debug);
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Util.e("CacheProvider delete " + uri);
        String deleted = _uriMap.remove(uri);
        return deleted != null ? 1 : 0;
    }

    public String getType(Uri uri) {
        return "text/html";
    }

    /*
     *  _data -> String absolute path of the file (NOT a file:// URI)
     */
    public Uri insert(Uri uri, ContentValues values) {
        Util.e("CacheProvider insert " + uri);
        String fileURI = values.getAsString(DATA);
        if (fileURI != null)
            _uriMap.put(uri, fileURI);
        return uri;
    }

    public boolean onCreate() {
        _uriMap = new ConcurrentHashMap();
        return true;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selecctionArgs, String sortOrder) {
        Util.e("CacheProvider query " + uri);
        // TODO return a MatrixCursor with a _data entry
        return null;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
