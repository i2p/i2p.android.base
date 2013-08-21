package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.I2PTunnelFragment;
import net.i2p.android.router.loader.TunnelEntry;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;

public class I2PTunnelActivity extends I2PActivityBase
        implements I2PTunnelFragment.OnTunnelSelectedListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up action bar for tabs
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Client tunnels tab
        Fragment f = new I2PTunnelFragment();
        Bundle args = new Bundle();
        args.putBoolean(I2PTunnelFragment.SHOW_CLIENT_TUNNELS, true);
        f.setArguments(args);
        Tab tab = actionBar.newTab()
                .setText(R.string.label_i2ptunnel_client)
                .setTabListener(new TabListener(f));
        actionBar.addTab(tab);

        // Server tunnels tab
        f = new I2PTunnelFragment();
        args = new Bundle();
        args.putBoolean(I2PTunnelFragment.SHOW_CLIENT_TUNNELS, false);
        f.setArguments(args);
        tab = actionBar.newTab()
                .setText(R.string.label_i2ptunnel_server)
                .setTabListener(new TabListener(f));
        actionBar.addTab(tab);
    }

    // I2PTunnelFragment.OnTunnelSelectedListener

    public void onTunnelSelected(TunnelEntry tunnel) {
    }
}
