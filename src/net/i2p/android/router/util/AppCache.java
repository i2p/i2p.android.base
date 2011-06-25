package net.i2p.android.router.util;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
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
 *
 *  Like Android's CacheManager but usable.
 */
public class AppCache {

    private static AppCache _instance;
    private static File _cacheDir;
    private static long _totalSize;
    /** the LRU cache */
    private final Map<Integer, Object> _cache;

    private static final Integer DUMMY = Integer.valueOf(0);
    private static final String DIR_NAME = "appCache";
    /** fragment into this many subdirectories */
    private static final int NUM_DIRS = 32;
    private static final int MAX_FILES = 1024;
    /** total used space */
    private static final long MAX_SPACE = 1024 * 1024;


    public static AppCache getInstance(Context ctx) {
        synchronized (AppCache.class) {
            if (_instance == null)
                _instance = new AppCache(ctx);
        }
        return _instance;
    }

    private AppCache(Context ctx) {
        _cacheDir = new File(ctx.getCacheDir(), DIR_NAME);
        _cacheDir.mkdir();
        Util.e("AppCache cache dir " + _cacheDir);
        _cache = new LHM(MAX_FILES);
        initialize();
    }

    /**
     *  Caller MUST close stream AND call either
     *  addCacheFile() or removeCacheFile() after the data is written.
     */
    public OutputStream createCacheFile(String key) throws IOException {
        // remove any old file so the total stays correct
        removeCacheFile(key);
        File f = toFile(key);
        f.getParentFile().mkdirs();
        return new FileOutputStream(f);
    }

    /**
     *  Add a previously written file to the cache.
     *  Return a file:/// uri for the cached content in question.
     */
    public String addCacheFile(String key) {
        int hash = toHash(key);
        synchronized(_cache) {
            _cache.put(Integer.valueOf(hash), DUMMY);
        }
        return Uri.fromFile(toFile(hash)).toString();
    }

    /**
     *  Remove a previously written file from the cache.
     */
    public void removeCacheFile(String key) {
        int hash = toHash(key);
        synchronized(_cache) {
            _cache.remove(Integer.valueOf(hash));
        }
    }

    /**
     *  Return a file:/// uri for any cached content in question.
     *  The file may or may not exist, and it may be deleted at any time.
     */
    public String getCacheFile(String key) {
        int hash = toHash(key);
        // poke the LRU
        synchronized(_cache) {
            _cache.get(Integer.valueOf(hash));
        }
        return Uri.fromFile(toFile(hash)).toString();
    }

    ////// private below here

    private void initialize() {
        _totalSize = 0;
        List<File> fileList = new ArrayList(MAX_FILES);
        long total = enumerate(_cacheDir, fileList);
        Util.e("AppCache found " + fileList.size() + " files totalling " + total + " bytes");
        Collections.sort(fileList, new FileComparator());
        // oldest first, delete if too big else add to LHM
        for (File f : fileList) {
            if (total > MAX_SPACE) {
                total -= f.length();
                f.delete();
            } else {
                addToCache(f);
            }
        }
        Util.e("after init " + _cache.size() + " files totalling " + total + " bytes");
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
        for (int i = 0; i < files.length; i++) {
             File f = files[i];
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
                _cache.put(Integer.valueOf(hash), DUMMY);
            }
        } catch (IllegalArgumentException iae) {
            f.delete();
        }
    }

    /** for initialization only */
    private static int toHash(File f) throws IllegalArgumentException {
        String path = f.getAbsolutePath();
        int slash = path.lastIndexOf("/");
        String basename = path.substring(slash);
        try {
            return Integer.parseInt(basename);
        } catch (NumberFormatException nfe) {
             throw new IllegalArgumentException("bad file name");
        }
    }

    /** just use the hashcode for the hash */
    private static int toHash(String key) {
        return key.hashCode();
    }

    /**
     *  /path/to/cache/dir/(hashCode(key) % 32)/hashCode(key)
     */
    private static File toFile(String key) {
        int hash = toHash(key);
        return toFile(hash);
    }

    private static File toFile(int hash) {
        int dir = hash % NUM_DIRS;
        return new File(_cacheDir, dir + "/" + hash);
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
        private final int _max;

        public LHM(int max) {
            super(max, 0.75f, true);
            _max = max;
        }

        /** Add the entry, and update the total size */
        @Override
        public Object put(Integer key, Object value) {
            Object rv = super.put(key, value);
            File f = toFile(key.intValue());
            if (f.exists()) {
                _totalSize += f.length();
            }
            return rv;
        }

        /** Remove the entry and the file, and update the total size */
        @Override
        public Object remove(Object key) {
            Object rv = super.remove(key);
            if (rv != null && key instanceof Integer) {
                File f = toFile(((Integer)key).intValue());
                if (f.exists()) {
                    _totalSize -= f.length();
                    f.delete();
                }
            }
            return rv;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Object> eldest) {
            if (size() > _max || _totalSize > MAX_SPACE) {
                Integer key = eldest.getKey();
                remove(key);
            }
            // we modified the map, we must return false
            return false;
        }
    }
}
