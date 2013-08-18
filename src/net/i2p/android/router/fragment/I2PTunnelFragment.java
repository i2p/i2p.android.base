package net.i2p.android.router.fragment;

import java.util.List;

import net.i2p.android.router.R;
import net.i2p.android.router.adapter.TunnelControllerAdapter;
import net.i2p.android.router.loader.TunnelControllerLoader;
import net.i2p.i2ptunnel.TunnelController;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class I2PTunnelFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<TunnelController>> {
    public static final String SHOW_CLIENT_TUNNELS = "show_client_tunnels";

    private static final int CLIENT_LOADER_ID = 1;
    private static final int SERVER_LOADER_ID = 2;

    private TunnelControllerGroup mGroup;
    private TunnelControllerAdapter mAdapter;
    private boolean mClientTunnels;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new TunnelControllerAdapter(getActivity());
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

    public Loader<List<TunnelController>> onCreateLoader(int id, Bundle args) {
        return new TunnelControllerLoader(getActivity(), mGroup, mClientTunnels);
    }

    public void onLoadFinished(Loader<List<TunnelController>> loader,
            List<TunnelController> data) {
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

    public void onLoaderReset(Loader<List<TunnelController>> loader) {
        if (loader.getId() == (mClientTunnels ?
                CLIENT_LOADER_ID : SERVER_LOADER_ID)) {
            mAdapter.setData(null);
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
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
