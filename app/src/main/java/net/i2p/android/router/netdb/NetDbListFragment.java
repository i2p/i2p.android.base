package net.i2p.android.router.netdb;

import android.app.Activity;
import android.os.Bundle;
//import android.support.v4.app.ListFragment;
import androidx.fragment.app.ListFragment;
//import android.support.v4.app.LoaderManager;
import androidx.loader.app.LoaderManager;
//import android.support.v4.content.Loader;
import androidx.loader.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import net.i2p.data.Hash;

import java.util.List;

public class NetDbListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<NetDbEntry>> {
    public static final String SHOW_ROUTERS = "show_routers";

    private static final int ROUTER_LOADER_ID = 1;
    private static final int LEASESET_LOADER_ID = 2;
    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    private OnEntrySelectedListener mEntrySelectedCallback;
    private NetDbEntryAdapter mAdapter;
    private boolean mRouters;
    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;
    private boolean mActivateOnItemClick = false;

    // Container Activity must implement this interface
    public interface OnEntrySelectedListener {
        void onEntrySelected(boolean isRouterInfo, Hash entryHash);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mEntrySelectedCallback = (OnEntrySelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnEntrySelectedListener");
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState
                    .getInt(STATE_ACTIVATED_POSITION));
        }

        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(
                mActivateOnItemClick ? ListView.CHOICE_MODE_SINGLE
                        : ListView.CHOICE_MODE_NONE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new NetDbEntryAdapter(getActivity());
        mRouters = getArguments().getBoolean(SHOW_ROUTERS);

        setListAdapter(mAdapter);

        if (Util.getRouterContext() == null)
            setEmptyText(getResources().getString(
                    R.string.router_not_running));
        else {
            setEmptyText(getResources().getString((mRouters ?
                    R.string.netdb_routers_empty :
                    R.string.netdb_leases_empty)));

            setListShown(false);
            getLoaderManager().initLoader(mRouters ? ROUTER_LOADER_ID
                    : LEASESET_LOADER_ID, null, this);
        }
    }

    @Override
    public void onListItemClick(ListView parent, View view, int pos, long id) {
        super.onListItemClick(parent, view, pos, id);
        NetDbEntry entry = mAdapter.getItem(pos);
        mEntrySelectedCallback.onEntrySelected(
                entry.isRouterInfo(), entry.getHash());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_netdb_list_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
        case R.id.action_refresh:
            if (Util.getRouterContext() != null) {
                setListShown(false);
                getLoaderManager().restartLoader(mRouters ? ROUTER_LOADER_ID
                        : LEASESET_LOADER_ID, null, this);
            }
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        mActivateOnItemClick = activateOnItemClick;
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    // LoaderManager.LoaderCallbacks<List<NetDbEntry>>

    public Loader<List<NetDbEntry>> onCreateLoader(int id, Bundle args) {
        return new NetDbEntryLoader(getActivity(), mRouters);
    }

    public void onLoadFinished(Loader<List<NetDbEntry>> loader,
            List<NetDbEntry> data) {
        if (loader.getId() == (mRouters ?
                ROUTER_LOADER_ID : LEASESET_LOADER_ID)) {
            mAdapter.setData(data);

            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }
        }
    }

    public void onLoaderReset(Loader<List<NetDbEntry>> loader) {
        if (loader.getId() == (mRouters ?
                ROUTER_LOADER_ID : LEASESET_LOADER_ID)) {
            mAdapter.setData(null);
        }
    }
}
