package net.i2p.android.router.netdb;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import net.i2p.android.router.util.Util;
import net.i2p.data.Destination;
import net.i2p.data.LeaseSet;
import net.i2p.data.router.RouterInfo;
import net.i2p.router.RouterContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class NetDbEntryLoader extends AsyncTaskLoader<List<NetDbEntry>> {
    private boolean mRouters;
    private List<NetDbEntry> mData;

    public NetDbEntryLoader(Context context, boolean routers) {
        super(context);
        mRouters = routers;
    }

    private static class RouterInfoComparator implements Comparator<RouterInfo> {
        public int compare(RouterInfo l, RouterInfo r) {
            return l.getIdentity().getHash().toBase64().compareTo(r.getIdentity().getHash().toBase64());
        }
    }

    private class LeaseSetComparator implements Comparator<LeaseSet> {
        private RouterContext mRContext;

        public LeaseSetComparator(RouterContext rContext) {
            super();
            mRContext = rContext;
        }

        public int compare(LeaseSet l, LeaseSet r) {
            Destination dl = l.getDestination();
            Destination dr = r.getDestination();
            boolean locall = mRContext.clientManager().isLocal(dl);
            boolean localr = mRContext.clientManager().isLocal(dr);
            if (locall && !localr) return -1;
            if (localr && !locall) return 1;
            return dl.calculateHash().toBase64().compareTo(dr.calculateHash().toBase64());
        }
    }

    @Override
    public List<NetDbEntry> loadInBackground() {
        List<NetDbEntry> ret = new ArrayList<>();
        RouterContext routerContext = Util.getRouterContext();
        if (routerContext != null && routerContext.netDb().isInitialized()) {
            if (mRouters) {
                Set<RouterInfo> routers = new TreeSet<>(new RouterInfoComparator());
                routers.addAll(routerContext.netDb().getRouters());
                for (RouterInfo ri : routers) {
                    NetDbEntry entry = NetDbEntry.fromRouterInfo(routerContext, ri);
                    ret.add(entry);
                }
            } else {
                Set<LeaseSet> leases = new TreeSet<>(new LeaseSetComparator(routerContext));
                leases.addAll(routerContext.netDb().getLeases());
                for (LeaseSet ls : leases) {
                    NetDbEntry entry = NetDbEntry.fromLeaseSet(routerContext, ls);
                    ret.add(entry);
                }
            }
        }
        return ret;
    }

    @Override
    public void deliverResult(List<NetDbEntry> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            if (data != null) {
                releaseResources(data);
                return;
            }
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<NetDbEntry> oldData = mData;
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
    public void onCanceled(List<NetDbEntry> data) {
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        releaseResources(data);
    }

    private void releaseResources(List<NetDbEntry> data) {
        // For a simple List, there is nothing to do. For something like a Cursor, we 
        // would close it in this method. All resources associated with the Loader
        // should be released here.
    }
}
