package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.I2PTunnelDetailFragment;
import net.i2p.android.router.fragment.I2PTunnelListFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;

public class I2PTunnelActivity extends I2PActivityBase
        implements I2PTunnelListFragment.OnTunnelSelectedListener {
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected boolean canUseTwoPanes() {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up action bar for tabs
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Client tunnels tab
        I2PTunnelListFragment cf = new I2PTunnelListFragment();
        Bundle args = new Bundle();
        args.putBoolean(I2PTunnelListFragment.SHOW_CLIENT_TUNNELS, true);
        cf.setArguments(args);
        Tab tab = actionBar.newTab()
                .setText(R.string.label_i2ptunnel_client)
                .setTabListener(new TabListener(cf));
        actionBar.addTab(tab);

        // Server tunnels tab
        I2PTunnelListFragment sf = new I2PTunnelListFragment();
        args = new Bundle();
        args.putBoolean(I2PTunnelListFragment.SHOW_CLIENT_TUNNELS, false);
        sf.setArguments(args);
        tab = actionBar.newTab()
                .setText(R.string.label_i2ptunnel_server)
                .setTabListener(new TabListener(sf));
        actionBar.addTab(tab);

        if (findViewById(R.id.detail_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            cf.setActivateOnItemClick(true);
            sf.setActivateOnItemClick(true);
        }
    }

    // I2PTunnelListFragment.OnTunnelSelectedListener

    public void onTunnelSelected(int tunnelId) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            I2PTunnelDetailFragment detailFrag = I2PTunnelDetailFragment.newInstance(tunnelId);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.detail_fragment, detailFrag).commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, I2PTunnelDetailActivity.class);
            detailIntent.putExtra(I2PTunnelDetailFragment.TUNNEL_ID, tunnelId);
            startActivity(detailIntent);
        }
    }
}
