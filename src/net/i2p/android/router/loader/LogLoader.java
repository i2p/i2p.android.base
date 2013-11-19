package net.i2p.android.router.loader;

import java.util.Collections;
import java.util.List;

import net.i2p.I2PAppContext;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

public class LogLoader extends AsyncTaskLoader<List<String>> {
    private I2PAppContext mCtx;
    private String mLogLevel;
    private List<String> mData;

    private static final int MAX_LOG_LENGTH = 250;

    public LogLoader(Context context, I2PAppContext ctx, String logLevel) {
        super(context);
        mCtx = ctx;
        mLogLevel = logLevel;
    }

    @Override
    public List<String> loadInBackground() {
        List<String> msgs;
        if ("ERROR".equals(mLogLevel)) {
            msgs = mCtx.logManager().getBuffer().getMostRecentCriticalMessages();
        } else {
            msgs = mCtx.logManager().getBuffer().getMostRecentMessages();
        }
        int sz = msgs.size();
        if (sz > 1)
            Collections.reverse(msgs);
        if (sz > 0 && mData != null) {
            String oldNewest = mData.size() > 0 ? mData.get(0) : null;
            for (int i = 0; i < sz; i++) {
                String newItem = msgs.get(i);
                if (newItem.equals(oldNewest))
                    break;
                mData.add(i, newItem);
            }
            int newSz = mData.size();
            for (int i = newSz - 1; i > MAX_LOG_LENGTH; i--) {
                mData.remove(i);
            }
        }
        return msgs;
    }

    @Override
    public void deliverResult(List<String> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            if (data != null) {
                releaseResources(data);
                return;
            }
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<String> oldData = mData;
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
    public void onCanceled(List<String> data) {
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        releaseResources(data);
    }

    private void releaseResources(List<String> data) {
        // For a simple List, there is nothing to do. For something like a Cursor, we 
        // would close it in this method. All resources associated with the Loader
        // should be released here.
    }
}
