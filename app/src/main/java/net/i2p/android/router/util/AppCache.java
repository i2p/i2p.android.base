package net.i2p.android.router.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import net.i2p.android.router.provider.CacheProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *  A least recently used cache with a max number of entries
 *  and a max total disk space.
 *  Inserts and deletes entries in the local ContentProvider.
 *
 *  Like Android's CacheManager but usable.
 */
public class AppCache {

    private static AppCache _instance;
    private static File _cacheDir;
    private static long _totalSize;
    private static ContentResolver _resolver;
    /** the LRU cache */
    private final Map<Integer, Object> _cache;

    private static final Integer DUMMY = 0;
    private static final String DIR_NAME = "appCache";
    /** fragment into this many subdirectories */
    private static final int NUM_DIRS = 32;
    private static final int MAX_FILES = 1024;
    /** total used space */
    private static final long MAX_SPACE = 1024 * 1024;
    private static final long MAX_AGE = 12 * 60 * 60 * 1000l;

    public static AppCache getInstance(Context ctx) {
        synchronized (AppCache.class) {
            if (_instance == null)
                _instance = new AppCache(ctx);
        }
        return _instance;
    }

    /**
     *  If you don't have a context. Could return null.
     */
    public static AppCache getInstance() {
        return _instance;
    }

    private AppCache(Context ctx) {
        _cacheDir = new File(ctx.getCacheDir(), DIR_NAME);
        _cacheDir.mkdir();
        Util.d("AppCache cache dir " + _cacheDir);
        _resolver = ctx.getContentResolver();
        _cache = new LHM(MAX_FILES);
        initialize();
    }

    /**
     *  Caller MUST close stream AND call either
     *  addCacheFile() or removeCacheFile() after the data is written.
     *  @param key no fragment allowed
     */
    public OutputStream createCacheFile(Uri key) throws IOException {
        // remove any old file so the total stays correct
        removeCacheFile(key);
        File f = toFile(key);
        f.getParentFile().mkdirs();
        return new FileOutputStream(f);
    }

    /**
     *  Add a previously written file to the cache index.
     *  Return a content:// uri for the cached content in question,
     *  or null on error
     *
     *  @param key no fragment allowed
     *  @param setAsCurrentBase tell CacheProvider
     */
    public Uri addCacheFile(Uri key, boolean setAsCurrentBase) {
        int hash = toHash(key);
        synchronized(_cache) {
            _cache.put(hash, DUMMY);
        }
        // file:/// uri
        //return Uri.fromFile(toFile(hash)).toString();
        // content:// uri
        return insertContent(key, setAsCurrentBase);
    }

    /**
     *  Remove a previously written file from the cache index and disk.
     *  @param key no fragment allowed
     */
    public void removeCacheFile(Uri key) {
        int hash = toHash(key);
        synchronized(_cache) {
            _cache.remove(hash);
        }
        deleteContent(key);
    }

    /**
     *  Return a content:// uri for any cached content in question.
     *  The file may or may not exist, and it may be deleted at any time.
     *  Side effect: If exists, sets as current base
     *
     *  @param key no fragment allowed
     */
    public Uri getCacheUri(Uri key) {
        int hash = toHash(key);
        // poke the LRU
        Object present;
        synchronized(_cache) {
            present = _cache.get(hash);
        }
        if (present != null)
            setAsCurrentBase(key);
        return CacheProvider.getContentUri(key);
    }

    /**
     *  Return an absolute file path for any cached content in question.
     *  The file may or may not exist, and it may be deleted at any time.
     *  @param key no fragment allowed
     */
    public File getCacheFile(Uri key) {
        int hash = toHash(key);
        return toFile(hash);
    }

    ////// private below here

    private void initialize() {
        _totalSize = 0;
        List<File> fileList = new ArrayList<File>(MAX_FILES);
        long total = enumerate(_cacheDir, fileList);
        Util.d("AppCache found " + fileList.size() + " files totalling " + total + " bytes");
        Collections.sort(fileList, new FileComparator());
        // oldest first, delete if too big or too old, else add to LHM
        long now = System.currentTimeMillis();
        for (File f : fileList) {
            if (total > MAX_SPACE || f.lastModified() < now - MAX_AGE) {
                total -= f.length();
                f.delete();
            } else {
                addToCache(f);
                // TODO insertContent
            }
        }
        Util.d("after init " + _cache.size() + " files totalling " + total + " bytes");
    }

