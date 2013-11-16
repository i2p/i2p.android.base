package net.i2p.android.router.fragment;

import net.i2p.router.CommSystemFacade;
import net.i2p.router.NetworkDatabaseFacade;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.TunnelManagerFacade;
import net.i2p.router.peermanager.ProfileOrganizer;
import net.i2p.router.transport.FIFOBandwidthLimiter;
import net.i2p.stat.StatManager;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

public class I2PFragmentBase extends Fragment {
    private boolean mOnActivityCreated;
    private boolean mOnRouterBind;
    RouterContextProvider mCallback;

    protected static final String PREF_INSTALLED_VERSION = "app.version";

    public interface RouterContextUser {
        public void onRouterBind();
    }

    // Container Activity must implement this interface
    public interface RouterContextProvider {
        public RouterContext getRouterContext();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (RouterContextProvider) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement RouterContextProvider");
        }

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mOnActivityCreated = true;
        // Check getRouterContext() in case this was not the
        // active Fragment when onRouterBind() was called.
        if (mOnRouterBind || getRouterContext() != null)
            onRouterConnectionReady();
    }

    public void onRouterBind() {
        mOnRouterBind = true;
        if (mOnActivityCreated)
            onRouterConnectionReady();
    }

    /** callback from I2PFragmentBase, override as necessary */
    public void onRouterConnectionReady() {}

    protected RouterContext getRouterContext() {
        return mCallback.getRouterContext();
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
