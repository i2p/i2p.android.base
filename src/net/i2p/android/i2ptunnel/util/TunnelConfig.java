package net.i2p.android.i2ptunnel.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import net.i2p.I2PAppContext;
import net.i2p.android.router.R;
import net.i2p.android.wizard.model.Page;
import net.i2p.i2ptunnel.I2PTunnelConnectClient;
import net.i2p.i2ptunnel.I2PTunnelHTTPClient;
import net.i2p.i2ptunnel.I2PTunnelHTTPClientBase;
import net.i2p.i2ptunnel.I2PTunnelIRCClient;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.util.ConcurrentHashSet;
import net.i2p.util.PasswordManager;

public class TunnelConfig {
    protected final I2PAppContext _context;

    private String _type;
    private String _name;
    private String _description;
    private String _i2cpHost;
    private String _i2cpPort;
    private String _tunnelDepth;
    private String _tunnelQuantity;
    private String _tunnelVariance;
    private String _tunnelBackupQuantity;
    private boolean _connectDelay;
    private String _customOptions;
    private String _proxyList;
    private String _port;
    private String _reachableBy;
    private String _targetDestination;
    private String _targetHost;
    private String _targetPort;
    private String _spoofedHost;
    private String _privKeyFile;
    private String _profile;
    private boolean _startOnLoad;
    private boolean _sharedClient;
    private final Set<String> _booleanOptions;
    private final Map<String, String> _otherOptions;
    private String _newProxyUser;
    private String _newProxyPW;

    static final String CLIENT_NICKNAME = "shared clients";

    public static TunnelConfig createFromWizard(
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
        String type = TunnelUtil.getTypeFromName(typeName, ctx);
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

        String targetPort = null;
        pageData = data.getBundle(kTargetPort);
        if (pageData != null) targetPort = pageData.getString(Page.SIMPLE_DATA_KEY);
        cfg.setTargetPort(targetPort);

        String reachableOn = null;
        pageData = data.getBundle(kReachableOn);
        if (pageData != null) reachableOn = pageData.getString(Page.SIMPLE_DATA_KEY);
        cfg.setReachableBy(reachableOn);

        String bindingPort = null;
        pageData = data.getBundle(kBindingPort);
        if (pageData != null) bindingPort = pageData.getString(Page.SIMPLE_DATA_KEY);
        cfg.setPort(bindingPort);

        boolean autoStart = data.getBundle(kAutoStart).getBoolean(Page.SIMPLE_DATA_KEY);
        cfg.setStartOnLoad(autoStart);

        // Set sensible defaults for a new tunnel
        cfg.setTunnelDepth("3");
        cfg.setTunnelVariance("0");
        cfg.setTunnelQuantity("2");
        cfg.setTunnelBackupQuantity("0");
        cfg.setClientHost("internal");
        cfg.setClientport("internal");
        cfg.setCustomOptions("");
        if (!"streamrclient".equals(type)) {
            cfg.setProfile("bulk");
            cfg.setReduceCount("1");
            cfg.setReduceTime("20");
        }
        if (TunnelUtil.isClient(type)) { /* Client-only defaults */
            if (!"streamrclient".equals(type)) {
                cfg.setNewDest("0");
                cfg.setCloseTime("30");
            }
            if ("httpclient".equals(type) ||
                    "connectclient".equals(type) ||
                    "sockstunnel".equals(type) |
                    "socksirctunnel".equals(type)) {
                cfg.setProxyUsername("");
                cfg.setProxyPassword("");
                cfg.setOutproxyUsername("");
                cfg.setOutproxyPassword("");
            }
            if ("httpclient".equals(type))
                cfg.setJumpList("http://i2host.i2p/cgi-bin/i2hostjump?\nhttp://stats.i2p/cgi-bin/jump.cgi?a=");
        } else { /* Server-only defaults */
            cfg.setPrivKeyFile(TunnelUtil.getPrivateKeyFile(tcg, -1));
            cfg.setEncrypt("");
            cfg.setEncryptKey("");
            cfg.setAccessMode("0");
            cfg.setAccessList("");
            cfg.setLimitMinute("0");
            cfg.setLimitHour("0");
            cfg.setLimitDay("0");
            cfg.setTotalMinute("0");
            cfg.setTotalHour("0");
            cfg.setTotalDay("0");
            cfg.setMaxStreams("0");
        }

        return cfg;
    }

