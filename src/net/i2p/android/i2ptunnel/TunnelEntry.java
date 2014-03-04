package net.i2p.android.i2ptunnel;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.Toast;
import net.i2p.android.i2ptunnel.util.TunnelConfig;
import net.i2p.android.i2ptunnel.util.TunnelUtil;
import net.i2p.android.router.R;
import net.i2p.data.Destination;
import net.i2p.data.PrivateKeyFile;
import net.i2p.i2ptunnel.TunnelController;
import net.i2p.i2ptunnel.TunnelControllerGroup;

public class TunnelEntry {
    public static final int RUNNING = 1;
    public static final int STARTING = 2;
    public static final int NOT_RUNNING = 3;
    public static final int STANDBY = 4;

    private final Context mContext;
    private final TunnelController mController;
    private final int mId;

    public static TunnelEntry createNewTunnel(
            Context ctx,
            TunnelControllerGroup tcg,
            TunnelConfig cfg) {
        int tunnelId = tcg.getControllers().size();
        List<String> msgs = TunnelUtil.saveTunnel(
                ctx, tcg, -1, cfg.getConfig());
        // TODO: Do something else with the other messages.
        Toast.makeText(ctx.getApplicationContext(),
                msgs.get(0), Toast.LENGTH_LONG).show();
        TunnelController cur = TunnelUtil.getController(tcg, tunnelId);
        return new TunnelEntry(ctx, cur, tunnelId);
    }

    public TunnelEntry(Context context, TunnelController controller, int id) {
        mContext = context;
        mController = controller;
        mId = id;
    }

    public int getId() {
        return mId;
    }

    public TunnelController getController() {
        return mController;
    }

    /* General tunnel data for any type */

    public String getName() {
        if (mController.getName() != null)
            return mController.getName();
        else
            return mContext.getResources()
                    .getString(R.string.i2ptunnel_new_tunnel);
    }

    public String getInternalType() {
        return mController.getType();
    }

    public String getType() {
        return TunnelUtil.getTypeName(mController.getType(), mContext);
    }

    public String getDescription() {
        String rv = mController.getDescription();
        if (rv != null)
            return rv;
        return "";
    }

    public boolean startAutomatically() {
        return mController.getStartOnLoad();
    }

    public int getStatus() {
        if (mController.getIsRunning()) {
            if (isClient() && mController.getIsStandby())
                return STANDBY;
            else
                return RUNNING;
        } else if (mController.getIsStarting()) return STARTING;
        else return NOT_RUNNING;
    }

    public boolean isClient() {
        return TunnelUtil.isClient(mController.getType());
    }

    /* Client tunnel data */

    public boolean isSharedClient() {
        return Boolean.parseBoolean(mController.getSharedClient());
    }

    public String getClientInterface() {
        if ("streamrclient".equals(mController.getType()))
            return mController.getTargetHost();
        else
            return mController.getListenOnInterface();
    }

    public String getClientPort() {
        String rv = mController.getListenPort();
        if (rv != null)
            return rv;
        return "";
    }

    public String getClientDestination() {
        String rv;
        if ("client".equals(getInternalType()) ||
                "ircclient".equals(getInternalType()) ||
                "streamrclient".equals(getInternalType()))
            rv = mController.getTargetDestination();
        else
            rv = mController.getProxyList();
        return rv != null ? rv : "";
    }

    /* Server tunnel data */

    /**
     * Call this to see if it is okay to linkify getServerTarget()
     * @return true if getServerTarget() can be linkified, false otherwise.
     */
    public boolean isServerTargetLinkValid() {
        return ("httpserver".equals(mController.getType()) ||
                "httpbidirserver".equals(mController.getType())) &&
                mController.getTargetHost() != null &&
                mController.getTargetPort() != null;
    }

    /**
     * @return valid host:port only if isServerTargetLinkValid() is true
     */
    public String getServerTarget() {
        String host;
        if ("streamrserver".equals(getInternalType()))
            host = mController.getListenOnInterface();
        else
            host = mController.getTargetHost();
        String port = mController.getTargetPort();
        if (host == null) host = "";
        if (port == null) port = "";
        if (host.indexOf(':') >= 0)
            host = '[' + host + ']';
        return host + ":" + port;
    }

    public String getDestinationBase64() {
        String rv = mController.getMyDestination();
        if (rv != null)
            return rv;
        // if not running, do this the hard way
        String keyFile = mController.getPrivKeyFile();
        if (keyFile != null && keyFile.trim().length() > 0) {
            PrivateKeyFile pkf = new PrivateKeyFile(keyFile);
            try {
                Destination d = pkf.getDestination();
                if (d != null)
                    return d.toBase64();
            } catch (Exception e) {}
        }
        return "";
    }

    public String getDestHashBase32() {
        String rv = mController.getMyDestHashBase32();
        if (rv != null)
            return rv;
        return "";
    }

    /* Data for some client and server tunnels */

    /* Other output formats */

    public String getIfacePort() {
        if (isClient()) {
            String host;
            if ("streamrclient".equals(getInternalType()))
                host = mController.getTargetHost();
            else
                host = mController.getListenOnInterface();
            String port = mController.getListenPort();
            if (host == null) host = "";
            if (port == null) port = "";
            return host + ":" + port;
        } else return getServerTarget();
    }

    public String getDetails() {
        String details;
        if (isClient())
            details = getClientDestination();
        else
            details = "";
        return details;
    }

    public Drawable getStatusIcon() {
        switch (getStatus()) {
        case STANDBY:
        case STARTING:
            return mContext.getResources()
                    .getDrawable(R.drawable.local_inprogress);
        case RUNNING:
            return mContext.getResources()
                    .getDrawable(R.drawable.local_up);
        case NOT_RUNNING:
        default:
            return mContext.getResources()
                    .getDrawable(R.drawable.local_down);
        }
    }
}
