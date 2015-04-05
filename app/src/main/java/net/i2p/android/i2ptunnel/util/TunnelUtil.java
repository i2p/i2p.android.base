package net.i2p.android.i2ptunnel.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;

import net.i2p.I2PAppContext;
import net.i2p.android.router.R;
import net.i2p.android.wizard.model.Page;
import net.i2p.i2ptunnel.TunnelController;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.i2ptunnel.ui.GeneralHelper;
import net.i2p.i2ptunnel.ui.TunnelConfig;

public class TunnelUtil extends GeneralHelper {
    public static final String PREFERENCES_FILENAME_PREFIX = "tunnel.";

    public TunnelUtil(I2PAppContext context, TunnelControllerGroup tcg) {
        super(context, tcg);
    }

    public TunnelUtil(TunnelControllerGroup tcg) {
        super(tcg);
    }

    /* General tunnel data for any type */

    public static String getTypeFromName(String typeName, Context ctx) {
        Resources res = ctx.getResources();
        if (res.getString(R.string.i2ptunnel_type_client).equals(typeName))
            return "client";
        else if (res.getString(R.string.i2ptunnel_type_httpclient).equals(typeName))
            return "httpclient";
        else if (res.getString(R.string.i2ptunnel_type_ircclient).equals(typeName))
            return "ircclient";
        else if (res.getString(R.string.i2ptunnel_type_server).equals(typeName))
            return "server";
        else if (res.getString(R.string.i2ptunnel_type_httpserver).equals(typeName))
            return "httpserver";
        else if (res.getString(R.string.i2ptunnel_type_sockstunnel).equals(typeName))
            return "sockstunnel";
        else if (res.getString(R.string.i2ptunnel_type_socksirctunnel).equals(typeName))
            return "socksirctunnel";
        else if (res.getString(R.string.i2ptunnel_type_connectclient).equals(typeName))
            return "connectclient";
        else if (res.getString(R.string.i2ptunnel_type_ircserver).equals(typeName))
            return "ircserver";
        else if (res.getString(R.string.i2ptunnel_type_streamrclient).equals(typeName))
            return "streamrclient";
        else if (res.getString(R.string.i2ptunnel_type_streamrserver).equals(typeName))
            return "streamrserver";
        else if (res.getString(R.string.i2ptunnel_type_httpbidirserver).equals(typeName))
            return "httpbidirserver";
        else
            return typeName;
    }

    public static String getTypeName(String type, Context context) {
        Resources res = context.getResources();
        switch (type) {
            case "client":
                return res.getString(R.string.i2ptunnel_type_client);
            case "httpclient":
                return res.getString(R.string.i2ptunnel_type_httpclient);
            case "ircclient":
                return res.getString(R.string.i2ptunnel_type_ircclient);
            case "server":
                return res.getString(R.string.i2ptunnel_type_server);
            case "httpserver":
                return res.getString(R.string.i2ptunnel_type_httpserver);
            case "sockstunnel":
                return res.getString(R.string.i2ptunnel_type_sockstunnel);
            case "socksirctunnel":
                return res.getString(R.string.i2ptunnel_type_socksirctunnel);
            case "connectclient":
                return res.getString(R.string.i2ptunnel_type_connectclient);
            case "ircserver":
                return res.getString(R.string.i2ptunnel_type_ircserver);
            case "streamrclient":
                return res.getString(R.string.i2ptunnel_type_streamrclient);
            case "streamrserver":
                return res.getString(R.string.i2ptunnel_type_streamrserver);
            case "httpbidirserver":
                return res.getString(R.string.i2ptunnel_type_httpbidirserver);
            default:
                return type;
        }
    }

    public static boolean isClient(String type) {
        return TunnelController.isClient(type);
    }

    public static String getPreferencesFilename(int tunnel) {
        return PREFERENCES_FILENAME_PREFIX + tunnel;
    }

