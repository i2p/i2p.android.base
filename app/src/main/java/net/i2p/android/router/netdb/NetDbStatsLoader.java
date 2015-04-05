package net.i2p.android.router.netdb;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import net.i2p.data.Hash;
import net.i2p.data.router.RouterAddress;
import net.i2p.data.router.RouterInfo;
import net.i2p.router.RouterContext;
import net.i2p.util.ObjectCounter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class NetDbStatsLoader extends AsyncTaskLoader<List<ObjectCounter<String>>> {
    private RouterContext mRContext;
    private List<ObjectCounter<String>> mData;

    public NetDbStatsLoader(Context context, RouterContext rContext) {
        super(context);
        mRContext = rContext;
    }

    private static class RouterInfoComparator implements Comparator<RouterInfo> {
        public int compare(RouterInfo l, RouterInfo r) {
            return l.getHash().toBase64().compareTo(r.getHash().toBase64());
        }
    }

    @Override
    public List<ObjectCounter<String>> loadInBackground() {
        List<ObjectCounter<String>> ret = new ArrayList<>();

        ObjectCounter<String> versions = new ObjectCounter<>();
        ObjectCounter<String> countries = new ObjectCounter<>();
        ObjectCounter<String> transports = new ObjectCounter<>();

        if (mRContext != null && mRContext.netDb() != null && mRContext.netDb().isInitialized()) {
            Hash us = mRContext.routerHash();

            Set<RouterInfo> routers = new TreeSet<>(new RouterInfoComparator());
            routers.addAll(mRContext.netDb().getRouters());
            for (RouterInfo ri : routers) {
                Hash key = ri.getHash();
                if (!key.equals(us)) {
                    String routerVersion = ri.getOption("router.version");
                    if (routerVersion != null)
                        versions.increment(routerVersion);
                    // XXX Disabled, no GeoIP file
                    String country = null;//mRContext.commSystem().getCountry(key);
                    if(country != null)
                        countries.increment(country);
                    transports.increment(classifyTransports(ri));
                }
            }
        }

        ret.add(versions);
        ret.add(countries);
        ret.add(transports);

        return ret;
    }

    private static final int SSU = 1;
    private static final int SSUI = 2;
    private static final int NTCP = 4;
    private static final int IPV6 = 8;
    private static final String[] TNAMES = { "Hidden or starting up", "SSU", "SSU with introducers", "",
                                  "NTCP", "NTCP and SSU", "NTCP and SSU with introducers", "",
                                  "", "IPv6 SSU", "IPv6 Only SSU, introducers", "IPv6 SSU, introducers",
                                  "IPv6 NTCP", "IPv6 NTCP, SSU", "IPv6 Only NTCP, SSU, introducers", "IPv6 NTCP, SSU, introducers" };
    /**
     *  what transport types
     */
    private static String classifyTransports(RouterInfo info) {
        int rv = 0;
        for (RouterAddress addr : info.getAddresses()) {
            String style = addr.getTransportStyle();
            if (style.equals("NTCP")) {
                rv |= NTCP;
            } else if (style.equals("SSU")) {
                if (addr.getOption("iport0") != null)
                    rv |= SSUI;
                else
                    rv |= SSU;
            }
            String host = addr.getHost();
            if (host != null && host.contains(":"))
                rv |= IPV6;

        }
        return TNAMES[rv];
    }

    @Override
    public void deliverResult(List<ObjectCounter<String>> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            if (data != null) {
                releaseResources(data);
                return;
            }
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<ObjectCounter<String>> oldData = mData;
        mData = data;

        if (isStarted()) {
            // If the Loader is in a started state, have the superclass deliver the
            // results to the client.
            super.deliverResult(data);
        }

        // Invalidate the old data as we don't need it any more.
        if (oldData != null && oldData != data) {
            releaseResources(oldData);
        }
    }

    @Override
    protected void onStartLoading() {
        if (mData != null) {
            // Deliver any previously loaded data immediately.
            deliverResult(mData);
        }

        if (takeContentChanged() || mData == null) {
            // When the observer detects a change, it should call onContentChanged()
            // on the Loader, which will cause the next call to takeContentChanged()
            // to return true. If this is ever the case (or if the current data is
            // null), we force a new load.
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // The Loader is in a stopped state, so we should attempt to cancel the 
        // current load (if there is one).
        cancelLoad();

        // Note that we leave the observer as is. Loaders in a stopped state
        // should still monitor the data source for changes so that the Loader
        // will know to force a new load if it is ever started again.
    }

    @Override
    protected void onReset() {
        // Ensure the loader has been stopped.
        onStopLoading();

        // At this point we can release the resources associated with 'mData'.
        if (mData != null) {
            releaseResources(mData);
            mData = null;
        }
    }

    @Override
    public void onCanceled(List<ObjectCounter<String>> data) {
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        releaseResources(data);
    }

    private void releaseResources(List<ObjectCounter<String>> data) {
        // For a simple List, there is nothing to do. For something like a Cursor, we 
        // would close it in this method. All resources associated with the Loader
        // should be released here.
    }
}
