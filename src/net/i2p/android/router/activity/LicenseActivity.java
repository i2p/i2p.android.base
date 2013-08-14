package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.LicenseFragment;
import android.os.Bundle;

public class LicenseActivity extends I2PActivityBase {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        // Start with the base view
        if (savedInstanceState == null) {
            LicenseFragment f = new LicenseFragment();
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_content, f).commit();
        }
    }
}
