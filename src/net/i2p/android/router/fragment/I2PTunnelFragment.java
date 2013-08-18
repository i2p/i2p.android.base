package net.i2p.android.router.fragment;

import java.util.ArrayList;
import java.util.List;

import net.i2p.android.router.R;
import net.i2p.android.router.adapter.TunnelControllerAdapter;
import net.i2p.i2ptunnel.TunnelController;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class I2PTunnelFragment extends ListFragment {
    public static final String SHOW_CLIENT_TUNNELS = "show_client_tunnels";

    private TunnelControllerGroup mGroup;
    private TunnelControllerAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new TunnelControllerAdapter(getActivity());

        String error;
        try {
            mGroup = TunnelControllerGroup.getInstance();
            error = mGroup == null ? getResources().getString(R.string.i2ptunnel_not_initialized) : null;
        } catch (IllegalArgumentException iae) {
            mGroup = null;
            error = iae.toString();
        }

        boolean clientTunnels = getArguments().getBoolean(SHOW_CLIENT_TUNNELS);
        if (mGroup == null) {
            setEmptyText(error);
        } else {
            if (clientTunnels)
                setEmptyText("No configured client tunnels.");
            else
                setEmptyText("No configured server tunnels.");
        }
        mAdapter.setData(getControllers(clientTunnels));
        setListAdapter(mAdapter);
    }

    private List<TunnelController> getControllers(boolean clientTunnels) {
        List<TunnelController> ret = new ArrayList<TunnelController>();
        for (TunnelController controller : mGroup.getControllers())
            if ( (clientTunnels && isClient(controller.getType())) ||
                 (!clientTunnels && !isClient(controller.getType())) )
                ret.add(controller);
        return ret;
    }

    private static boolean isClient(String type) {
        return ( ("client".equals(type)) ||
                 ("httpclient".equals(type)) ||
                 ("sockstunnel".equals(type)) ||
                 ("socksirctunnel".equals(type)) ||
                 ("connectclient".equals(type)) ||
                 ("streamrclient".equals(type)) ||
                 ("ircclient".equals(type)));
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
