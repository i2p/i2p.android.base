package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.GraphFragment;
import net.i2p.android.router.service.RouterService;
import android.os.Bundle;

public class GraphActivity extends I2PActivityBase {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        // Start with the base view
        if (savedInstanceState == null) {
            String rateName = getIntent().getStringExtra(GraphFragment.RATE_NAME);
            long period = getIntent().getLongExtra(GraphFragment.RATE_PERIOD, 60 * 1000);
            GraphFragment f = GraphFragment.newInstance(rateName, period);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment, f).commit();
        }
    }
}
