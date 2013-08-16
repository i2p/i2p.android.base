package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.I2PTunnelFragment;
import android.os.Bundle;

public class I2PTunnelActivity extends I2PActivityBase {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Start with the base view
        if (savedInstanceState == null) {
            I2PTunnelFragment f = new I2PTunnelFragment();
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_content, f).commit();
        }
    }
}