    public static void writeTunnelToPreferences(Context ctx, TunnelControllerGroup tcg, int tunnel) {
        new TunnelUtil(tcg).writeTunnelToPreferences(ctx, tunnel);
    }
    public void writeTunnelToPreferences(Context ctx, int tunnel) {
        Resources res = ctx.getResources();

        if (getController(tunnel) == null)
            throw new IllegalArgumentException("Cannot write non-existent tunnel to Preferences");

        // Get the current preferences for this tunnel
        SharedPreferences preferences = ctx.getSharedPreferences(
                getPreferencesFilename(tunnel), Context.MODE_PRIVATE);

        // Clear all previous values
        SharedPreferences.Editor ed = preferences.edit().clear();

        // Load the tunnel config into the preferences
        String type = getTunnelType(tunnel);
        ed.putString(res.getString(R.string.TUNNEL_TYPE), type);

        new TunnelToPreferences(ed, res, tunnel, type).runLogic();

        ed.apply();
    }

    class TunnelToPreferences extends TunnelLogic {
        SharedPreferences.Editor ed;
        Resources res;
        int tunnel;

        public TunnelToPreferences(SharedPreferences.Editor ed, Resources res, int tunnel, String type) {
            super(type);
            this.ed = ed;
            this.res = res;
            this.tunnel = tunnel;
        }

        @Override
        protected void general() {
            ed.putString(res.getString(R.string.TUNNEL_NAME), getTunnelName(tunnel));
            ed.putString(res.getString(R.string.TUNNEL_DESCRIPTION), getTunnelDescription(tunnel));
            ed.putBoolean(res.getString(R.string.TUNNEL_START_ON_LOAD), shouldStartAutomatically(tunnel));
            if (!isClient(mType) || getPersistentClientKey(tunnel))
                ed.putString(res.getString(R.string.TUNNEL_PRIV_KEY_FILE), getPrivateKeyFile(tunnel));
        }

        @Override
        protected void generalClient() {
            ed.putBoolean(res.getString(R.string.TUNNEL_OPT_PERSISTENT_KEY), getPersistentClientKey(tunnel));
        }

        @Override
        protected void generalClientStreamr(boolean isStreamr) {
            if (isStreamr)
                ed.putString(res.getString(R.string.TUNNEL_TARGET_HOST), getTargetHost(tunnel));
            else
                ed.putBoolean(res.getString(R.string.TUNNEL_SHARED_CLIENT), isSharedClient(tunnel));
        }

        @Override
        protected void generalClientPort() {
            ed.putInt(res.getString(R.string.TUNNEL_LISTEN_PORT), getClientPort(tunnel));
        }

        @Override
        protected void generalClientPortStreamr(boolean isStreamr) {
            if (!isStreamr)
                ed.putString(res.getString(R.string.TUNNEL_INTERFACE), getClientInterface(tunnel));
        }

        @Override
        protected void generalClientProxy(boolean isProxy) {
            if (isProxy)
                ed.putString(res.getString(R.string.TUNNEL_PROXIES), getClientDestination(tunnel));
            else
                ed.putString(res.getString(R.string.TUNNEL_DEST), getClientDestination(tunnel));
        }

        @Override
        protected void generalClientProxyHttp(boolean isHttp) {
            if (isHttp)
                ed.putString(res.getString(R.string.TUNNEL_HTTPCLIENT_SSL_OUTPROXIES), getSslProxies(tunnel));
        }

        @Override
        protected void generalClientStandardOrIrc(boolean isStandardOrIrc) {
            if (isStandardOrIrc)
                ed.putBoolean(res.getString(R.string.TUNNEL_USE_SSL), isSSLEnabled(tunnel));
        }

        @Override
        protected void generalClientIrc() {
            ed.putBoolean(res.getString(R.string.TUNNEL_IRCCLIENT_ENABLE_DCC), getDCC(tunnel));
        }

        @Override
        protected void generalServerHttp() {
            ed.putString(res.getString(R.string.TUNNEL_SPOOFED_HOST), getSpoofedHost(tunnel));
        }

        @Override
        protected void generalServerHttpBidirOrStreamr(boolean isStreamr) {
            ed.putString(res.getString(R.string.TUNNEL_INTERFACE), getClientInterface(tunnel));
            if (!isStreamr)
                ed.putInt(res.getString(R.string.TUNNEL_LISTEN_PORT), getClientPort(tunnel));
        }

        @Override
        protected void generalServerPort() {
            ed.putInt(res.getString(R.string.TUNNEL_TARGET_PORT), getTargetPort(tunnel));
        }

