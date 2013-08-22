package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.I2PTunnelDetailFragment;
import android.os.Bundle;

public class I2PTunnelDetailActivity extends I2PActivityBase {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrawerToggle.setDrawerIndicatorEnabled(false);

        if (savedInstanceState == null) {
            int tunnelId = getIntent().getIntExtra(I2PTunnelDetailFragment.TUNNEL_ID, 0);
            I2PTunnelDetailFragment detailFrag = I2PTunnelDetailFragment.newInstance(tunnelId);
            getSupportFragmentManager().beginTransaction()
                .add(R.id.main_fragment, detailFrag).commit();
        }
    }
}
