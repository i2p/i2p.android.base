package net.i2p.android.i2ptunnel;

import android.content.Intent;
import android.os.Bundle;

import net.i2p.android.router.I2PActivityBase;
import net.i2p.android.router.R;

public class TunnelDetailActivity extends I2PActivityBase implements
        TunnelDetailFragment.TunnelDetailListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrawerToggle.setDrawerIndicatorEnabled(false);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (savedInstanceState == null) {
            int tunnelId = getIntent().getIntExtra(TunnelDetailFragment.TUNNEL_ID, 0);
            TunnelDetailFragment detailFrag = TunnelDetailFragment.newInstance(tunnelId);
            getSupportFragmentManager().beginTransaction()
                .add(R.id.main_fragment, detailFrag).commit();
        }
    }

    // TunnelDetailFragment.TunnelDetailListener

    @Override
    public void onEditTunnel(int tunnelId) {
        Intent editIntent = new Intent(this, EditTunnelActivity.class);
        editIntent.putExtra(TunnelDetailFragment.TUNNEL_ID, tunnelId);
        startActivity(editIntent);
    }

    public void onTunnelDeleted(int tunnelId, int numTunnelsLeft) {
        finish();
    }
}
