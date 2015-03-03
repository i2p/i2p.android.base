package net.i2p.android.router;

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
                    .add(R.id.main_fragment, f).commit();
        }
    }
}
