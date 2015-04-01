package net.i2p.android.router;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import net.i2p.android.I2PActivityBase;

public class NewsActivity extends I2PActivityBase {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onepane);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        // Start with the base view
        if (savedInstanceState == null) {
            NewsFragment f = new NewsFragment();
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment, f).commit();
        }
    }
}
