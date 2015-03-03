package net.i2p.android.i2ptunnel.util;

import android.content.Context;
import android.content.res.Resources;

import net.i2p.I2PAppContext;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import net.i2p.i2ptunnel.TunnelController;
import net.i2p.i2ptunnel.TunnelControllerGroup;
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
                    cOpt.setProperty("option.inbound.nickname", TunnelConfig.CLIENT_NICKNAME);
                    cOpt.setProperty("option.outbound.nickname", TunnelConfig.CLIENT_NICKNAME);
                    
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
        return ( ("client".equals(type)) ||
                 ("httpclient".equals(type)) ||
                 ("sockstunnel".equals(type)) ||
                 ("socksirctunnel".equals(type)) ||
                 ("connectclient".equals(type)) ||
                 ("streamrclient".equals(type)) ||
                 ("ircclient".equals(type)));
    }

    public static String getPrivateKeyFile(TunnelControllerGroup tcg, int tunnel) {
        TunnelController tun = getController(tcg, tunnel);
        if (tun != null && tun.getPrivKeyFile() != null)
            return tun.getPrivKeyFile();
        if (tunnel < 0)
            tunnel = tcg == null ? 999 : tcg.getControllers().size();
        return "i2ptunnel" + tunnel + "-privKeys.dat";
    }
}