    public TunnelConfig() {
        _context = I2PAppContext.getGlobalContext();
        _booleanOptions = new ConcurrentHashSet<String>(4);
        _otherOptions = new ConcurrentHashMap<String,String>(4);
    }

    /**
     * What type of tunnel (httpclient, ircclient, client, or server).  This is 
     * required when adding a new tunnel.
     *
     */
    public void setType(String type) { 
        _type = (type != null ? type.trim() : null);   
    }
    String getType() { return _type; }

    /** Short name of the tunnel */
    public void setName(String name) { 
        _name = (name != null ? name.trim() : null);
    }
    /** one line description */
    public void setDescription(String description) { 
        _description = (description != null ? description.trim() : null);
    }
    /** I2CP host the router is on, ignored when in router context */
    public void setClientHost(String host) {
        _i2cpHost = (host != null ? host.trim() : null);
    }
    /** I2CP port the router is on, ignored when in router context */
    public void setClientport(String port) {
        _i2cpPort = (port != null ? port.trim() : null);
    }
    /** how many hops to use for inbound tunnels */
    public void setTunnelDepth(String tunnelDepth) { 
        _tunnelDepth = (tunnelDepth != null ? tunnelDepth.trim() : null);
    }
    /** how many parallel inbound tunnels to use */
    public void setTunnelQuantity(String tunnelQuantity) { 
        _tunnelQuantity = (tunnelQuantity != null ? tunnelQuantity.trim() : null);
    }
    /** how much randomisation to apply to the depth of tunnels */
    public void setTunnelVariance(String tunnelVariance) { 
        _tunnelVariance = (tunnelVariance != null ? tunnelVariance.trim() : null);
    }
    /** how many tunnels to hold in reserve to guard against failures */
    public void setTunnelBackupQuantity(String tunnelBackupQuantity) { 
        _tunnelBackupQuantity = (tunnelBackupQuantity != null ? tunnelBackupQuantity.trim() : null);
    }
    /** what I2P session overrides should be used */
    public void setCustomOptions(String customOptions) { 
        _customOptions = (customOptions != null ? customOptions.trim() : null);
    }
    /** what HTTP outproxies should be used (httpclient specific) */
    public void setProxyList(String proxyList) { 
        _proxyList = (proxyList != null ? proxyList.trim() : null);
    }
    /** what port should this client/httpclient/ircclient listen on */
    public void setPort(String port) { 
        _port = (port != null ? port.trim() : null);
    }
    /** 
     * what interface should this client/httpclient/ircclient listen on
     */
    public void setReachableBy(String reachableBy) { 
        _reachableBy = (reachableBy != null ? reachableBy.trim() : null);
    }
    /** What peer does this client tunnel point at */
    public void setTargetDestination(String dest) { 
        _targetDestination = (dest != null ? dest.trim() : null);
    }
    /** What host does this server tunnel point at */
    public void setTargetHost(String host) { 
        _targetHost = (host != null ? host.trim() : null);
    }
    /** What port does this server tunnel point at */
    public void setTargetPort(String port) { 
        _targetPort = (port != null ? port.trim() : null);
    }
    /** What host does this http server tunnel spoof */
    public void setSpoofedHost(String host) { 
        _spoofedHost = (host != null ? host.trim() : null);
    }
    /** What filename is this server tunnel's private keys stored in */
    public void setPrivKeyFile(String file) { 
        _privKeyFile = (file != null ? file.trim() : null);
    }
    /**
     * If called with true, we want this tunnel to start whenever it is
     * loaded (aka right now and whenever the router is started up)
     */
    public void setStartOnLoad(boolean val) {
        _startOnLoad = val;
    }
    public void setShared(boolean val) {
        _sharedClient=val;
    }
    public void setConnectDelay(String moo) {
        _connectDelay = true;
    }
    public void setProfile(String profile) { 
        _profile = profile; 
    }

    public void setReduce(String moo) {
        _booleanOptions.add("i2cp.reduceOnIdle");
    }
    public void setClose(String moo) {
        _booleanOptions.add("i2cp.closeOnIdle");
    }
    public void setEncrypt(String moo) {
        _booleanOptions.add("i2cp.encryptLeaseSet");
    }

