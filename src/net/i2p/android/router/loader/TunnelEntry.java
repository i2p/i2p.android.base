package net.i2p.android.router.loader;

import net.i2p.i2ptunnel.TunnelController;

public class TunnelEntry {
    private final TunnelEntryLoader mLoader;
    private final TunnelController mController;

    public TunnelEntry(TunnelEntryLoader loader, TunnelController controller) {
        mLoader = loader;
        mController = controller;
    }

    public TunnelController getController() {
        return mController;
    }

    public String getName() {
        return mController.getName();
    }

    public boolean isClient() {
        return isClient(mController.getType());
    }

    public static boolean isClient(String type) {
        return ( ("client".equals(type)) ||
                 ("httpclient".equals(type)) ||
                 ("sockstunnel".equals(type)) ||
                 ("socksirctunnel".equals(type)) ||
                 ("connectclient".equals(type)) ||
                 ("streamrclient".equals(type)) ||
                 ("ircclient".equals(type)));
    }

    public String getType() {
        return mController.getType();
    }

    public String getIfacePort() {
        return "127.0.0.1:1234";
    }

    public String getDetails() {
        return "Details";
    }
}
