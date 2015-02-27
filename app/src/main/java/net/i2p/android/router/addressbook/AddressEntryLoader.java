package net.i2p.android.router.addressbook;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import net.i2p.android.router.I2PFragmentBase;
import net.i2p.android.router.util.NamingServiceUtil;
import net.i2p.android.router.util.Util;
import net.i2p.client.naming.NamingService;
import net.i2p.data.Destination;
import net.i2p.router.RouterContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class AddressEntryLoader extends AsyncTaskLoader<List<AddressEntry>> {
    private I2PFragmentBase.RouterContextProvider mRContextProvider;
    private String mBook;
    private String mFilter;
    private List<AddressEntry> mData;

    public AddressEntryLoader(Context context, I2PFragmentBase.RouterContextProvider rContextProvider,
            String book, String filter) {
        super(context);
        mRContextProvider = rContextProvider;
        mBook = book;
        mFilter = filter;
    }

    @Override
    public List<AddressEntry> loadInBackground() {
        RouterContext routerContext = mRContextProvider.getRouterContext();
        if (routerContext == null)
            return null;

        // get the names
        NamingService ns = NamingServiceUtil.getNamingService(routerContext, mBook);
        Util.d("NamingService: " + ns.getName());
        // After router shutdown we get nothing... why?
        List<AddressEntry> ret = new ArrayList<AddressEntry>();
        Map<String, Destination> names = new TreeMap<String, Destination>();

        Properties searchProps = new Properties();
        // Needed for HostsTxtNamingService
        searchProps.setProperty("file", mBook);
        if (mFilter != null && mFilter.length() > 0)
            searchProps.setProperty("search", mFilter);

        names.putAll(ns.getEntries(searchProps));
        for (String hostName : names.keySet())
            ret.add(new AddressEntry(hostName, names.get(hostName)));
        return ret;
    }

    @Override
    public void deliverResult(List<AddressEntry> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            if (data != null) {
                releaseResources(data);
                return;
            }
        }

        // Hold a reference to the old data so it doesn't get garbage collected.
        // We must protect it until the new data has been delivered.
        List<AddressEntry> oldData = mData;
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
    public void onCanceled(List<AddressEntry> data) {
        // Attempt to cancel the current asynchronous load.
        super.onCanceled(data);

        // The load has been canceled, so we should release the resources
        // associated with 'data'.
        releaseResources(data);
    }

    private void releaseResources(List<AddressEntry> data) {
        // For a simple List, there is nothing to do. For something like a Cursor, we 
        // would close it in this method. All resources associated with the Loader
        // should be released here.
    }
}
