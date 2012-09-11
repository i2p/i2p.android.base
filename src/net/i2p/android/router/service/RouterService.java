package net.i2p.android.router.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import net.i2p.android.router.binder.RouterBinder;
import net.i2p.android.router.receiver.I2PReceiver;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataHelper;
import net.i2p.router.Job;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.RouterLaunch;
import net.i2p.util.OrderedProperties;

/**
 * Runs the router
 */
public class RouterService extends Service {

    public enum State {

        INIT, WAITING, STARTING, RUNNING,
        // unplanned (router stopped itself), next: killSelf()
        STOPPING, STOPPED,
        // button, don't kill service when stopped, stay in MANUAL_STOPPED
        MANUAL_STOPPING, MANUAL_STOPPED,
        // button, DO kill service when stopped, next: killSelf()
        MANUAL_QUITTING, MANUAL_QUITTED,
        // Stopped by listener (no network), next: WAITING (spin waiting for network)
        NETWORK_STOPPING, NETWORK_STOPPED
    }
    private RouterContext _context;
    private String _myDir;
    //private String _apkPath;
    private State _state = State.INIT;
    private Thread _starterThread;
    private StatusBar _statusBar;
    private I2PReceiver _receiver;
    private IBinder _binder;
    private final Object _stateLock = new Object();
    private Handler _handler;
    private Runnable _updater;
    private static final String SHARED_PREFS = "net.i2p.android.router";
    private static final String LAST_STATE = "service.lastState";
    private static final String EXTRA_RESTART = "restart";
    private static final String MARKER = "**************************************  ";

    @Override
    public void onCreate() {
        State lastState = getSavedState();
        setState(State.INIT);
        Util.i(this + " onCreate called"
                + " Saved state is: " + lastState
                + " Current state is: " + _state);

        //(new File(getFilesDir(), "wrapper.log")).delete();
        _myDir = getFilesDir().getAbsolutePath();
        // init other stuff here, delete log, etc.
        Init init = new Init(this);
        init.initialize();
        //_apkPath = init.getAPKPath();
        _statusBar = new StatusBar(this);
        // Remove stale notification icon.
        _statusBar.remove();
        _binder = new RouterBinder(this);
        _handler = new Handler();
        _updater = new Updater();
        if(lastState == State.RUNNING) {
            Intent intent = new Intent(this, RouterService.class);
            intent.putExtra(EXTRA_RESTART, true);
            onStartCommand(intent, 12345, 67890);
        } else if(lastState == State.MANUAL_QUITTING || lastState == State.MANUAL_QUITTED) {
            stopSelf(); // Die.
        }
    }

    /**
     * NOT called by system if it restarts us after a crash
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Util.i(this + " onStart called"
                + " Intent is: " + intent
                + " Flags is: " + flags
                + " ID is: " + startId
                + " Current state is: " + _state);
        boolean restart = intent != null && intent.getBooleanExtra(EXTRA_RESTART, false);
        if(restart) {
            Util.i(this + " RESTARTING");
        }
        synchronized(_stateLock) {
            if(_state != State.INIT) //return START_STICKY;
            {
                return START_NOT_STICKY;
            }
            _receiver = new I2PReceiver(this);
            if(Util.isConnected(this)) {
                if(restart) {
                    _statusBar.replace(StatusBar.ICON1, "I2P is restarting");
                } else {
                    _statusBar.replace(StatusBar.ICON1, "I2P is starting up");
                }
                setState(State.STARTING);
                _starterThread = new Thread(new Starter());
                _starterThread.start();
            } else {
                _statusBar.replace(StatusBar.ICON6, "I2P is waiting for a network connection");
                setState(State.WAITING);
                _handler.postDelayed(new Waiter(), 10 * 1000);
            }
        }
        _handler.removeCallbacks(_updater);
        _handler.postDelayed(_updater, 50);
        if(!restart) {
            startForeground(1337, _statusBar.getNote());
        }

        //return START_STICKY;
        return START_NOT_STICKY;
    }

    /**
     * maybe this goes away when the receiver can bind to us
     */
    private class Waiter implements Runnable {

