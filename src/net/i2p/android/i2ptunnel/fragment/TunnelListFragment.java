package net.i2p.android.i2ptunnel.fragment;

import java.util.List;

import net.i2p.android.i2ptunnel.activity.TunnelWizardActivity;
import net.i2p.android.i2ptunnel.adapter.TunnelEntryAdapter;
import net.i2p.android.i2ptunnel.loader.TunnelEntry;
import net.i2p.android.i2ptunnel.loader.TunnelEntryLoader;
import net.i2p.android.router.R;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class TunnelListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<TunnelEntry>> {
    public static final String SHOW_CLIENT_TUNNELS = "show_client_tunnels";
    public static final String TUNNEL_WIZARD_DATA = "tunnel_wizard_data";

    static final int TUNNEL_WIZARD_REQUEST = 1;

    private static final int CLIENT_LOADER_ID = 1;
    private static final int SERVER_LOADER_ID = 2;
    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    OnTunnelSelectedListener mCallback;
    private TunnelControllerGroup mGroup;
    private TunnelEntryAdapter mAdapter;
    private boolean mClientTunnels;
    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;
    private boolean mActivateOnItemClick = false;

    // Container Activity must implement this interface
    public interface OnTunnelSelectedListener {
        public void onTunnelSelected(int tunnelId);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnTunnelSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTunnelSelectedListener");
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
        mAdapter = new TunnelEntryAdapter(getActivity());
        mClientTunnels = getArguments().getBoolean(SHOW_CLIENT_TUNNELS);

        String error;
        try {
            mGroup = TunnelControllerGroup.getInstance();
            error = mGroup == null ? getResources().getString(R.string.i2ptunnel_not_initialized) : null;
        } catch (IllegalArgumentException iae) {
            mGroup = null;
            error = iae.toString();
        }

        if (mGroup == null) {
            setEmptyText(error);
        } else {
            if (mClientTunnels)
                setEmptyText("No configured client tunnels.");
            else
                setEmptyText("No configured server tunnels.");
        }

        setListAdapter(mAdapter);
        setListShown(false);

        getLoaderManager().initLoader(mClientTunnels ? CLIENT_LOADER_ID
                : SERVER_LOADER_ID, null, this);
    }

    @Override
    public void onListItemClick(ListView parent, View view, int pos, long id) {
        super.onListItemClick(parent, view, pos, id);
        mCallback.onTunnelSelected(mAdapter.getItem(pos).getId());
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
        inflater.inflate(R.menu.fragment_i2ptunnel_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_add_tunnel:
                Intent wi = new Intent(getActivity(), TunnelWizardActivity.class);
                startActivityForResult(wi, TUNNEL_WIZARD_REQUEST);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TUNNEL_WIZARD_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Bundle tunnelData = data.getExtras().getBundle(TUNNEL_WIZARD_DATA);
            }
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

    // LoaderManager.LoaderCallbacks<List<TunnelEntry>>

    public Loader<List<TunnelEntry>> onCreateLoader(int id, Bundle args) {
        return new TunnelEntryLoader(getActivity(), mGroup, mClientTunnels);
    }

    public void onLoadFinished(Loader<List<TunnelEntry>> loader,
            List<TunnelEntry> data) {
        if (loader.getId() == (mClientTunnels ?
                CLIENT_LOADER_ID : SERVER_LOADER_ID)) {
            mAdapter.setData(data);

            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }
        }
    }

    public void onLoaderReset(Loader<List<TunnelEntry>> loader) {
        if (loader.getId() == (mClientTunnels ?
                CLIENT_LOADER_ID : SERVER_LOADER_ID)) {
            mAdapter.setData(null);
        }
    }
}
