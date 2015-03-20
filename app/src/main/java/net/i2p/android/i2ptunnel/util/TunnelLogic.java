package net.i2p.android.i2ptunnel.util;

/**
 * Generic class for handling the composition of tunnel properties.
 * <p/>
 * See I2PTunnel's editClient.jsp and editServer.jsp for composition logic.
 * <p/>
 * Some of the abstract methods have boolean parameters. These are the methods
 * where the corresponding tunnel properties may or may not exist, depending on
 * the value of the boolean. In all other abstract methods, all corresponding
 * tunnel properties always exist.
 */
public abstract class TunnelLogic {
    protected String mType;

    public TunnelLogic(String type) {
        mType = type;
    }

    public void runLogic() {
        boolean isProxy = "httpclient".equals(mType) ||
                "connectclient".equals(mType) ||
                "sockstunnel".equals(mType) ||
                "socksirctunnel".equals(mType);

        general();

        if (TunnelUtil.isClient(mType)) {
            generalClient();
            generalClientStreamr("streamrclient".equals(mType));

            generalClientPort();
            generalClientPortStreamr("streamrclient".equals(mType));

            generalClientProxy(isProxy);
            if (isProxy)
                generalClientProxyHttp("httpclient".equals(mType));

            generalClientStandardOrIrc("client".equals(mType) || "ircclient".equals(mType));
            if ("ircclient".equals(mType))
                generalClientIrc();
        } else {
            if ("httpserver".equals(mType) || "httpbidirserver".equals(mType))
                generalServerHttp();
            generalServerHttpBidirOrStreamr("httpbidirserver".equals(mType) || "streamrserver".equals(mType));

            generalServerPort();
            generalServerPortStreamr("streamrserver".equals(mType));
        }

        advanced();
        advancedStreamr("streamrclient".equals(mType) || "streamrserver".equals(mType));
        advancedServerOrStreamrClient(!TunnelUtil.isClient(mType) || "streamrclient".equals(mType));

        if (!TunnelUtil.isClient(mType)) {
            advancedServer();
            advancedServerHttp("httpserver".equals(mType) || "httpbidirserver".equals(mType));
        }

        advancedIdle();
        // streamr client sends pings so it will never be idle
        advancedIdleServerOrStreamrClient(!TunnelUtil.isClient(mType) || "streamrclient".equals(mType));

        if (TunnelUtil.isClient(mType)) {
            advancedClient();
            if ("httpclient".equals(mType))
                advancedClientHttp();
            if (isProxy)
                advancedClientProxy();
        }

        advancedOther();
    }

    protected abstract void general();
    protected abstract void generalClient();
    protected abstract void generalClientStreamr(boolean isStreamr);
    protected abstract void generalClientPort();
    protected abstract void generalClientPortStreamr(boolean isStreamr);
    protected abstract void generalClientProxy(boolean isProxy);
    protected abstract void generalClientProxyHttp(boolean isHttp);
    protected abstract void generalClientStandardOrIrc(boolean isStandardOrIrc);
    protected abstract void generalClientIrc();
    protected abstract void generalServerHttp();
    protected abstract void generalServerHttpBidirOrStreamr(boolean isStreamr);
    protected abstract void generalServerPort();
    protected abstract void generalServerPortStreamr(boolean isStreamr);

    protected abstract void advanced();
    protected abstract void advancedStreamr(boolean isStreamr);
    protected abstract void advancedServerOrStreamrClient(boolean isServerOrStreamrClient);
    protected abstract void advancedServer();
    protected abstract void advancedServerHttp(boolean isHttp);
    protected abstract void advancedIdle();
    protected abstract void advancedIdleServerOrStreamrClient(boolean isServerOrStreamrClient);
    protected abstract void advancedClient();
    protected abstract void advancedClientHttp();
    protected abstract void advancedClientProxy();
    protected abstract void advancedOther();
}