        public void run() {
            Util.i(MARKER + this + " waiter handler"
                    + " Current state is: " + _state);
            if(_state == State.WAITING) {
                if(Util.isConnected(RouterService.this)) {
                    synchronized(_stateLock) {
                        if(_state != State.WAITING) {
                            return;
                        }
                        _statusBar.replace(StatusBar.ICON1, "Network connected, I2P is starting up");
                        setState(State.STARTING);
                        _starterThread = new Thread(new Starter());
                        _starterThread.start();
                    }
                    return;
                }
                _handler.postDelayed(this, 15 * 1000);
            }
        }
    }

    private class Starter implements Runnable {

        public void run() {
            Util.i(MARKER + this + " starter thread"
                    + " Current state is: " + _state);
            //Util.i(MARKER + this + " JBigI speed test started");
            //NativeBigInteger.main(null);
            //Util.i(MARKER + this + " JBigI speed test finished, launching router");


            // Before we launch, fix up any settings that need to be fixed here.
            // This should be done in the core, but as of this writing it isn't!

            // Step one. Load the propertites.
            Properties props = new OrderedProperties();
            Properties oldprops = new OrderedProperties();
            String wrapName = _myDir + "/router.config";
            try {
                InputStream fin = new FileInputStream(new File(wrapName));
                DataHelper.loadProps(props, fin);
            } catch(IOException ioe) {
                // shouldn't happen...
            }
            oldprops.putAll(props);
            // Step two, check for any port settings, and copy for those that are missing.
            int UDPinbound;
            int UDPinlocal;
            int TCPinbound;
            int TCPinlocal;
            UDPinbound = Integer.parseInt(props.getProperty("i2np.udp.port", "-1"));
            UDPinlocal = Integer.parseInt(props.getProperty("i2np.udp.internalPort", "-1"));
            TCPinbound = Integer.parseInt(props.getProperty("i2np.ntcp.port", "-1"));
            TCPinlocal = Integer.parseInt(props.getProperty("i2np.ntcp.internalPort", "-1"));
            boolean hasUDPinbound = UDPinbound != -1;
            boolean hasUDPinlocal = UDPinlocal != -1;
            boolean hasTCPinbound = TCPinbound != -1;
            boolean hasTCPinlocal = TCPinlocal != -1;

            // check and clear values based on these:
            boolean udp = Boolean.parseBoolean(props.getProperty("i2np.udp.enable", "false"));
            boolean tcp = Boolean.parseBoolean(props.getProperty("i2np.ntcp.enable", "false"));

            // Fix if both are false.
            if(!(udp || tcp)) {
                // If both are not on, turn them both on.
                props.setProperty("i2np.udp.enable", "true");
                props.setProperty("i2np.ntcp.enable", "true");
            }

            // Fix if we have local but no inbound
            if(!hasUDPinbound && hasUDPinlocal) {
                // if we got a local port and no external port, set it
                hasUDPinbound = true;
                UDPinbound = UDPinlocal;
            }
            if(!hasTCPinbound && hasTCPinlocal) {
                // if we got a local port and no external port, set it
                hasTCPinbound = true;
                TCPinbound = TCPinlocal;
            }

            boolean anyUDP = hasUDPinbound || hasUDPinlocal;
            boolean anyTCP = hasTCPinbound || hasTCPinlocal;
            boolean anyport = anyUDP || anyTCP;

            if(!anyport) {
                // generate one for UDPinbound, and fall thru.
                // FIX ME: Possibly not the best but should be OK.
                Random generator = new Random(System.currentTimeMillis());
                UDPinbound = generator.nextInt(55500) + 10000;
                anyUDP = true;
            }

            // Copy missing port numbers
            if(anyUDP && !anyTCP) {
                TCPinbound = UDPinbound;
                TCPinlocal = UDPinlocal;
            }
            if(anyTCP && !anyUDP) {
                UDPinbound = TCPinbound;
                UDPinlocal = TCPinlocal;
            }
            // reset for a retest.
            hasUDPinbound = UDPinbound != -1;
            hasUDPinlocal = UDPinlocal != -1;
            hasTCPinbound = TCPinbound != -1;
            hasTCPinlocal = TCPinlocal != -1;
            anyUDP = hasUDPinbound || hasUDPinlocal;
            anyTCP = hasTCPinbound || hasTCPinlocal;
            boolean checkAnyUDP = anyUDP && udp;
            boolean checkAnyTCP = anyTCP && tcp;

            // Enable things that need to be enabled.
            // Disable anything that needs to be disabled.
            if(!checkAnyUDP && !checkAnyTCP) {
                // enable the one(s) with values.
                if(anyUDP) {
                    udp = true;
                }
                if(anyTCP) {
                    tcp = true;
                }
            }

            if(!udp) {
                props.setProperty("i2np.udp.enable", "false");
                props.remove("i2np.udp.port");
                props.remove("i2np.udp.internalPort");
            } else {
                props.setProperty("i2np.udp.enable", "true");
                if(hasUDPinbound) {
                    props.setProperty("i2np.udp.port", Integer.toString(UDPinbound));
                } else {
                    props.remove("i2np.udp.port");
                }
                if(hasUDPinlocal) {
                    props.setProperty("i2np.udp.internalPort", Integer.toString(UDPinlocal));
                } else {
                    props.remove("i2np.udp.internalPort");
                }
            }

            if(!tcp) {
                props.setProperty("i2np.ntcp.enable", "false");
                props.remove("i2np.ntcp.port");
                props.remove("i2np.ntcp.internalPort");
            } else {
                props.setProperty("i2np.ntcp.enable", "true");
                if(hasTCPinbound) {
                    props.setProperty("i2np.ntcp.port", Integer.toString(TCPinbound));
                } else {
                    props.remove("i2np.ntcp.port");
                }
                if(hasTCPinlocal) {
                    props.setProperty("i2np.ntcp.internalPort", Integer.toString(TCPinlocal));
                } else {
                    props.remove("i2np.ntcp.internalPort");
                }
            }
            // WHEW! Now test for any changes.
            if(!props.equals(oldprops)) {
                // save fixed properties.
                try {
                    DataHelper.storeProps(props, new File(wrapName));
                } catch(IOException ioe) {
                    // shouldn't happen...
                }
            }

            // _NOW_ launch the router!
            RouterLaunch.main(null);
            synchronized(_stateLock) {
                if(_state != State.STARTING) {
                    return;
                }
                setState(State.RUNNING);
                List<?> contexts = RouterContext.listContexts();
                if((contexts == null) || (contexts.isEmpty())) {
                    throw new IllegalStateException("No contexts. This is usually because the router is either starting up or shutting down.");
                }
                _statusBar.replace(StatusBar.ICON2, "I2P is running");
                _context = (RouterContext) contexts.get(0);
                _context.router().setKillVMOnEnd(false);
                Job loadJob = new LoadClientsJob(_context);
                _context.jobQueue().addJob(loadJob);
                _context.addShutdownTask(new ShutdownHook());
                _context.addFinalShutdownTask(new FinalShutdownHook());
                _starterThread = null;
            }
            Util.i("Router.main finished");
        }
    }

