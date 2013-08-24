package net.i2p.android.i2ptunnel.activity;

import net.i2p.android.i2ptunnel.fragment.TunnelDetailFragment;
import net.i2p.android.router.R;
import net.i2p.android.router.activity.I2PActivityBase;
import android.os.Bundle;

public class TunnelDetailActivity extends I2PActivityBase {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrawerToggle.setDrawerIndicatorEnabled(false);

        if (savedInstanceState == null) {
            int tunnelId = getIntent().getIntExtra(TunnelDetailFragment.TUNNEL_ID, 0);
            TunnelDetailFragment detailFrag = TunnelDetailFragment.newInstance(tunnelId);
            getSupportFragmentManager().beginTransaction()
                .add(R.id.main_fragment, detailFrag).commit();
        }
    }
}
