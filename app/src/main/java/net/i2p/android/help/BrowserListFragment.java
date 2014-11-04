package net.i2p.android.help;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.i2p.android.router.R;
import net.i2p.android.router.util.BetterAsyncTaskLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrowserListFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<List<Browser>> {
    private static final int BROWSER_LOADER_ID = 1;

    private BrowserAdapter.OnBrowserSelectedListener mCallback;
    private BrowserAdapter mAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (BrowserAdapter.OnBrowserSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnBrowserSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_help_browsers, container, false);
        RecyclerView mRecyclerView = (RecyclerView) v.findViewById(R.id.browser_list);

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new BrowserAdapter(getActivity(), mCallback);
        mRecyclerView.setAdapter(mAdapter);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(BROWSER_LOADER_ID, null, this);
    }

    // LoaderManager.LoaderCallbacks<List<Browser>>

    @Override
    public Loader<List<Browser>> onCreateLoader(int id, Bundle args) {
        return new BrowserLoader(getActivity());
    }

    public static class BrowserLoader extends BetterAsyncTaskLoader<List<Browser>> {
        private List<String> supported;
        private List<String> unsupported;

        public BrowserLoader(Context context) {
            super(context);
            supported = Arrays.asList(
                    context.getResources().getStringArray(R.array.supported_browsers));
            unsupported = Arrays.asList(
                    context.getResources().getStringArray(R.array.unsupported_browsers));
        }

        @Override
        public List<Browser> loadInBackground() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://stats.i2p"));

            final PackageManager pm = getContext().getPackageManager();
            List<ResolveInfo> installedBrowsers = pm.queryIntentActivities(intent, 0);

            List<Browser> browsers = new ArrayList<Browser>();
            for (ResolveInfo browser : installedBrowsers) {
                if (supported.contains(browser.activityInfo.packageName))
                    browsers.add(new Browser(pm, browser, true));
                else if (unsupported.contains(browser.activityInfo.packageName))
                    browsers.add(new Browser(pm, browser, false));
                else
                    browsers.add(new Browser(pm, browser));
            }
            Collections.sort(browsers);
            return browsers;
        }

        @Override
        protected void onStartMonitoring() {
        }

        @Override
        protected void onStopMonitoring() {
        }

        @Override
        protected void releaseResources(List<Browser> data) {
        }
    }

    @Override
    public void onLoadFinished(Loader<List<Browser>> listLoader, List<Browser> browsers) {
        if (listLoader.getId() == BROWSER_LOADER_ID)
            mAdapter.setBrowsers(browsers.toArray(new Browser[browsers.size()]));
    }

    @Override
    public void onLoaderReset(Loader<List<Browser>> listLoader) {
        if (listLoader.getId() == BROWSER_LOADER_ID)
            mAdapter.clear();
    }
}