    /** @since 0.8.9 */
    public void setDCC(String moo) {
        _booleanOptions.add(I2PTunnelIRCClient.PROP_DCC);
    }

    protected static final String PROP_ENABLE_ACCESS_LIST = "i2cp.enableAccessList";
    protected static final String PROP_ENABLE_BLACKLIST = "i2cp.enableBlackList";

    public void setAccessMode(String val) {
        if ("1".equals(val))
            _booleanOptions.add(PROP_ENABLE_ACCESS_LIST);
        else if ("2".equals(val))
            _booleanOptions.add(PROP_ENABLE_BLACKLIST);
    }

    public void setDelayOpen(String moo) {
        _booleanOptions.add("i2cp.delayOpen");
    }
    public void setNewDest(String val) {
        if ("1".equals(val))
            _booleanOptions.add("i2cp.newDestOnResume");
        else if ("2".equals(val))
            _booleanOptions.add("persistentClientKey");
    }

    public void setReduceTime(String val) {
        if (val != null) {
            try {
                _otherOptions.put("i2cp.reduceIdleTime", "" + (Integer.parseInt(val.trim()) * 60*1000));
            } catch (NumberFormatException nfe) {}
        }
    }
    public void setReduceCount(String val) {
        if (val != null)
            _otherOptions.put("i2cp.reduceQuantity", val.trim());
    }
    public void setEncryptKey(String val) {
        if (val != null)
            _otherOptions.put("i2cp.leaseSetKey", val.trim());
    }

    public void setAccessList(String val) {
        if (val != null)
            _otherOptions.put("i2cp.accessList", val.trim().replace("\r\n", ",").replace("\n", ",").replace(" ", ","));
    }

    public void setJumpList(String val) {
        if (val != null)
            _otherOptions.put(I2PTunnelHTTPClient.PROP_JUMP_SERVERS, val.trim().replace("\r\n", ",").replace("\n", ",").replace(" ", ","));
    }

    public void setCloseTime(String val) {
        if (val != null) {
            try {
                _otherOptions.put("i2cp.closeIdleTime", "" + (Integer.parseInt(val.trim()) * 60*1000));
            } catch (NumberFormatException nfe) {}
        }
    }

    /** all proxy auth @since 0.8.2 */
    public void setProxyAuth(String s) {
        if (s != null)
            _otherOptions.put(I2PTunnelHTTPClientBase.PROP_AUTH, I2PTunnelHTTPClientBase.DIGEST_AUTH);
    }

    public void setProxyUsername(String s) {
        if (s != null)
            _newProxyUser = s.trim();
    }

    public void setProxyPassword(String s) {
        if (s != null)
            _newProxyPW = s.trim();
    }

    public void setOutproxyAuth(String s) {
        _otherOptions.put(I2PTunnelHTTPClientBase.PROP_OUTPROXY_AUTH, I2PTunnelHTTPClientBase.DIGEST_AUTH);
    }

    public void setOutproxyUsername(String s) {
        if (s != null)
            _otherOptions.put(I2PTunnelHTTPClientBase.PROP_OUTPROXY_USER, s.trim());
    }

    public void setOutproxyPassword(String s) {
        if (s != null)
            _otherOptions.put(I2PTunnelHTTPClientBase.PROP_OUTPROXY_PW, s.trim());
    }

    /** all of these are @since 0.8.3 */
    protected static final String PROP_MAX_CONNS_MIN = "i2p.streaming.maxConnsPerMinute";
    protected static final String PROP_MAX_CONNS_HOUR = "i2p.streaming.maxConnsPerHour";
    protected static final String PROP_MAX_CONNS_DAY = "i2p.streaming.maxConnsPerDay";
    protected static final String PROP_MAX_TOTAL_CONNS_MIN = "i2p.streaming.maxTotalConnsPerMinute";
    protected static final String PROP_MAX_TOTAL_CONNS_HOUR = "i2p.streaming.maxTotalConnsPerHour";
    protected static final String PROP_MAX_TOTAL_CONNS_DAY = "i2p.streaming.maxTotalConnsPerDay";
    protected static final String PROP_MAX_STREAMS = "i2p.streaming.maxConcurrentStreams";

    public void setLimitMinute(String s) {
        if (s != null)
            _otherOptions.put(PROP_MAX_CONNS_MIN, s.trim());
    }

