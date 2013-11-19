package net.i2p.android.router.activity;

import android.os.Bundle;
import net.i2p.android.router.R;
import net.i2p.android.router.fragment.LogDetailFragment;

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
