package net.i2p.android.i2ptunnel.util;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import net.i2p.I2PAppContext;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import net.i2p.android.wizard.model.Page;
import net.i2p.i2ptunnel.I2PTunnelHTTPServer;
import net.i2p.i2ptunnel.I2PTunnelServer;
import net.i2p.i2ptunnel.TunnelController;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.i2ptunnel.ui.TunnelConfig;
import net.i2p.util.FileUtil;
import net.i2p.util.SecureFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public abstract class TunnelUtil {
    public static TunnelController getController(TunnelControllerGroup tcg, int tunnel) {
        if (tunnel < 0) return null;
        if (tcg == null) return null;
        List<TunnelController> controllers = tcg.getControllers();
        if (controllers.size() > tunnel)
            return controllers.get(tunnel); 
        else
            return null;
    }

    public static List<String> saveTunnel(Context ctx,
            TunnelControllerGroup tcg,
            int tunnelId,
            Properties config) {
        // Get current tunnel controller
        TunnelController cur = getController(tcg, tunnelId);

        if (config == null) {
            List<String> ret = new ArrayList<>();
            ret.add("Invalid params");
            return ret;
        }
        
        if (cur == null) {
            // creating new
            cur = new TunnelController(config, "", true);
            tcg.addController(cur);
            if (cur.getStartOnLoad())
                cur.startTunnelBackground();
        } else {
            cur.setConfig(config, "");
        }
        // Only modify other shared tunnels
        // if the current tunnel is shared, and of supported type
        if (Boolean.parseBoolean(cur.getSharedClient()) && isClient(cur.getType())) {
            // all clients use the same I2CP session, and as such, use the same I2CP options
            List<TunnelController> controllers = tcg.getControllers();

            for (int i = 0; i < controllers.size(); i++) {
                TunnelController c = controllers.get(i);

                // Current tunnel modified by user, skip
                if (c == cur) continue;

                // Only modify this non-current tunnel
                // if it belongs to a shared destination, and is of supported type
                if (Boolean.parseBoolean(c.getSharedClient()) && isClient(c.getType())) {
                    Properties cOpt = c.getConfig("");
                    if (config.getProperty("option.inbound.quantity") != null)
                        cOpt.setProperty("option.inbound.quantity", config.getProperty("option.inbound.quantity"));
                    if (config.getProperty("option.outbound.quantity") != null)
                        cOpt.setProperty("option.outbound.quantity", config.getProperty("option.outbound.quantity"));
                    if (config.getProperty("option.inbound.length") != null)
                        cOpt.setProperty("option.inbound.length", config.getProperty("option.inbound.length"));
                    if (config.getProperty("option.outbound.length") != null)
                        cOpt.setProperty("option.outbound.length", config.getProperty("option.outbound.length"));
                    if (config.getProperty("option.inbound.lengthVariance") != null)
                        cOpt.setProperty("option.inbound.lengthVariance", config.getProperty("option.inbound.lengthVariance"));
                    if (config.getProperty("option.outbound.lengthVariance") != null)
                        cOpt.setProperty("option.outbound.lengthVariance", config.getProperty("option.outbound.lengthVariance"));
                    if (config.getProperty("option.inbound.backupQuantity") != null)
                        cOpt.setProperty("option.inbound.backupQuantity", config.getProperty("option.inbound.backupQuantity"));
                    if (config.getProperty("option.outbound.backupQuantity") != null)
                        cOpt.setProperty("option.outbound.backupQuantity", config.getProperty("option.outbound.backupQuantity"));
                    cOpt.setProperty("option.inbound.nickname", TunnelConfig.SHARED_CLIENT_NICKNAME);
                    cOpt.setProperty("option.outbound.nickname", TunnelConfig.SHARED_CLIENT_NICKNAME);
                    
                    c.setConfig(cOpt, "");
                }
            }
        }

        return doSave(ctx, tcg);
    }

    /**
     *  Stop the tunnel, delete from config,
     *  rename the private key file if in the default directory
     */
    public static List<String> deleteTunnel(Context ctx, TunnelControllerGroup tcg, int tunnelId) {
        List<String> msgs;        
        TunnelController cur = getController(tcg, tunnelId);
        if (cur == null) {
            msgs = new ArrayList<>();
            msgs.add("Invalid tunnel number");
            return msgs;
        }
        
        msgs = tcg.removeController(cur);
        msgs.addAll(doSave(ctx, tcg));

        // Rename private key file if it was a default name in
        // the default directory, so it doesn't get reused when a new
        // tunnel is created.
        // Use configured file name if available, not the one from the form.
        String pk = cur.getPrivKeyFile();
        //if (pk == null)
        //    pk = _privKeyFile;
        if (pk != null && pk.startsWith("i2ptunnel") && pk.endsWith("-privKeys.dat") &&
            ((!isClient(cur.getType())) || cur.getPersistentClientKey())) {
            I2PAppContext context = I2PAppContext.getGlobalContext();
            File pkf = new File(context.getConfigDir(), pk);
            if (pkf.exists()) {
                String name = cur.getName();
                if (name == null) {
                    name = cur.getDescription();
                    if (name == null) {
                        name = cur.getType();
                        if (name == null)
                            name = Long.toString(context.clock().now());
                    }
                }
                name = "i2ptunnel-deleted-" + name.replace(' ', '_') + '-' + context.clock().now() + "-privkeys.dat";
                File backupDir = new SecureFile(context.getConfigDir(), TunnelController.KEY_BACKUP_DIR);
                File to;
                if (backupDir.isDirectory() || backupDir.mkdir())
                    to = new File(backupDir, name);
                else
                    to = new File(context.getConfigDir(), name);
                boolean success = FileUtil.rename(pkf, to);
                if (success)
                    msgs.add("Private key file " + pkf.getAbsolutePath() +
                             " renamed to " + to.getAbsolutePath());
            }
        }
        return msgs;
    }

    private static List<String> doSave(Context ctx, TunnelControllerGroup tcg) { 
        List<String> rv = tcg.clearAllMessages();
        try {
            tcg.saveConfig();
            rv.add(0, ctx.getResources().getString(R.string.i2ptunnel_msg_config_saved));
        } catch (IOException ioe) {
            Util.e("Failed to save config file", ioe);
            rv.add(0, ctx.getResources().getString(R.string.i2ptunnel_msg_config_save_failed) + ": " + ioe.toString());
        }
        return rv;
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

    public static String getPrivateKeyFile(TunnelControllerGroup tcg, int tunnel) {
        TunnelController tun = getController(tcg, tunnel);
        if (tun != null && tun.getPrivKeyFile() != null)
            return tun.getPrivKeyFile();
        if (tunnel < 0)
            tunnel = tcg == null ? 999 : tcg.getControllers().size();
        return "i2ptunnel" + tunnel + "-privKeys.dat";
    }

    /** @since 0.9.9 */
    public boolean isSSLEnabled(TunnelControllerGroup tcg, int tunnel) {
        TunnelController tun = getController(tcg, tunnel);
        if (tun != null) {
            Properties opts = tun.getClientOptionProps();
            return Boolean.parseBoolean(opts.getProperty(I2PTunnelServer.PROP_USE_SSL));
        }
        return false;
    }

    /** @since 0.9.12 */
    public boolean isRejectInproxy(TunnelControllerGroup tcg, int tunnel) {
        TunnelController tun = getController(tcg, tunnel);
        if (tun != null) {
            Properties opts = tun.getClientOptionProps();
            return Boolean.parseBoolean(opts.getProperty(I2PTunnelHTTPServer.OPT_REJECT_INPROXY));
        }
        return false;
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
                cfg.setOutproxyAuth("false");
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