    public void setLimitHour(String s) {
        if (s != null)
            _otherOptions.put(PROP_MAX_CONNS_HOUR, s.trim());
    }

    public void setLimitDay(String s) {
        if (s != null)
            _otherOptions.put(PROP_MAX_CONNS_DAY, s.trim());
    }

    public void setTotalMinute(String s) {
        if (s != null)
            _otherOptions.put(PROP_MAX_TOTAL_CONNS_MIN, s.trim());
    }

    public void setTotalHour(String s) {
        if (s != null)
            _otherOptions.put(PROP_MAX_TOTAL_CONNS_HOUR, s.trim());
    }

    public void setTotalDay(String s) {
        if (s != null)
            _otherOptions.put(PROP_MAX_TOTAL_CONNS_DAY, s.trim());
    }

    public void setMaxStreams(String s) {
        if (s != null)
            _otherOptions.put(PROP_MAX_STREAMS, s.trim());
    }

    /**
     * Based on all provided data, create a set of configuration parameters 
     * suitable for use in a TunnelController.  This will replace (not add to)
     * any existing parameters, so this should return a comprehensive mapping.
     *
     */
    public Properties getConfig() {
        Properties config = new Properties();
        updateConfigGeneric(config);
        
        if ((TunnelUtil.isClient(_type) && !"streamrclient".equals(_type)) || "streamrserver".equals(_type)) {
            // streamrserver uses interface
            if (_reachableBy != null)
                config.setProperty("interface", _reachableBy);
            else
                config.setProperty("interface", "");
        } else {
            // streamrclient uses targetHost
            if (_targetHost != null)
                config.setProperty("targetHost", _targetHost);
        }

        if (TunnelUtil.isClient(_type)) {
            // generic client stuff
            if (_port != null)
                config.setProperty("listenPort", _port);
            config.setProperty("sharedClient", _sharedClient + "");
            for (String p : _booleanClientOpts)
                config.setProperty("option." + p, "" + _booleanOptions.contains(p));
            for (String p : _otherClientOpts)
                if (_otherOptions.containsKey(p))
                    config.setProperty("option." + p, _otherOptions.get(p));
        } else {
            // generic server stuff
            if (_targetPort != null)
                config.setProperty("targetPort", _targetPort);
            for (String p : _booleanServerOpts)
                config.setProperty("option." + p, "" + _booleanOptions.contains(p));
            for (String p : _otherServerOpts)
                if (_otherOptions.containsKey(p))
                    config.setProperty("option." + p, _otherOptions.get(p));
        }

        // generic proxy stuff
        if ("httpclient".equals(_type) || "connectclient".equals(_type) || 
            "sockstunnel".equals(_type) ||"socksirctunnel".equals(_type)) {
            for (String p : _booleanProxyOpts)
                config.setProperty("option." + p, "" + _booleanOptions.contains(p));
            if (_proxyList != null)
                config.setProperty("proxyList", _proxyList);
        }

        // Proxy auth including migration to MD5
        if ("httpclient".equals(_type) || "connectclient".equals(_type)) {
            // Migrate even if auth is disabled
            // go get the old from custom options that updateConfigGeneric() put in there
            String puser = "option." + I2PTunnelHTTPClientBase.PROP_USER;
            String user = config.getProperty(puser);
            String ppw = "option." + I2PTunnelHTTPClientBase.PROP_PW;
            String pw = config.getProperty(ppw);
            if (user != null && pw != null && user.length() > 0 && pw.length() > 0) {
                String pmd5 = "option." + I2PTunnelHTTPClientBase.PROP_PROXY_DIGEST_PREFIX +
                              user + I2PTunnelHTTPClientBase.PROP_PROXY_DIGEST_SUFFIX;
                if (config.getProperty(pmd5) == null) {
                    // not in there, migrate
                    String realm = _type.equals("httpclient") ? I2PTunnelHTTPClient.AUTH_REALM
                                                              : I2PTunnelConnectClient.AUTH_REALM;
                    String hex = PasswordManager.md5Hex(realm, user, pw);
                    if (hex != null) {
                        config.setProperty(pmd5, hex);
                        config.remove(puser);
                        config.remove(ppw);
                    }
                }
            }
            // New user/password
            String auth = _otherOptions.get(I2PTunnelHTTPClientBase.PROP_AUTH);
            if (auth != null && !auth.equals("false")) {
                if (_newProxyUser != null && _newProxyPW != null &&
                    _newProxyUser.length() > 0 && _newProxyPW.length() > 0) {
                    String pmd5 = "option." + I2PTunnelHTTPClientBase.PROP_PROXY_DIGEST_PREFIX +
                                  _newProxyUser + I2PTunnelHTTPClientBase.PROP_PROXY_DIGEST_SUFFIX;
                    String realm = _type.equals("httpclient") ? I2PTunnelHTTPClient.AUTH_REALM
                                                              : I2PTunnelConnectClient.AUTH_REALM;
                    String hex = PasswordManager.md5Hex(realm, _newProxyUser, _newProxyPW);
                    if (hex != null)
                        config.setProperty(pmd5, hex);
                }
            }
        }

        if ("ircclient".equals(_type) || "client".equals(_type) || "streamrclient".equals(_type)) {
            if (_targetDestination != null)
                config.setProperty("targetDestination", _targetDestination);
        } else if ("httpserver".equals(_type) || "httpbidirserver".equals(_type)) {
            if (_spoofedHost != null)
                config.setProperty("spoofedHost", _spoofedHost);
        }
        if ("httpbidirserver".equals(_type)) {
            if (_port != null)
                config.setProperty("listenPort", _port);
            if (_reachableBy != null)
                config.setProperty("interface", _reachableBy);
            else if (_targetHost != null)
                config.setProperty("interface", _targetHost);
            else
                config.setProperty("interface", "");
        }

        if ("ircclient".equals(_type)) {
            boolean dcc = _booleanOptions.contains(I2PTunnelIRCClient.PROP_DCC);
            config.setProperty("option." + I2PTunnelIRCClient.PROP_DCC,
                               "" + dcc);
            // add some sane server options since they aren't in the GUI (yet)
            if (dcc) {
                config.setProperty("option." + PROP_MAX_CONNS_MIN, "3");
                config.setProperty("option." + PROP_MAX_CONNS_HOUR, "10");
                config.setProperty("option." + PROP_MAX_TOTAL_CONNS_MIN, "5");
                config.setProperty("option." + PROP_MAX_TOTAL_CONNS_HOUR, "25");
            }
        }

        return config;
    }
    
