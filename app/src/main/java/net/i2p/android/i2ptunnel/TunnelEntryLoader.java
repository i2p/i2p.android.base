package net.i2p.android.i2ptunnel;

import android.content.Context;
import android.os.Handler;
import android.support.v4.content.AsyncTaskLoader;

import net.i2p.android.router.util.Util;
import net.i2p.i2ptunnel.TunnelController;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.router.RouterContext;

import java.util.ArrayList;
import java.util.List;

public class TunnelEntryLoader extends AsyncTaskLoader<List<TunnelEntry>> {
    private final TunnelControllerGroup mGroup;
    private final boolean mClientTunnels;
    private List<TunnelEntry> mData;
    private final Handler mHandler;
    private TunnelControllerMonitor mMonitor;

    public TunnelEntryLoader(Context context, TunnelControllerGroup tcg, boolean clientTunnels) {
        super(context);
        mGroup = tcg;
        mClientTunnels = clientTunnels;
        mHandler = new Handler();
    }

    @Override
    public List<TunnelEntry> loadInBackground() {
        // Don't load tunnels if the router is not running
        // TODO: in future we might be able to view and edit tunnels while router is not running
        RouterContext routerContext = Util.getRouterContext();
        if (routerContext == null)
            return null;

        List<TunnelEntry> ret = new ArrayList<>();
        List<TunnelController> controllers = mGroup.getControllers();
        for (int i = 0; i < controllers.size(); i++) {
            TunnelEntry tunnel = new TunnelEntry(getContext(), controllers.get(i), i);
            if ( (mClientTunnels && tunnel.isClient()) ||
                 (!mClientTunnels && !tunnel.isClient()) )
                ret.add(tunnel);
        }
        return ret;
    }

    @Override
    public void deliverResult(List<TunnelEntry> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            if (data != null) {
                releaseResources(data);
                return;
            }
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<TunnelEntry> oldData = mData;
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

        // Begin monitoring the underlying data source.
        mMonitor = new TunnelControllerMonitor();
        mHandler.postDelayed(mMonitor, 50);

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

        // The Loader is being reset, so we should stop monitoring for changes.
        if (mMonitor != null) {
            mHandler.removeCallbacks(mMonitor);
            mMonitor = null;
        }
    }

    @Override
    public void onCanceled(List<TunnelEntry> data) {
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        releaseResources(data);
    }

    private void releaseResources(List<TunnelEntry> data) {
        // For a simple List, there is nothing to do. For something like a Cursor, we 
        // would close it in this method. All resources associated with the Loader
        // should be released here.
    }

    private class TunnelControllerMonitor implements Runnable {
        public void run() {
            // There is no way (yet) to monitor for changes to the list of
            // TunnelControllers, so just force a refresh every 10 seconds.
            onContentChanged();
            mHandler.postDelayed(this, 10 * 1000);
        }
    }
}
