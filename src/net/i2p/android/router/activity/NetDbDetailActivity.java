package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.NetDbDetailFragment;
import net.i2p.android.router.fragment.NetDbListFragment;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataFormatException;
import net.i2p.data.Hash;
import android.content.Intent;
import android.os.Bundle;

public class NetDbDetailActivity extends I2PActivityBase implements
        NetDbListFragment.OnEntrySelectedListener {
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

    // NetDbListFragment.OnEntrySelectedListener

    public void onEntrySelected(boolean isRouterInfo, Hash entryHash) {
        // Start the detail activity for the selected item ID.
        Intent detailIntent = new Intent(this, NetDbDetailActivity.class);
        detailIntent.putExtra(NetDbDetailFragment.IS_RI, isRouterInfo);
        detailIntent.putExtra(NetDbDetailFragment.ENTRY_HASH,
                entryHash.toBase64());
        startActivity(detailIntent);
    }
}
