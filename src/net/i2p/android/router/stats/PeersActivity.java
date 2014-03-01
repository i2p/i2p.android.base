package net.i2p.android.router.stats;

import net.i2p.android.router.I2PActivityBase;
import net.i2p.android.router.R;
import net.i2p.android.router.service.RouterService;
import android.os.Bundle;

public class PeersActivity extends I2PActivityBase {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        // Start with the base view
        if (savedInstanceState == null) {
            PeersFragment f = new PeersFragment();
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment, f).commit();
        }
    }

    /**
     *  Not bound by the time onResume() is called, so we have to do it here.
     *  If it is bound we update twice.
     */
    @Override
    protected void onRouterBind(RouterService svc) {
        PeersFragment f = (PeersFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        f.update();
    }

    @Override
    public void onBackPressed() {
        PeersFragment f = (PeersFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        if (!f.onBackPressed())
            super.onBackPressed();
    }
}
