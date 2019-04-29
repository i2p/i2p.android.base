package net.i2p.android.i2ptunnel.util;

import android.os.AsyncTask;

import net.i2p.I2PAppContext;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.i2ptunnel.ui.TunnelConfig;

import java.util.List;

/**
 * Save a TunnelConfig.
 *
 * This must be performed in a background thread, because the underlying I2P code calls
 * InetAddress.getByName(), which will trigger a NetworkOnMainThreadException otherwise.
 */
public class SaveTunnelTask  extends AsyncTask<Void, Void, List<String>> {
    final TunnelControllerGroup mGroup;
    final int mTunnelId;
    final TunnelConfig mCfg;

    public SaveTunnelTask(TunnelControllerGroup group, int tunnelId, TunnelConfig cfg) {
        mGroup = group;
        mTunnelId = tunnelId;
        mCfg = cfg;
    }

    @Override
    protected List<String> doInBackground(Void... voids) {
        return TunnelUtil.saveTunnel(I2PAppContext.getGlobalContext(), mGroup, mTunnelId, mCfg);
    }
}
