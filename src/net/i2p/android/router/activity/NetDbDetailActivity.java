package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.NetDbDetailFragment;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataFormatException;
import net.i2p.data.Hash;
import android.os.Bundle;

public class NetDbDetailActivity extends I2PActivityBase {
    NetDbDetailFragment mDetailFrag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDrawerToggle.setDrawerIndicatorEnabled(false);

        if (savedInstanceState == null) {
            boolean isRI = getIntent().getBooleanExtra(NetDbDetailFragment.IS_RI, true);
            Hash hash = new Hash();
            try {
                hash.fromBase64(getIntent().getStringExtra(NetDbDetailFragment.ENTRY_HASH));
                mDetailFrag = NetDbDetailFragment.newInstance(isRI, hash);
                getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment, mDetailFrag).commit();
            } catch (DataFormatException e) {
                Util.e(e.toString());
            }
        }
    }

    @Override
    protected void onRouterBind(RouterService svc) {
        mDetailFrag.onRouterBind();
    }
}
