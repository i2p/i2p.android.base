package net.i2p.android.router.loader;

import android.graphics.drawable.Drawable;
import net.i2p.android.router.R;
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

    public String getTypeName() {
        if ("client".equals(mController.getType()))
            return mLoader.getContext().getResources()
                    .getString(R.string.i2ptunnel_type_client);
        else if ("httpclient".equals(mController.getType()))
            return mLoader.getContext().getResources()
                    .getString(R.string.i2ptunnel_type_httpclient);
        else if ("ircclient".equals(mController.getType()))
            return mLoader.getContext().getResources()
                    .getString(R.string.i2ptunnel_type_ircclient);
        else if ("server".equals(mController.getType()))
            return mLoader.getContext().getResources()
                    .getString(R.string.i2ptunnel_type_server);
        else if ("httpserver".equals(mController.getType()))
            return mLoader.getContext().getResources()
                    .getString(R.string.i2ptunnel_type_httpserver);
        else if ("sockstunnel".equals(mController.getType()))
            return mLoader.getContext().getResources()
                    .getString(R.string.i2ptunnel_type_sockstunnel);
        else if ("socksirctunnel".equals(mController.getType()))
            return mLoader.getContext().getResources()
                    .getString(R.string.i2ptunnel_type_socksirctunnel);
        else if ("connectclient".equals(mController.getType()))
            return mLoader.getContext().getResources()
                    .getString(R.string.i2ptunnel_type_connectclient);
        else if ("ircserver".equals(mController.getType()))
            return mLoader.getContext().getResources()
                    .getString(R.string.i2ptunnel_type_ircserver);
        else if ("streamrclient".equals(mController.getType()))
            return mLoader.getContext().getResources()
                    .getString(R.string.i2ptunnel_type_streamrclient);
        else if ("streamrserver".equals(mController.getType()))
            return mLoader.getContext().getResources()
                    .getString(R.string.i2ptunnel_type_streamrserver);
        else if ("httpbidirserver".equals(mController.getType()))
            return mLoader.getContext().getResources()
                    .getString(R.string.i2ptunnel_type_httpbidirserver);
        else
            return mController.getType();
    }

    public String getIfacePort() {
        String host = "";
        String port = "";
        if (isClient()) {
            if ("streamrclient".equals(getType()))
                host = mController.getTargetHost();
            else
                host = mController.getListenOnInterface();
            port = mController.getListenPort();
        } else {
            if ("streamrserver".equals(getType()))
                host = mController.getListenOnInterface();
            else
                host = mController.getTargetHost();
            port = mController.getTargetPort();
            if (host.indexOf(':') >= 0)
                host = '[' + host + ']';
        }
        return host + ":" + port;
    }

    public String getDetails() {
        String details;
        if (isClient()) {
            if ("client".equals(getType()) ||
                    "ircclient".equals(getType()) ||
                    "streamrclient".equals(getType()))
                details = mController.getTargetDestination();
            else
                details = mController.getProxyList();
        } else
            details = "";
        return details;
    }

    public Drawable getStatusIcon() {
        if (mController.getIsRunning()) {
            if (isClient() && mController.getIsStandby())
                return mLoader.getContext().getResources()
                        .getDrawable(R.drawable.local_inprogress);
            else
                return mLoader.getContext().getResources()
                        .getDrawable(R.drawable.local_up);
        } else if (mController.getIsStarting())
            return mLoader.getContext().getResources()
                    .getDrawable(R.drawable.local_inprogress);
        else
            return mLoader.getContext().getResources()
                    .getDrawable(R.drawable.local_down);
    }
}
