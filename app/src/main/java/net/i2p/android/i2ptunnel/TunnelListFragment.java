package net.i2p.android.i2ptunnel;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import net.i2p.android.router.R;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.service.State;
import net.i2p.android.router.util.Util;
import net.i2p.android.util.FragmentUtils;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.router.RouterContext;

import java.util.List;

public class TunnelListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<TunnelEntry>> {
    public static final String SHOW_CLIENT_TUNNELS = "show_client_tunnels";

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

    public static TunnelListFragment newInstance(boolean showClientTunnels) {
        TunnelListFragment f = new TunnelListFragment();
        Bundle args = new Bundle();
        args.putBoolean(TunnelListFragment.SHOW_CLIENT_TUNNELS, showClientTunnels);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        mCallback = FragmentUtils.getParent(this, OnTunnelSelectedListener.class);
        if (mCallback == null)
            throw new ClassCastException("Parent must implement OnTunnelSelectedListener");

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

        setListAdapter(mAdapter);

        if (Util.getRouterContext() == null)
            setEmptyText(getResources().getString(
                    R.string.router_not_running));
        else {
            initTunnels();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());

        IntentFilter filter = new IntentFilter();
        filter.addAction(RouterService.LOCAL_BROADCAST_STATE_NOTIFICATION);
        filter.addAction(RouterService.LOCAL_BROADCAST_STATE_CHANGED);
        lbm.registerReceiver(onStateChange, filter);

        lbm.sendBroadcast(new Intent(RouterService.LOCAL_BROADCAST_REQUEST_STATE));
    }

    private State lastRouterState = null;
    private BroadcastReceiver onStateChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            State state = intent.getParcelableExtra(RouterService.LOCAL_BROADCAST_EXTRA_STATE);
            if (lastRouterState == null || lastRouterState != state) {
                updateState(state);
                lastRouterState = state;
            }
        }
    };

    public void updateState(State state) {
        if (state == State.STOPPING || state == State.STOPPED ||
                state == State.MANUAL_STOPPING ||
                state == State.MANUAL_STOPPED ||
                state == State.MANUAL_QUITTING ||
                state == State.MANUAL_QUITTED)
            setEmptyText(getResources().getString(
                    R.string.router_not_running));
        else
            initTunnels();
    }

    private void initTunnels() {
        if (mGroup == null) {
            String error = null;
            try {
                mGroup = TunnelControllerGroup.getInstance();
            } catch (IllegalArgumentException iae) {
                mGroup = null;
                error = iae.toString();
            }

            if (mGroup == null)
                setEmptyText(error);
        }

        if (mGroup != null) {
            if (mClientTunnels)
                setEmptyText("No configured client tunnels.");
            else
                setEmptyText("No configured server tunnels.");

            setListShown(false);
            getLoaderManager().initLoader(mClientTunnels ? CLIENT_LOADER_ID
                    : SERVER_LOADER_ID, null, this);
        }
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
    public void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(onStateChange);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_i2ptunnel_list_actions, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        RouterContext rCtx = Util.getRouterContext();

        menu.findItem(R.id.action_start_all_tunnels).setVisible(rCtx != null);
        menu.findItem(R.id.action_stop_all_tunnels).setVisible(rCtx != null);
        menu.findItem(R.id.action_restart_all_tunnels).setVisible(rCtx != null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        List<String> msgs;
        switch (item.getItemId()) {
            case R.id.action_start_all_tunnels:
                msgs = mGroup.startAllControllers();
                break;
            case R.id.action_stop_all_tunnels:
                msgs = mGroup.stopAllControllers();
                break;
            case R.id.action_restart_all_tunnels:
                msgs = mGroup.restartAllControllers();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        // TODO: Do something with the other messages
        if (msgs.size() > 0)
            Toast.makeText(getActivity().getApplicationContext(),
                    msgs.get(0), Toast.LENGTH_LONG).show();
        return true;
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

    public void addTunnel(TunnelEntry tunnelEntry) {
        mAdapter.add(tunnelEntry);
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
