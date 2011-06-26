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
 *  Usage:  content://net.i2p.android.router/foobar.i2p/path/to/blah.html?foo=bar&baz=baf
 *
 *  http://www.techjini.com/blog/2009/01/10/android-tip-1-contentprovider-accessing-local-file-system-from-webview-showing-image-in-webview-using-content/
 */
public class CacheProvider extends ContentProvider {

    /** content:// Uri to file:// Uri */
    private Map<Uri, String> _uriMap;

    public static final Uri CONTENT_URI = Uri.parse("content://net.i2p.android.router");
    /** the database key */
    public static final String DATA = "_data";


    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Util.e("CacheProvider open " + uri);
        // map the resource URI to a local file URI and return it if it exists
        String path = _uriMap.get(uri);
        if (path != null) {
            try {
                File file = new File(path);
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            } catch (FileNotFoundException fnfe) {
            }
        }
        String newURI = uri.getPath();
        if (path == null)
            throw new FileNotFoundException("Bad uri " + uri);
        // convert the encoded path to the new uri
        //load the URL with eepget
        File file = new File(path);
        ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        return parcel;
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
     *  _data -> String file:///path/to/file
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
        // TODO return a MatrixCursor with a _data entry
        return null;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
