package net.i2p.android.i2ptunnel;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import net.i2p.android.I2PActivityBase;
import net.i2p.android.i2ptunnel.preferences.EditTunnelActivity;

public class TunnelDetailActivity extends I2PActivityBase implements
        TunnelDetailFragment.TunnelDetailListener {
    private boolean transitionReversed;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transitionReversed = false;

        if (savedInstanceState == null) {
            int tunnelId = getIntent().getIntExtra(TunnelDetailFragment.TUNNEL_ID, 0);
            TunnelDetailFragment detailFrag = TunnelDetailFragment.newInstance(tunnelId);
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, detailFrag).commit();
        }
    }

    @Override
    public void finish() {
        if (transitionReversed)
            super.finish();
        else {
            transitionReversed = true;
            ActivityCompat.finishAfterTransition(this);
        }
    }

    // TunnelDetailFragment.TunnelDetailListener

    @Override
    public void onEditTunnel(int tunnelId) {
        Intent editIntent = new Intent(this, EditTunnelActivity.class);
        editIntent.putExtra(TunnelDetailFragment.TUNNEL_ID, tunnelId);
        startActivity(editIntent);
    }

    public void onTunnelDeleted(int tunnelId, int numTunnelsLeft) {
        finish();
    }
}
