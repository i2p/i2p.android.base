package net.i2p.android.router;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import net.i2p.android.router.util.Util;
import net.i2p.router.CommSystemFacade;
import net.i2p.router.NetworkDatabaseFacade;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.TunnelManagerFacade;
import net.i2p.router.peermanager.ProfileOrganizer;
import net.i2p.router.transport.FIFOBandwidthLimiter;
import net.i2p.stat.StatManager;

public class I2PFragmentBase extends Fragment {
    private boolean mOnActivityCreated;

    public static final String PREF_INSTALLED_VERSION = "app.version";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mOnActivityCreated = true;
        if (getRouterContext() != null)
            onRouterConnectionReady();
        else
            onRouterConnectionNotReady();
    }

    public void onRouterBind() {
        if (mOnActivityCreated)
            onRouterConnectionReady();
    }

    /** callback from I2PFragmentBase, override as necessary */
    public void onRouterConnectionReady() {}

    /** callback from I2PFragmentBase, override as necessary */
    public void onRouterConnectionNotReady() {}

    protected RouterContext getRouterContext() {
        return Util.getRouterContext();
    }

    protected Router getRouter() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.router();
    }

    protected NetworkDatabaseFacade getNetDb() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.netDb();
    }

    protected ProfileOrganizer getProfileOrganizer() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.profileOrganizer();
    }

    protected TunnelManagerFacade getTunnelManager() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.tunnelManager();
    }

    protected CommSystemFacade getCommSystem() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.commSystem();
    }

    protected FIFOBandwidthLimiter getBandwidthLimiter() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.bandwidthLimiter();
    }

    protected StatManager getStatManager() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.statManager();
    }
}