    private class Updater implements Runnable {

        public void run() {
            RouterContext ctx = _context;
            if(ctx != null && _state == State.RUNNING) {
                Router router = ctx.router();
                if(router.isAlive()) {
                    updateStatus(ctx);
                }
            }
            _handler.postDelayed(this, 15 * 1000);
        }
    }
    private boolean _hadTunnels;

    private void updateStatus(RouterContext ctx) {
        int active = ctx.commSystem().countActivePeers();
        int known = Math.max(ctx.netDb().getKnownRouters() - 1, 0);
        int inEx = ctx.tunnelManager().getFreeTunnelCount();
        int outEx = ctx.tunnelManager().getOutboundTunnelCount();
        int inCl = ctx.tunnelManager().getInboundClientTunnelCount();
        int outCl = ctx.tunnelManager().getOutboundClientTunnelCount();
        String uptime = DataHelper.formatDuration(ctx.router().getUptime());
        double inBW = ctx.bandwidthLimiter().getReceiveBps() / 1024;
        double outBW = ctx.bandwidthLimiter().getSendBps() / 1024;
        // control total width
        DecimalFormat fmt;
        if(inBW >= 1000 || outBW >= 1000) {
            fmt = new DecimalFormat("#0");
        } else if(inBW >= 100 || outBW >= 100) {
            fmt = new DecimalFormat("#0.0");
        } else {
            fmt = new DecimalFormat("#0.00");
        }

        String status =
                "I2P "
                + active + '/' + known + " peers connected";

        String details =
                fmt.format(inBW) + '/' + fmt.format(outBW) + " KBps"
                + "; Expl " + inEx + '/' + outEx
                + "; Client " + inCl + '/' + outCl;

        boolean haveTunnels = inCl > 0 && outCl > 0;
        if(haveTunnels != _hadTunnels) {
            if(haveTunnels) {
                _statusBar.replace(StatusBar.ICON3, "Client tunnels are ready");
            } else {
                _statusBar.replace(StatusBar.ICON2, "Client tunnels are down");
            }
            _hadTunnels = haveTunnels;
        }
        _statusBar.update(status, details);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Util.i(this + "onBind called"
                + " Current state is: " + _state);
        return _binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    // ******** following methods may be accessed from Activities and Receivers ************
    /**
     * @returns null if router is not running
     */
    public RouterContext getRouterContext() {
        RouterContext rv = _context;
        if(rv == null) {
            return null;
        }
        if(!rv.router().isAlive()) {
            return null;
        }
        if(_state != State.RUNNING
                && _state != State.STOPPING
                && _state != State.MANUAL_STOPPING
                && _state != State.MANUAL_QUITTING
                && _state != State.NETWORK_STOPPING) {
            return null;
        }
        return rv;
    }

    /**
     * debug
     */
    public String getState() {
        return _state.toString();
    }

    public boolean canManualStop() {
        return _state == State.WAITING || _state == State.STARTING || _state == State.RUNNING;
    }

    /**
     * Stop and don't restart the router, but keep the service
     */
    public void manualStop() {
        Util.i("manualStop called"
                + " Current state is: " + _state);
        synchronized(_stateLock) {
            if(!canManualStop()) {
                return;
            }
            if(_state == State.STARTING) {
                _starterThread.interrupt();
            }
            if(_state == State.STARTING || _state == State.RUNNING) {
                _statusBar.replace(StatusBar.ICON4, "Stopping I2P");
                Thread stopperThread = new Thread(new Stopper(State.MANUAL_STOPPING, State.MANUAL_STOPPED));
                stopperThread.start();
            }
        }
    }

    /**
     * Stop the router and kill the service
     */
    public void manualQuit() {
        Util.i("manualQuit called"
                + " Current state is: " + _state);
        synchronized(_stateLock) {
            if(!canManualStop()) {
                return;
            }
            if(_state == State.STARTING) {
                _starterThread.interrupt();
            }
            if(_state == State.STARTING || _state == State.RUNNING) {
                _statusBar.replace(StatusBar.ICON4, "Stopping I2P");
                Thread stopperThread = new Thread(new Stopper(State.MANUAL_QUITTING, State.MANUAL_QUITTED));
                stopperThread.start();
            } else if(_state == State.WAITING) {
                setState(State.MANUAL_QUITTING);
                (new FinalShutdownHook()).run();
            }
        }
    }

    /**
     * Stop and then spin waiting for a network connection, then restart
     */
    public void networkStop() {
        Util.i("networkStop called"
                + " Current state is: " + _state);
        synchronized(_stateLock) {
            if(_state == State.STARTING) {
                _starterThread.interrupt();
            }
            if(_state == State.STARTING || _state == State.RUNNING) {
                _statusBar.replace(StatusBar.ICON4, "Network disconnected, stopping I2P");
                // don't change state, let the shutdown hook do it
                Thread stopperThread = new Thread(new Stopper(State.NETWORK_STOPPING, State.NETWORK_STOPPING));
                stopperThread.start();
            }
        }
    }

    public boolean canManualStart() {
        // We can be in INIT if we restarted after crash but previous state was not RUNNING.
        return _state == State.INIT || _state == State.MANUAL_STOPPED || _state == State.STOPPED;
    }

    public void manualStart() {
        Util.i("restart called"
                + " Current state is: " + _state);
        synchronized(_stateLock) {
            if(!canManualStart()) {
                return;
            }
            _statusBar.replace(StatusBar.ICON1, "I2P is starting up");
            setState(State.STARTING);
            _starterThread = new Thread(new Starter());
            _starterThread.start();
        }
    }

    // ******** end methods accessed from Activities and Receivers ************
    /**
     * Turn off the status bar. Unregister the receiver. If we were running,
     * fire up the Stopper thread.
     */
    @Override
    public void onDestroy() {
        Util.i("onDestroy called"
                + " Current state is: " + _state);

        _handler.removeCallbacks(_updater);
        _statusBar.remove();

        I2PReceiver rcvr = _receiver;
        if(rcvr != null) {
            synchronized(rcvr) {
                try {
                    // throws if not registered
                    unregisterReceiver(rcvr);
                } catch(IllegalArgumentException iae) {
                }
                //rcvr.unbindRouter();
                //_receiver = null;
            }
        }
        synchronized(_stateLock) {
            if(_state == State.STARTING) {
                _starterThread.interrupt();
            }
            if(_state == State.STARTING || _state == State.RUNNING) {
                // should this be in a thread?
                _statusBar.replace(StatusBar.ICON5, "I2P is shutting down");
                Thread stopperThread = new Thread(new Stopper(State.STOPPING, State.STOPPED));
                stopperThread.start();
            }
        }
    }

    /**
     * Transition to the next state. If we still have a context, shut down the
     * router. Turn off the status bar. Then transition to the stop state.
     */
    private class Stopper implements Runnable {

        private final State nextState;
        private final State stopState;

        /**
         * call holding statelock
         */
        public Stopper(State next, State stop) {
            nextState = next;
            stopState = stop;
            setState(next);
        }

        public void run() {
            try {
                Util.i(MARKER + this + " stopper thread"
                        + " Current state is: " + _state);
                RouterContext ctx = _context;
                if(ctx != null) {
                    ctx.router().shutdown(Router.EXIT_HARD);
                }
                _statusBar.remove();
                Util.i("********** Router shutdown complete");
                synchronized(_stateLock) {
                    if(_state == nextState) {
                        setState(stopState);
                    }
                }
            } finally {
                stopForeground(true);
                _statusBar.remove();
            }
        }
    }

    /**
     * First (early) hook. Update the status bar. Unregister the receiver.
     */
    private class ShutdownHook implements Runnable {

        public void run() {
            Util.i(this + " shutdown hook"
                    + " Current state is: " + _state);
            _statusBar.replace(StatusBar.ICON5, "I2P is shutting down");
            I2PReceiver rcvr = _receiver;
            if(rcvr != null) {
                synchronized(rcvr) {
                    try {
                        // throws if not registered
                        unregisterReceiver(rcvr);
                    } catch(IllegalArgumentException iae) {
                    }
                    //rcvr.unbindRouter();
                    //_receiver = null;
                }
            }
            synchronized(_stateLock) {
                // null out to release the memory
                _context = null;
                if(_state == State.STARTING) {
                    _starterThread.interrupt();
                }
                if(_state == State.WAITING || _state == State.STARTING
                        || _state == State.RUNNING) {
                    setState(State.STOPPING);
                }
            }
        }
    }

    /**
     * Second (late) hook. Turn off the status bar. Null out the context. If we
     * were stopped manually, do nothing. If we were stopped because of no
     * network, start the waiter thread. If it stopped of unknown causes or from
     * manualQuit(), kill the Service.
     */
    private class FinalShutdownHook implements Runnable {

        public void run() {
            try {
                Util.i(this + " final shutdown hook"
                        + " Current state is: " + _state);
                //I2PReceiver rcvr = _receiver;

                synchronized(_stateLock) {
                    // null out to release the memory
                    _context = null;
                    Runtime.getRuntime().gc();
                    if(_state == State.STARTING) {
                        _starterThread.interrupt();
                    }
                    if(_state == State.MANUAL_STOPPING) {
                        setState(State.MANUAL_STOPPED);
                    } else if(_state == State.NETWORK_STOPPING) {
                        // start waiter handler
                        setState(State.WAITING);
                        _handler.postDelayed(new Waiter(), 10 * 1000);
                    } else if(_state == State.STARTING || _state == State.RUNNING
                            || _state == State.STOPPING) {
                        Util.i(this + " died of unknown causes");
                        setState(State.STOPPED);
                        stopSelf();
                    } else if(_state == State.MANUAL_QUITTING) {
                        setState(State.MANUAL_QUITTED);
                        stopSelf();
                    }
                }
            } finally {
                stopForeground(true);
                _statusBar.remove();
            }
        }
    }

    private State getSavedState() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, 0);
        String stateString = prefs.getString(LAST_STATE, State.INIT.toString());
        for(State s : State.values()) {
            if(s.toString().equals(stateString)) {
                return s;
            }
        }
        return State.INIT;
    }

    private void setState(State s) {
        _state = s;
        saveState();
    }

    /**
     * @return success
     */
    private boolean saveState() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(LAST_STATE, _state.toString());
        return edit.commit();
    }
}
