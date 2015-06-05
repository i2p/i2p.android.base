package net.i2p.android.router.web;

import android.os.Bundle;

import net.i2p.android.I2PActivityBase;
import net.i2p.android.router.R;

public class WebActivity extends I2PActivityBase {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);
        // Start with the base view
        if (savedInstanceState == null) {
            WebFragment f = new WebFragment();
            if (getIntent().getData() != null) {
                Bundle b = new Bundle();
                b.putString(WebFragment.HTML_URI, getIntent().getDataString());
                f.setArguments(b);
            } else
                f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment, f).commit();
        }
    }

    @Override
    public void onBackPressed() {
        WebFragment f = (WebFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        if (!f.onBackPressed())
            super.onBackPressed();
    }
}
