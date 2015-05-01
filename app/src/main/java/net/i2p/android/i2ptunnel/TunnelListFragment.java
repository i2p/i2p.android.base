package net.i2p.android.i2ptunnel;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.pnikosis.materialishprogress.ProgressWheel;

import net.i2p.android.router.R;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.service.State;
import net.i2p.android.router.util.Util;
import net.i2p.android.util.FragmentUtils;
import net.i2p.android.widget.DividerItemDecoration;
import net.i2p.android.widget.LoadingRecyclerView;
import net.i2p.app.ClientAppState;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.router.RouterContext;

import java.util.ArrayList;
import java.util.List;

public class TunnelListFragment extends Fragment implements
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
    FragmentUtils.TwoPaneProvider mTwoPane;
    private TunnelControllerGroup mGroup;

    private LoadingRecyclerView mRecyclerView;
    private TunnelEntryAdapter mAdapter;
    private boolean mClientTunnels;

    // Container Activity must implement this interface
    public interface OnTunnelSelectedListener {
        void onTunnelSelected(int tunnelId, Pair<View, String> tunnelName,
                              Pair<View, String> tunnelDescription);
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
        mTwoPane = FragmentUtils.getParent(this, FragmentUtils.TwoPaneProvider.class);
        if (mTwoPane == null)
            throw new ClassCastException("Parent must implement TwoPaneProvider");

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list, container, false);

        mRecyclerView = (LoadingRecyclerView) v.findViewById(R.id.list);
        View empty = v.findViewById(R.id.empty);
        ProgressWheel loading = (ProgressWheel) v.findViewById(R.id.loading);
        mRecyclerView.setLoadingView(empty, loading);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mClientTunnels = getArguments().getBoolean(SHOW_CLIENT_TUNNELS);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Set the adapter for the list view
        mAdapter = new TunnelEntryAdapter(getActivity(), mClientTunnels, mCallback, mTwoPane);
        mRecyclerView.setAdapter(mAdapter);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION))
            mAdapter.setActivatedPosition(savedInstanceState
                    .getInt(STATE_ACTIVATED_POSITION));
        else
            mAdapter.clearActivatedPosition();

        // Initialize the adapter in case the RouterService has not been created
        if (Util.getRouterContext() == null)
            mAdapter.setTunnels(null);
    }

    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());

        IntentFilter filter = new IntentFilter();
        filter.addAction(RouterService.LOCAL_BROADCAST_STATE_NOTIFICATION);
        filter.addAction(RouterService.LOCAL_BROADCAST_STATE_CHANGED);
        lbm.registerReceiver(onStateChange, filter);
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
            getLoaderManager().destroyLoader(mClientTunnels ? CLIENT_LOADER_ID : SERVER_LOADER_ID);
        else
            initTunnels();
    }

    private void initTunnels() {
        if (mGroup == null) {
            try {
                mGroup = TunnelControllerGroup.getInstance();
            } catch (IllegalArgumentException iae) {
                Util.e("Could not load tunnels", iae);
                mGroup = null;
            }
        }

        if (mGroup != null && isAdded()) {
            mRecyclerView.setLoading(true);
            getLoaderManager().initLoader(mClientTunnels ? CLIENT_LOADER_ID
                    : SERVER_LOADER_ID, null, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Triggers loader init via updateState() if the router is running
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(RouterService.LOCAL_BROADCAST_REQUEST_STATE));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int activatedPosition = mAdapter.getActivatedPosition();
        if (activatedPosition >= 0) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, activatedPosition);
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
        boolean showActions = rCtx != null && mGroup != null &&
                (mGroup.getState() == ClientAppState.STARTING ||
                        mGroup.getState() == ClientAppState.RUNNING);

        menu.findItem(R.id.action_start_all_tunnels).setVisible(showActions);
        menu.findItem(R.id.action_stop_all_tunnels).setVisible(showActions);
        menu.findItem(R.id.action_restart_all_tunnels).setVisible(showActions);
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

    public void addTunnel(TunnelEntry tunnelEntry) {
        mAdapter.addTunnel(tunnelEntry);
    }

    // LoaderManager.LoaderCallbacks<List<TunnelEntry>>

    public Loader<List<TunnelEntry>> onCreateLoader(int id, Bundle args) {
        return new TunnelEntryLoader(getActivity(), mGroup, mClientTunnels);
    }

    public void onLoadFinished(Loader<List<TunnelEntry>> loader,
                               List<TunnelEntry> data) {
        if (loader.getId() == (mClientTunnels ?
                CLIENT_LOADER_ID : SERVER_LOADER_ID)) {
            mAdapter.setTunnels(data);
        }
    }

    public void onLoaderReset(Loader<List<TunnelEntry>> loader) {
        if (loader.getId() == (mClientTunnels ?
                CLIENT_LOADER_ID : SERVER_LOADER_ID)) {
            if (Util.getRouterContext() == null)
                mAdapter.setTunnels(null);
            else
                mAdapter.setTunnels(new ArrayList<TunnelEntry>());
        }
    }
}