    /** oldest first */
    private static class FileComparator implements Comparator<File> {
        public int compare(File l, File r) {
            return (int) (l.lastModified() - r.lastModified());
        }
    }

    /** get all the files, deleting empty ones on the way, returning total size */
    private static long enumerate(File dir, List<File> fileList) {
        long rv = 0;
        File[] files = dir.listFiles();
        if (files == null)
            return 0;
        for (File f : files) {
            if (f.isDirectory()) {
                rv += enumerate(f, fileList);
            } else {
                long len = f.length();
                if (len > 0) {
                    fileList.add(f);
                    rv += len;
                } else {
                    f.delete();
                }
            }
        }
        return rv;
    }

    /** for initialization only */
    private void addToCache(File f) {
        try {
            int hash = toHash(f);
            synchronized(_cache) {
                _cache.put(hash, DUMMY);
            }
        } catch (IllegalArgumentException iae) {
            Util.d("Huh bad file?" + iae);
            f.delete();
        }
    }

    /** for initialization only */
    private static int toHash(File f) throws IllegalArgumentException {
        String path = f.getAbsolutePath();
        int slash = path.lastIndexOf("/");
        String basename = path.substring(slash + 1);
        try {
            return Integer.parseInt(basename);
        } catch (NumberFormatException nfe) {
             throw new IllegalArgumentException("bad file name " + f);
        }
    }

    /**
     *  Just use the hashcode for the hash for now
     *  TODO switch to something secure like SHA-1
     */
    private static int toHash(Uri key) {
        return key.toString().hashCode();
    }

    /**
     *  /path/to/cache/dir/(hashCode(key) % 32)/hashCode(key)
     */
    private static File toFile(Uri key) {
        int hash = toHash(key);
        return toFile(hash);
    }

    private static File toFile(int hash) {
        int dir = hash % NUM_DIRS;
        if (dir < 0)
            dir = 0 - dir;
        return new File(_cacheDir, dir + "/" + hash);
    }

    /**
     *  @return the uri inserted or null on failure
     */
    private static Uri insertContent(Uri key, boolean setAsCurrentBase) {
        String path = toFile(key).getAbsolutePath();
        ContentValues cv = new ContentValues();
        cv.put(CacheProvider.DATA, path);
        if (setAsCurrentBase)
            cv.put(CacheProvider.CURRENT_BASE, Boolean.TRUE);
        Uri uri = CacheProvider.getContentUri(key);
        if (uri != null) {
           _resolver.insert(uri, cv);
           return uri;
        }
        return null;
    }

    /**
     *  Set key as current base. May be content or i2p key.
     */
    private static void setAsCurrentBase(Uri key) {
        ContentValues cv = new ContentValues();
        cv.put(CacheProvider.CURRENT_BASE, Boolean.TRUE);
        Uri uri = CacheProvider.getContentUri(key);
        if (uri != null)
           _resolver.insert(uri, cv);
    }

    /** ok for now but we will need to store key in the map and delete by integer */
    private static void deleteContent(Uri key) {
        Uri uri = CacheProvider.getContentUri(key);
        if (uri != null)
            _resolver.delete(uri, null, null);
    }

    /**
     *  An LRU set of hashcodes, implemented on a HashMap.
     *  We use a dummy for the value to save space, because the
     *  hashcode key is reversable to the file name.
     *  The put and remove methods are overridden to
     *  keep the total size counter updated, and to delete the underlying file
     *  on remove.
     */
    private static class LHM extends LinkedHashMap<Integer, Object> {
        private static final long serialVersionUID = 1L;
        private final int _max;

        public LHM(int max) {
            super(max, 0.75f, true);
            _max = max;
        }

        /** Add the entry, and update the total size */
        @Override
        public Object put(Integer key, Object value) {
            Object rv = super.put(key, value);
            File f = toFile(key);
            if (f.exists()) {
                _totalSize += f.length();
            }
            return rv;
        }

        /** Remove the entry and the file, and update the total size */
        @Override
        public Object remove(Object key) {
            Object rv = super.remove(key);
            if ( /* rv != null && */ key instanceof Integer) {
                File f = toFile((Integer) key);
                if (f.exists()) {
                    _totalSize -= f.length();
                    f.delete();
                    Util.d("AppCache deleted file " + f);
                }
            }
            return rv;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Object> eldest) {
            if (size() > _max || _totalSize > MAX_SPACE) {
                Integer key = eldest.getKey();
                remove(key);
                // TODO deleteContent()
            }
            // we modified the map, we must return false
            return false;
        }
    }
}
