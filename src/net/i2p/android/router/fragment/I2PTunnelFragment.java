package net.i2p.android.router.fragment;

import net.i2p.android.router.R;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class I2PTunnelFragment extends ListFragment {
    private TunnelControllerGroup mGroup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TunnelControllerGroup tcg;
        String error;
        try {
            tcg = TunnelControllerGroup.getInstance();
            error = tcg == null ? getResources().getString(R.string.i2ptunnel_not_initialized) : null;
        } catch (IllegalArgumentException iae) {
            tcg = null;
            error = iae.toString();
        }

        if (tcg != null) {
            mGroup = tcg;
            setEmptyText("Win!");
        } else {
            setEmptyText(error);
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