        @Override
        protected void generalServerPortStreamr(boolean isStreamr) {
            if (!isStreamr) {
                ed.putString(res.getString(R.string.TUNNEL_TARGET_HOST), getTargetHost(tunnel));
                ed.putBoolean(res.getString(R.string.TUNNEL_USE_SSL), isSSLEnabled(tunnel));
            }
        }

        @Override
        protected void advanced() {
            ed.putInt(res.getString(R.string.TUNNEL_OPT_LENGTH),
                    getTunnelDepth(tunnel, res.getInteger(R.integer.DEFAULT_TUNNEL_LENGTH)));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_VARIANCE),
                    getTunnelVariance(tunnel, res.getInteger(R.integer.DEFAULT_TUNNEL_VARIANCE)));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_QUANTITY),
                    getTunnelQuantity(tunnel, res.getInteger(R.integer.DEFAULT_TUNNEL_QUANTITY)));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_BACKUP_QUANTITY),
                    getTunnelQuantity(tunnel, res.getInteger(R.integer.DEFAULT_TUNNEL_BACKUP_QUANTITY)));
        }

        @Override
        protected void advancedStreamr(boolean isStreamr) {
            if (!isStreamr)
                ed.putString(res.getString(R.string.TUNNEL_OPT_PROFILE),
                        isInteractive(tunnel) ? "interactive" : "bulk");
        }

        @Override
        protected void advancedServerOrStreamrClient(boolean isServerOrStreamrClient) {
            if (!isServerOrStreamrClient)
                ed.putBoolean(res.getString(R.string.TUNNEL_OPT_DELAY_CONNECT),
                        shouldDelayConnect(tunnel));
        }

        @Override
        protected void advancedServer() {
            //ed.putBoolean(res.getString(R.string.TUNNEL_OPT_ENCRYPT), getEncrypt(tunnel));
            //ed.putString(res.getString(R.string.TUNNEL_OPT_ENCRYPT_KEY), getEncryptKey(tunnel));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_ACCESS_MODE), getAccessMode(tunnel));
            ed.putString(res.getString(R.string.TUNNEL_OPT_ACCESS_LIST), getAccessList(tunnel));
            ed.putBoolean(res.getString(R.string.TUNNEL_OPT_UNIQUE_LOCAL), getUniqueLocal(tunnel));
            ed.putBoolean(res.getString(R.string.TUNNEL_OPT_MULTIHOME), getMultihome(tunnel));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_LIMIT_MINUTE), getLimitMinute(tunnel));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_LIMIT_HOUR), getLimitHour(tunnel));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_LIMIT_DAY), getLimitDay(tunnel));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_TOTAL_MINUTE), getTotalMinute(tunnel));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_TOTAL_HOUR), getTotalHour(tunnel));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_TOTAL_DAY), getTotalDay(tunnel));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_MAX_STREAMS), getMaxStreams(tunnel));
        }

        @Override
        protected void advancedServerHttp(boolean isHttp) {
            if (isHttp) {
                ed.putBoolean(res.getString(R.string.TUNNEL_OPT_REJECT_INPROXY), getRejectInproxy(tunnel));
                ed.putInt(res.getString(R.string.TUNNEL_OPT_POST_CHECK_TIME), getPostCheckTime(tunnel));
                ed.putInt(res.getString(R.string.TUNNEL_OPT_POST_MAX), getPostMax(tunnel));
                ed.putInt(res.getString(R.string.TUNNEL_OPT_POST_BAN_TIME), getPostBanTime(tunnel));
                ed.putInt(res.getString(R.string.TUNNEL_OPT_POST_TOTAL_MAX), getPostTotalMax(tunnel));
                ed.putInt(res.getString(R.string.TUNNEL_OPT_POST_TOTAL_BAN_TIME), getPostTotalBanTime(tunnel));
            }
        }

        @Override
        protected void advancedIdle() {
            ed.putBoolean(res.getString(R.string.TUNNEL_OPT_REDUCE_IDLE),
                    getReduceOnIdle(tunnel, res.getBoolean(R.bool.DEFAULT_REDUCE_ON_IDLE)));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_REDUCE_QUANTITY),
                    getReduceCount(tunnel, res.getInteger(R.integer.DEFAULT_REDUCE_COUNT)));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_REDUCE_TIME),
                    getReduceTime(tunnel, res.getInteger(R.integer.DEFAULT_REDUCE_TIME)));
        }

        @Override
        protected void advancedIdleServerOrStreamrClient(boolean isServerOrStreamrClient) {
            if (!isServerOrStreamrClient)
                ed.putBoolean(res.getString(R.string.TUNNEL_OPT_DELAY_OPEN), getDelayOpen(tunnel));
        }

        @Override
        protected void advancedClient() {
            ed.putBoolean(res.getString(R.string.TUNNEL_OPT_CLOSE_IDLE),
                    getCloseOnIdle(tunnel, res.getBoolean(R.bool.DEFAULT_CLOSE_ON_IDLE)));
            ed.putInt(res.getString(R.string.TUNNEL_OPT_CLOSE_TIME),
                    getCloseTime(tunnel, res.getInteger(R.integer.DEFAULT_CLOSE_TIME)));
            ed.putBoolean(res.getString(R.string.TUNNEL_OTP_NEW_KEYS), getNewDest(tunnel));
        }

        @Override
        protected void advancedClientHttp() {
            ed.putBoolean(res.getString(R.string.TUNNEL_OPT_HTTPCLIENT_PASS_UA), getAllowUserAgent(tunnel));
            ed.putBoolean(res.getString(R.string.TUNNEL_OPT_HTTPCLIENT_PASS_REFERER), getAllowReferer(tunnel));
            ed.putBoolean(res.getString(R.string.TUNNEL_OPT_HTTPCLIENT_PASS_ACCEPT), getAllowAccept(tunnel));
            ed.putBoolean(res.getString(R.string.TUNNEL_OPT_HTTPCLIENT_ALLOW_SSL), getAllowInternalSSL(tunnel));
            ed.putString(res.getString(R.string.TUNNEL_OPT_JUMP_LIST), getJumpList(tunnel));
        }

        @Override
        protected void advancedClientProxy() {
            ed.putBoolean(res.getString(R.string.TUNNEL_OPT_LOCAL_AUTH), !"false".equals(getProxyAuth(tunnel)));
            ed.putString(res.getString(R.string.TUNNEL_OPT_LOCAL_USERNAME), "");
            ed.putString(res.getString(R.string.TUNNEL_OPT_LOCAL_PASSWORD), "");
            ed.putBoolean(res.getString(R.string.TUNNEL_OPT_OUTPROXY_AUTH), getOutproxyAuth(tunnel));
            ed.putString(res.getString(R.string.TUNNEL_OPT_OUTPROXY_USERNAME), getOutproxyUsername(tunnel));
            ed.putString(res.getString(R.string.TUNNEL_OPT_OUTPROXY_PASSWORD), getOutproxyPassword(tunnel));
        }

        @Override
        protected void advancedOther() {
            ed.putInt(res.getString(R.string.TUNNEL_OPT_SIGTYPE), getSigType(tunnel, mType));
            ed.putString(res.getString(R.string.TUNNEL_OPT_CUSTOM_OPTIONS), getCustomOptionsString(tunnel));
        }
    }

    public static TunnelConfig createConfigFromPreferences(Context ctx, TunnelControllerGroup tcg, int tunnel) {
        return new TunnelUtil(tcg).createConfigFromPreferences(ctx, tunnel);
    }
    public TunnelConfig createConfigFromPreferences(Context ctx, int tunnel) {
        Resources res = ctx.getResources();

        // Get the current preferences for this tunnel
        SharedPreferences prefs = ctx.getSharedPreferences(
                getPreferencesFilename(tunnel), Context.MODE_PRIVATE);

        // Create the TunnelConfig
        TunnelConfig cfg = new TunnelConfig();

        // Update the TunnelConfig from the preferences
        cfg.setType(prefs.getString(res.getString(R.string.TUNNEL_TYPE), null));
        String type = cfg.getType();

        new TunnelConfigFromPreferences(cfg, prefs, res, _group, tunnel, type).runLogic();

        return cfg;
    }

    class TunnelConfigFromPreferences extends TunnelLogic {
        TunnelConfig cfg;
        SharedPreferences prefs;
        Resources res;
        TunnelControllerGroup tcg;
        int tunnel;

        public TunnelConfigFromPreferences(TunnelConfig cfg, SharedPreferences prefs, Resources res,
                                           TunnelControllerGroup tcg, int tunnel, String type) {
            super(type);
            this.cfg = cfg;
            this.prefs = prefs;
            this.res = res;
            this.tcg = tcg;
            this.tunnel = tunnel;
        }

        @Override
        protected void general() {
            cfg.setName(prefs.getString(res.getString(R.string.TUNNEL_NAME), null));
            cfg.setDescription(prefs.getString(res.getString(R.string.TUNNEL_DESCRIPTION), null));
            cfg.setStartOnLoad(prefs.getBoolean(res.getString(R.string.TUNNEL_START_ON_LOAD),
                    res.getBoolean(R.bool.DEFAULT_START_ON_LOAD)));
            if (!isClient(mType) || prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_PERSISTENT_KEY),
                    res.getBoolean(R.bool.DEFAULT_PERSISTENT_KEY)))
                cfg.setPrivKeyFile(prefs.getString(res.getString(R.string.TUNNEL_PRIV_KEY_FILE),
                        getPrivateKeyFile(tcg, tunnel)));
        }

        @Override
        protected void generalClient() {
            // See advancedClient() for persistent key handling
        }

        @Override
        protected void generalClientStreamr(boolean isStreamr) {
            if (isStreamr)
                cfg.setTargetHost(prefs.getString(res.getString(R.string.TUNNEL_TARGET_HOST), null));
            else
                cfg.setShared(prefs.getBoolean(res.getString(R.string.TUNNEL_SHARED_CLIENT),
                        res.getBoolean(R.bool.DEFAULT_SHARED_CLIENTS)));
        }

        @Override
        protected void generalClientPort() {
            cfg.setPort(prefs.getInt(res.getString(R.string.TUNNEL_LISTEN_PORT), -1));
        }

        @Override
        protected void generalClientPortStreamr(boolean isStreamr) {
            if (!isStreamr)
                cfg.setReachableBy(prefs.getString(res.getString(R.string.TUNNEL_INTERFACE), "127.0.0.1"));
        }

        @Override
        protected void generalClientProxy(boolean isProxy) {
            if (isProxy)
                cfg.setProxyList(prefs.getString(res.getString(R.string.TUNNEL_PROXIES), null));
            else
                cfg.setTargetDestination(prefs.getString(res.getString(R.string.TUNNEL_DEST), null));
        }

        @Override
        protected void generalClientProxyHttp(boolean isHttp) {
            if (isHttp)
                cfg.setSslProxies(prefs.getString(res.getString(R.string.TUNNEL_HTTPCLIENT_SSL_OUTPROXIES), null));
        }

        @Override
        protected void generalClientStandardOrIrc(boolean isStandardOrIrc) {
            if (isStandardOrIrc)
                cfg.setUseSSL(prefs.getBoolean(res.getString(R.string.TUNNEL_USE_SSL), false));
        }

        @Override
        protected void generalClientIrc() {
            cfg.setDCC(prefs.getBoolean(res.getString(R.string.TUNNEL_IRCCLIENT_ENABLE_DCC), false));
        }

        @Override
        protected void generalServerHttp() {
            cfg.setSpoofedHost(prefs.getString(res.getString(R.string.TUNNEL_SPOOFED_HOST), null));
        }

        @Override
        protected void generalServerHttpBidirOrStreamr(boolean isStreamr) {
            cfg.setReachableBy(prefs.getString(res.getString(R.string.TUNNEL_INTERFACE), "127.0.0.1"));
            if (!isStreamr)
                cfg.setPort(prefs.getInt(res.getString(R.string.TUNNEL_LISTEN_PORT), -1));
        }

        @Override
        protected void generalServerPort() {
            cfg.setTargetPort(prefs.getInt(res.getString(R.string.TUNNEL_TARGET_PORT), -1));
        }

        @Override
        protected void generalServerPortStreamr(boolean isStreamr) {
            if (!isStreamr) {
                cfg.setTargetHost(prefs.getString(res.getString(R.string.TUNNEL_TARGET_HOST), "127.0.0.1"));
                cfg.setUseSSL(prefs.getBoolean(res.getString(R.string.TUNNEL_USE_SSL), false));
            }
        }

        @Override
        protected void advanced() {
            cfg.setTunnelDepth(prefs.getInt(res.getString(R.string.TUNNEL_OPT_LENGTH),
                    res.getInteger(R.integer.DEFAULT_TUNNEL_LENGTH)));
            cfg.setTunnelVariance(prefs.getInt(res.getString(R.string.TUNNEL_OPT_VARIANCE),
                    res.getInteger(R.integer.DEFAULT_TUNNEL_VARIANCE)));
            cfg.setTunnelQuantity(prefs.getInt(res.getString(R.string.TUNNEL_OPT_QUANTITY),
                    res.getInteger(R.integer.DEFAULT_TUNNEL_QUANTITY)));
            cfg.setTunnelBackupQuantity(prefs.getInt(res.getString(R.string.TUNNEL_OPT_BACKUP_QUANTITY),
                    res.getInteger(R.integer.DEFAULT_TUNNEL_BACKUP_QUANTITY)));
        }

        @Override
        protected void advancedStreamr(boolean isStreamr) {
            if (!isStreamr)
                cfg.setProfile(prefs.getString(res.getString(R.string.TUNNEL_OPT_PROFILE), "bulk"));
        }

        @Override
        protected void advancedServerOrStreamrClient(boolean isServerOrStreamrClient) {
            if (!isServerOrStreamrClient)
                cfg.setConnectDelay(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_DELAY_CONNECT), false));
        }

        @Override
        protected void advancedServer() {
            //cfg.setEncrypt(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_ENCRYPT), false));
            //cfg.setEncryptKey(prefs.getString(res.getString(R.string.TUNNEL_OPT_ENCRYPT_KEY), ""));
            cfg.setAccessMode(prefs.getInt(res.getString(R.string.TUNNEL_OPT_ACCESS_MODE), 0));
            cfg.setAccessList(prefs.getString(res.getString(R.string.TUNNEL_OPT_ACCESS_LIST), ""));
            cfg.setUniqueLocal(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_UNIQUE_LOCAL), false));
            cfg.setMultihome(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_MULTIHOME), false));
            cfg.setLimitMinute(prefs.getInt(res.getString(R.string.TUNNEL_OPT_LIMIT_MINUTE), 0));
            cfg.setLimitHour(prefs.getInt(res.getString(R.string.TUNNEL_OPT_LIMIT_HOUR), 0));
            cfg.setLimitDay(prefs.getInt(res.getString(R.string.TUNNEL_OPT_LIMIT_DAY), 0));
            cfg.setTotalMinute(prefs.getInt(res.getString(R.string.TUNNEL_OPT_TOTAL_MINUTE), 0));
            cfg.setTotalHour(prefs.getInt(res.getString(R.string.TUNNEL_OPT_TOTAL_HOUR), 0));
            cfg.setTotalDay(prefs.getInt(res.getString(R.string.TUNNEL_OPT_TOTAL_DAY), 0));
            cfg.setMaxStreams(prefs.getInt(res.getString(R.string.TUNNEL_OPT_MAX_STREAMS), 0));
        }

        @Override
        protected void advancedServerHttp(boolean isHttp) {
            if (isHttp) {
                cfg.setRejectInproxy(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_REJECT_INPROXY), false));
                cfg.setPostCheckTime(prefs.getInt(res.getString(R.string.TUNNEL_OPT_POST_CHECK_TIME), 0));
                cfg.setPostMax(prefs.getInt(res.getString(R.string.TUNNEL_OPT_POST_MAX), 0));
                cfg.setPostBanTime(prefs.getInt(res.getString(R.string.TUNNEL_OPT_POST_BAN_TIME), 0));
                cfg.setPostTotalMax(prefs.getInt(res.getString(R.string.TUNNEL_OPT_POST_TOTAL_MAX), 0));
                cfg.setPostTotalBanTime(prefs.getInt(res.getString(R.string.TUNNEL_OPT_POST_TOTAL_BAN_TIME), 0));
            }
        }

        @Override
        protected void advancedIdle() {
            cfg.setReduce(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_REDUCE_IDLE),
                    res.getBoolean(R.bool.DEFAULT_REDUCE_ON_IDLE)));
            cfg.setReduceCount(prefs.getInt(res.getString(R.string.TUNNEL_OPT_REDUCE_QUANTITY),
                    res.getInteger(R.integer.DEFAULT_REDUCE_COUNT)));
            cfg.setReduceTime(prefs.getInt(res.getString(R.string.TUNNEL_OPT_REDUCE_TIME),
                    res.getInteger(R.integer.DEFAULT_REDUCE_TIME)));
        }

        @Override
        protected void advancedIdleServerOrStreamrClient(boolean isServerOrStreamrClient) {
            if (!isServerOrStreamrClient)
                cfg.setDelayOpen(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_DELAY_OPEN),
                        res.getBoolean(R.bool.DEFAULT_DELAY_OPEN)));
        }

        @Override
        protected void advancedClient() {
            cfg.setClose(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_CLOSE_IDLE),
                    res.getBoolean(R.bool.DEFAULT_CLOSE_ON_IDLE)));
            cfg.setCloseTime(prefs.getInt(res.getString(R.string.TUNNEL_OPT_CLOSE_TIME),
                    res.getInteger(R.integer.DEFAULT_CLOSE_TIME)));
            cfg.setNewDest(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_PERSISTENT_KEY),
                    res.getBoolean(R.bool.DEFAULT_PERSISTENT_KEY)) ? 2 :
                    prefs.getBoolean(res.getString(R.string.TUNNEL_OTP_NEW_KEYS), res.getBoolean(R.bool.DEFAULT_NEW_KEYS)) ? 1 : 0);
        }

        @Override
        protected void advancedClientHttp() {
            cfg.setAllowUserAgent(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_HTTPCLIENT_PASS_UA), false));
            cfg.setAllowReferer(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_HTTPCLIENT_PASS_REFERER), false));
            cfg.setAllowAccept(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_HTTPCLIENT_PASS_ACCEPT), false));
            cfg.setAllowInternalSSL(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_HTTPCLIENT_ALLOW_SSL), false));
            cfg.setJumpList(prefs.getString(res.getString(R.string.TUNNEL_OPT_JUMP_LIST),
                    res.getString(R.string.DEFAULT_JUMP_LIST)));
        }

        @Override
        protected void advancedClientProxy() {
            cfg.setProxyAuth(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_LOCAL_AUTH), false) ? "digest" : "false");
            String username = prefs.getString(res.getString(R.string.TUNNEL_OPT_LOCAL_USERNAME), "");
            if (!username.isEmpty()) {
                cfg.setProxyUsername(username);
                cfg.setProxyPassword(prefs.getString(res.getString(R.string.TUNNEL_OPT_LOCAL_PASSWORD), ""));
            }
            cfg.setOutproxyAuth(prefs.getBoolean(res.getString(R.string.TUNNEL_OPT_OUTPROXY_AUTH), false));
            cfg.setOutproxyUsername(prefs.getString(res.getString(R.string.TUNNEL_OPT_OUTPROXY_USERNAME), ""));
            cfg.setOutproxyPassword(prefs.getString(res.getString(R.string.TUNNEL_OPT_OUTPROXY_PASSWORD), ""));
        }

        @Override
        protected void advancedOther() {
            cfg.setSigType(Integer.toString(prefs.getInt(res.getString(R.string.TUNNEL_OPT_SIGTYPE),
                    res.getInteger(R.integer.DEFAULT_SIGTYPE))));
            cfg.setCustomOptions(prefs.getString(res.getString(R.string.TUNNEL_OPT_CUSTOM_OPTIONS), null));
        }
    }

    public static TunnelConfig createConfigFromWizard(
            Context ctx, TunnelControllerGroup tcg, Bundle data) {
        // Get the Bundle keys
        Resources res = ctx.getResources();

        String kClientServer = res.getString(R.string.i2ptunnel_wizard_k_client_server);
        String kType = res.getString(R.string.i2ptunnel_wizard_k_type);

        String kName = res.getString(R.string.i2ptunnel_wizard_k_name);
        String kDesc = res.getString(R.string.i2ptunnel_wizard_k_desc);
        String kDest = res.getString(R.string.i2ptunnel_wizard_k_dest);
        String kOutproxies = res.getString(R.string.i2ptunnel_wizard_k_outproxies);
        String kTargetHost = res.getString(R.string.i2ptunnel_wizard_k_target_host);
        String kTargetPort = res.getString(R.string.i2ptunnel_wizard_k_target_port);
        String kReachableOn = res.getString(R.string.i2ptunnel_wizard_k_reachable_on);
        String kBindingPort = res.getString(R.string.i2ptunnel_wizard_k_binding_port);
        String kAutoStart = res.getString(R.string.i2ptunnel_wizard_k_auto_start);

        // Create the TunnelConfig
        TunnelConfig cfg = new TunnelConfig();

        // Get/set the tunnel wizard settings
        String clientServer = data.getBundle(kClientServer).getString(Page.SIMPLE_DATA_KEY);
        String typeName = data.getBundle(clientServer + ":" + kType).getString(Page.SIMPLE_DATA_KEY);
        String type = getTypeFromName(typeName, ctx);
        cfg.setType(type);

        String name = data.getBundle(kName).getString(Page.SIMPLE_DATA_KEY);
        cfg.setName(name);

        String desc = data.getBundle(kDesc).getString(Page.SIMPLE_DATA_KEY);
        cfg.setDescription(desc);

        String dest = null;
        Bundle pageData = data.getBundle(kDest);
        if (pageData != null) dest = pageData.getString(Page.SIMPLE_DATA_KEY);
        cfg.setTargetDestination(dest);

        String outproxies = null;
        pageData = data.getBundle(kOutproxies);
        if (pageData != null) outproxies = pageData.getString(Page.SIMPLE_DATA_KEY);
        cfg.setProxyList(outproxies);

        String targetHost = null;
        pageData = data.getBundle(kTargetHost);
        if (pageData != null) targetHost = pageData.getString(Page.SIMPLE_DATA_KEY);
        cfg.setTargetHost(targetHost);

        int targetPort = -1;
        pageData = data.getBundle(kTargetPort);
        if (pageData != null) targetPort = pageData.getInt(Page.SIMPLE_DATA_KEY);
        cfg.setTargetPort(targetPort);

        String reachableOn = null;
        pageData = data.getBundle(kReachableOn);
        if (pageData != null) reachableOn = pageData.getString(Page.SIMPLE_DATA_KEY);
        cfg.setReachableBy(reachableOn);

        int bindingPort = -1;
        pageData = data.getBundle(kBindingPort);
        if (pageData != null) bindingPort = pageData.getInt(Page.SIMPLE_DATA_KEY);
        cfg.setPort(bindingPort);

        boolean autoStart = data.getBundle(kAutoStart).getBoolean(Page.SIMPLE_DATA_KEY);
        cfg.setStartOnLoad(autoStart);

        // Set sensible defaults for a new tunnel
        cfg.setTunnelDepth(res.getInteger(R.integer.DEFAULT_TUNNEL_LENGTH));
        cfg.setTunnelVariance(res.getInteger(R.integer.DEFAULT_TUNNEL_VARIANCE));
        cfg.setTunnelQuantity(res.getInteger(R.integer.DEFAULT_TUNNEL_QUANTITY));
        cfg.setTunnelBackupQuantity(res.getInteger(R.integer.DEFAULT_TUNNEL_BACKUP_QUANTITY));
        cfg.setClientHost("internal");
        cfg.setClientPort("internal");
        cfg.setCustomOptions("");
        if (!"streamrclient".equals(type)) {
            cfg.setProfile("bulk");
            cfg.setReduceCount(1);
            cfg.setReduceTime(20);
        }
        if (isClient(type)) { /* Client-only defaults */
            if (!"streamrclient".equals(type)) {
                cfg.setNewDest(0);
                cfg.setCloseTime(30);
            }
            if ("httpclient".equals(type) ||
                    "connectclient".equals(type) ||
                    "sockstunnel".equals(type) |
                            "socksirctunnel".equals(type)) {
                cfg.setProxyAuth("false");
                cfg.setProxyUsername("");
                cfg.setProxyPassword("");
                cfg.setOutproxyAuth(false);
                cfg.setOutproxyUsername("");
                cfg.setOutproxyPassword("");
            }
            if ("httpclient".equals(type))
                cfg.setJumpList(res.getString(R.string.DEFAULT_JUMP_LIST));
        } else { /* Server-only defaults */
            cfg.setPrivKeyFile(getPrivateKeyFile(tcg, -1));
            cfg.setEncrypt(false);
            cfg.setAccessMode(0);
            cfg.setLimitMinute(0);
            cfg.setLimitHour(0);
            cfg.setLimitDay(0);
            cfg.setTotalMinute(0);
            cfg.setTotalHour(0);
            cfg.setTotalDay(0);
            cfg.setMaxStreams(0);
        }

        return cfg;
    }
}
