package net.i2p.android.i2ptunnel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import net.i2p.android.help.HelpActivity;
import net.i2p.android.i2ptunnel.util.TunnelConfig;
import net.i2p.android.router.I2PFragmentBase;
import net.i2p.android.router.I2PFragmentBase.RouterContextProvider;
import net.i2p.android.router.R;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.router.RouterContext;

import java.util.List;

public class TunnelListFragment extends ListFragment implements
        I2PFragmentBase.RouterContextUser,
        LoaderManager.LoaderCallbacks<List<TunnelEntry>> {
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

    private boolean mOnActivityCreated;
    RouterContextProvider mRouterContextProvider;
    OnTunnelSelectedListener mCallback;
    private TunnelControllerGroup mGroup;
    private TunnelEntryAdapter mAdapter;
    private boolean mClientTunnels;
    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;
    private boolean mActivateOnItemClick = false;

    private ImageButton mNewTunnel;

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
            mRouterContextProvider = (RouterContextProvider) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement RouterContextProvider");
        }

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create the list fragment's content view by calling the super method
        final View listFragmentView = super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_list_with_add, container, false);
        FrameLayout listContainer = (FrameLayout) v.findViewById(R.id.list_container);
        listContainer.addView(listFragmentView);

        mNewTunnel = (ImageButton) v.findViewById(R.id.promoted_action);
        mNewTunnel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent wi = new Intent(getActivity(), TunnelWizardActivity.class);
                startActivityForResult(wi, TUNNEL_WIZARD_REQUEST);
            }
        });

        return v;
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

        mOnActivityCreated = true;
        if (getRouterContext() != null)
            onRouterConnectionReady();
        else
            setEmptyText(getResources().getString(
                    R.string.router_not_running));
    }

    public void onRouterConnectionReady() {
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_i2ptunnel_list_actions, menu);
        if (getRouterContext() == null) {
            mNewTunnel.setVisibility(View.GONE);
            menu.findItem(R.id.action_start_all_tunnels).setVisible(false);
            menu.findItem(R.id.action_stop_all_tunnels).setVisible(false);
            menu.findItem(R.id.action_restart_all_tunnels).setVisible(false);
        }
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
            case R.id.action_i2ptunnel_help:
                Intent hi = new Intent(getActivity(), HelpActivity.class);
                hi.putExtra(HelpActivity.CATEGORY, HelpActivity.CAT_I2PTUNNEL);
                startActivity(hi);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
        // TODO: Do something with the other messages
        if (msgs.size() > 0)
            Toast.makeText(getActivity().getApplicationContext(),
                    msgs.get(0), Toast.LENGTH_LONG).show();
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TUNNEL_WIZARD_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Bundle tunnelData = data.getExtras().getBundle(TUNNEL_WIZARD_DATA);
                TunnelConfig cfg = TunnelConfig.createFromWizard(getActivity(), mGroup, tunnelData);
                TunnelEntry tunnel = TunnelEntry.createNewTunnel(getActivity(), mGroup, cfg);
                mAdapter.add(tunnel);
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

    // Duplicated from I2PFragmentBase because this extends ListFragment
    private RouterContext getRouterContext() {
        return mRouterContextProvider.getRouterContext();
    }

    // I2PFragmentBase.RouterContextUser

    public void onRouterBind() {
        if (mOnActivityCreated)
            onRouterConnectionReady();
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
