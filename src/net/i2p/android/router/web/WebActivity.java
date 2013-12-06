package net.i2p.android.router.web;

import net.i2p.android.router.I2PActivityBase;
import net.i2p.android.router.R;
import android.os.Bundle;

public class WebActivity extends I2PActivityBase {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Start with the base view
        if (savedInstanceState == null) {
            WebFragment f = new WebFragment();
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment, f).commit();
        }
    }

    @Override
    public void onBackPressed() {
        WebFragment f = (WebFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        if (!f.onBackPressed())
            super.onBackPressed();
    }
}
