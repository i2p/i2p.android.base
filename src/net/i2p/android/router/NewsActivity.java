package net.i2p.android.router;

import net.i2p.android.router.R;
import android.os.Bundle;

public class NewsActivity extends I2PActivityBase {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        // Start with the base view
        if (savedInstanceState == null) {
            NewsFragment f = new NewsFragment();
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment, f).commit();
        }
    }
}