    private static final String _noShowOpts[] = {
        "inbound.length", "outbound.length", "inbound.lengthVariance", "outbound.lengthVariance",
        "inbound.backupQuantity", "outbound.backupQuantity", "inbound.quantity", "outbound.quantity",
        "inbound.nickname", "outbound.nickname", "i2p.streaming.connectDelay", "i2p.streaming.maxWindowSize",
        I2PTunnelIRCClient.PROP_DCC
        };
    private static final String _booleanClientOpts[] = {
        "i2cp.reduceOnIdle", "i2cp.closeOnIdle", "i2cp.newDestOnResume", "persistentClientKey", "i2cp.delayOpen"
        };
    private static final String _booleanProxyOpts[] = {
        I2PTunnelHTTPClientBase.PROP_OUTPROXY_AUTH
        };
    private static final String _booleanServerOpts[] = {
        "i2cp.reduceOnIdle", "i2cp.encryptLeaseSet", PROP_ENABLE_ACCESS_LIST, PROP_ENABLE_BLACKLIST
        };
    private static final String _otherClientOpts[] = {
        "i2cp.reduceIdleTime", "i2cp.reduceQuantity", "i2cp.closeIdleTime",
        "outproxyUsername", "outproxyPassword",
        I2PTunnelHTTPClient.PROP_JUMP_SERVERS,
        I2PTunnelHTTPClientBase.PROP_AUTH
        };
    private static final String _otherServerOpts[] = {
        "i2cp.reduceIdleTime", "i2cp.reduceQuantity", "i2cp.leaseSetKey", "i2cp.accessList",
         PROP_MAX_CONNS_MIN, PROP_MAX_CONNS_HOUR, PROP_MAX_CONNS_DAY,
         PROP_MAX_TOTAL_CONNS_MIN, PROP_MAX_TOTAL_CONNS_HOUR, PROP_MAX_TOTAL_CONNS_DAY,
         PROP_MAX_STREAMS
        };

