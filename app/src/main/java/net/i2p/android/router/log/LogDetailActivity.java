package net.i2p.android.router.log;

import android.os.Bundle;
import net.i2p.android.router.I2PActivityBase;
import net.i2p.android.router.R;

public class LogDetailActivity extends I2PActivityBase {
    LogDetailFragment mDetailFrag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrawerToggle.setDrawerIndicatorEnabled(false);

        if (savedInstanceState == null) {
            String entry = getIntent().getStringExtra(LogDetailFragment.LOG_ENTRY);
            mDetailFrag = LogDetailFragment.newInstance(entry);
            getSupportFragmentManager().beginTransaction()
                .add(R.id.main_fragment, mDetailFrag).commit();
        }
    }
}