    /**
     *  do NOT add these to noShoOpts, we must leave them in for HTTPClient and ConnectCLient
     *  so they will get migrated to MD5
     *  TODO migrate socks to MD5
     */
    private static final String _otherProxyOpts[] = {
        "proxyUsername", "proxyPassword"
        };

    protected static final Set<String> _noShowSet = new HashSet<String>(64);
    protected static final Set<String> _nonProxyNoShowSet = new HashSet<String>(4);
    static {
        _noShowSet.addAll(Arrays.asList(_noShowOpts));
        _noShowSet.addAll(Arrays.asList(_booleanClientOpts));
        _noShowSet.addAll(Arrays.asList(_booleanProxyOpts));
        _noShowSet.addAll(Arrays.asList(_booleanServerOpts));
        _noShowSet.addAll(Arrays.asList(_otherClientOpts));
        _noShowSet.addAll(Arrays.asList(_otherServerOpts));
        _nonProxyNoShowSet.addAll(Arrays.asList(_otherProxyOpts));
    }

    private void updateConfigGeneric(Properties config) {
        config.setProperty("type", _type);
        if (_name != null)
            config.setProperty("name", _name);
        if (_description != null)
            config.setProperty("description", _description);
        if (!_context.isRouterContext()) {
            if (_i2cpHost != null)
                config.setProperty("i2cpHost", _i2cpHost);
            if ( (_i2cpPort != null) && (_i2cpPort.trim().length() > 0) ) {
                config.setProperty("i2cpPort", _i2cpPort);
            } else {
                config.setProperty("i2cpPort", "7654");
            }
        }
        if (_privKeyFile != null)
            config.setProperty("privKeyFile", _privKeyFile);
        
        if (_customOptions != null) {
            StringTokenizer tok = new StringTokenizer(_customOptions);
            while (tok.hasMoreTokens()) {
                String pair = tok.nextToken();
                int eq = pair.indexOf('=');
                if ( (eq <= 0) || (eq >= pair.length()) )
                    continue;
                String key = pair.substring(0, eq);
                if (_noShowSet.contains(key))
                    continue;
                // leave in for HTTP and Connect so it can get migrated to MD5
                // hide for SOCKS until migrated to MD5
                if ((!"httpclient".equals(_type)) &&
                    (! "connectclient".equals(_type)) &&
                    _nonProxyNoShowSet.contains(key))
                    continue;
                String val = pair.substring(eq+1);
                config.setProperty("option." + key, val);
            }
        }

        config.setProperty("startOnLoad", _startOnLoad + "");

        if (_tunnelQuantity != null) {
            config.setProperty("option.inbound.quantity", _tunnelQuantity);
            config.setProperty("option.outbound.quantity", _tunnelQuantity);
        }
        if (_tunnelDepth != null) {
            config.setProperty("option.inbound.length", _tunnelDepth);
            config.setProperty("option.outbound.length", _tunnelDepth);
        }
        if (_tunnelVariance != null) {
            config.setProperty("option.inbound.lengthVariance", _tunnelVariance);
            config.setProperty("option.outbound.lengthVariance", _tunnelVariance);
        }
        if (_tunnelBackupQuantity != null) {
            config.setProperty("option.inbound.backupQuantity", _tunnelBackupQuantity);
            config.setProperty("option.outbound.backupQuantity", _tunnelBackupQuantity);
        }
        if (_connectDelay)
            config.setProperty("option.i2p.streaming.connectDelay", "1000");
        else
            config.setProperty("option.i2p.streaming.connectDelay", "0");
        if (TunnelUtil.isClient(_type) && _sharedClient) {
            config.setProperty("option.inbound.nickname", CLIENT_NICKNAME);
            config.setProperty("option.outbound.nickname", CLIENT_NICKNAME);
        } else if (_name != null) {
            config.setProperty("option.inbound.nickname", _name);
            config.setProperty("option.outbound.nickname", _name);
        }
        if ("interactive".equals(_profile))
            // This was 1 which doesn't make much sense
            // The real way to make it interactive is to make the streaming lib
            // MessageInputStream flush faster but there's no option for that yet,
            // Setting it to 16 instead of the default but not sure what good that is either.
            config.setProperty("option.i2p.streaming.maxWindowSize", "16");
        else
            config.remove("option.i2p.streaming.maxWindowSize");
    }
}
