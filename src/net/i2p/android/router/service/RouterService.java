package net.i2p.android.router.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

import net.i2p.android.router.R;
import net.i2p.android.router.binder.RouterBinder;
import net.i2p.android.router.receiver.I2PReceiver;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataHelper;
import net.i2p.router.Job;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.RouterLaunch;
import net.i2p.util.NativeBigInteger;

/**
 *  Runs the router
 */
public class RouterService extends Service {
    public enum State {INIT, WAITING, STARTING, RUNNING,
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
        System.err.println(this + " onCreate called" +
                           " Saved state is: " + lastState +
                           " Current state is: " + _state);

        //(new File(getFilesDir(), "wrapper.log")).delete();
        _myDir = getFilesDir().getAbsolutePath();
        Init init = new Init(this);
        init.debugStuff();
        init.initialize();
        //_apkPath = init.getAPKPath();
        _statusBar = new StatusBar(this);
        // kill any old one... will this work?
        _statusBar.off();
        _binder = new RouterBinder(this);
        _handler = new Handler();
        _updater = new Updater();
        if (lastState == State.RUNNING) {
            Intent intent = new Intent(this, RouterService.class);
            intent.putExtra(EXTRA_RESTART, true);
            onStartCommand(intent, 12345, 67890);
        }
    }

    /** NOT called by system if it restarts us after a crash */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.err.println(this + " onStart called" +
                           " Intent is: " + intent +
                           " Flags is: " + flags +
                           " ID is: " + startId +
                           " Current state is: " + _state);
        boolean restart = intent != null && intent.getBooleanExtra(EXTRA_RESTART, false);
        if (restart)
            System.err.println(this + " RESTARTING");
        synchronized (_stateLock) {
            if (_state != State.INIT)
                //return START_STICKY;
                return START_NOT_STICKY;
            _receiver = new I2PReceiver(this);
            if (Util.isConnected(this)) {
                if (restart)
                    _statusBar.replace("I2P is restarting");
                else
                    _statusBar.replace("I2P is starting up");
                setState(State.STARTING);
                _starterThread = new Thread(new Starter());
                _starterThread.start();
            } else {
                _statusBar.replace("I2P is waiting for a network connection");
                setState(State.WAITING);
                _handler.postDelayed(new Waiter(), 10*1000);
            }
        }
        _handler.removeCallbacks(_updater);
        _handler.postDelayed(_updater, 50);

        //return START_STICKY;
        return START_NOT_STICKY;
    }

    /** maybe this goes away when the receiver can bind to us */
    private class Waiter implements Runnable {
        public void run() {
            System.err.println(MARKER + this + " waiter handler" +
                           " Current state is: " + _state);
            if (_state == State.WAITING) {
                if (Util.isConnected(RouterService.this)) {
                    synchronized (_stateLock) {
                        if (_state != State.WAITING)
                            return;
                        _statusBar.replace("Network connected, I2P is starting up");
                        setState(State.STARTING);
                        _starterThread = new Thread(new Starter());
                        _starterThread.start();
                    }
                    return;
                }
                _handler.postDelayed(this, 15*1000);
            }
        }
    }

    private class Starter implements Runnable {
        public void run() {
            System.err.println(MARKER + this + " starter thread" +
                           " Current state is: " + _state);
            //System.err.println(MARKER + this + " JBigI speed test started");
            //NativeBigInteger.main(null);
            //System.err.println(MARKER + this + " JBigI speed test finished, launching router");
            RouterLaunch.main(null);
            synchronized (_stateLock) {
                if (_state != State.STARTING)
                    return;
                setState(State.RUNNING);
                List contexts = RouterContext.listContexts();
                if ( (contexts == null) || (contexts.isEmpty()) ) 
                      throw new IllegalStateException("No contexts. This is usually because the router is either starting up or shutting down.");
                _statusBar.update("I2P is running");
                _context = (RouterContext)contexts.get(0);
                _context.router().setKillVMOnEnd(false);
                Job loadJob = new LoadClientsJob(_context);
                _context.jobQueue().addJob(loadJob);
                _context.addShutdownTask(new ShutdownHook());
                _context.addFinalShutdownTask(new FinalShutdownHook());
                _starterThread = null;
            }
            System.err.println("Router.main finished");
        }
    }

    private class Updater implements Runnable {
        public void run() {
            RouterContext ctx = _context;
            if (ctx != null && _state == State.RUNNING) {
                Router router = ctx.router();
                if (router.isAlive())
                    updateStatus(ctx);
            }
            _handler.postDelayed(this, 15*1000);
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
        if (inBW >= 1000 || outBW >= 1000)
            fmt = new DecimalFormat("#0");
        else if (inBW >= 100 || outBW >= 100)
            fmt = new DecimalFormat("#0.0");
        else
            fmt = new DecimalFormat("#0.00");

        String status =
               "I2P " +
               active + '/' + known + " peers connected";

        String details =
               fmt.format(inBW) + '/' + fmt.format(outBW) + " KBps" +
               "; Expl " + inEx + '/' + outEx +
               "; Client " + inCl + '/' + outCl;

        boolean haveTunnels = inCl > 0 && outCl > 0;
        if (haveTunnels != _hadTunnels) {
            if (haveTunnels)
                _statusBar.replace("Client tunnels are ready");
            else
                _statusBar.replace("Client tunnels are down");
            _hadTunnels = haveTunnels;
        }
        _statusBar.update(status, details);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        System.err.println(this + "onBind called" +
                           " Current state is: " + _state);
        return _binder;
    }

    // ******** following methods may be accessed from Activities and Receivers ************

    /**
     *  @returns null if router is not running
     */
    public RouterContext getRouterContext() {
        RouterContext rv = _context;
        if (rv == null)
            return null;
        if (!rv.router().isAlive())
            return null;
        if (_state != State.RUNNING &&
            _state != State.STOPPING &&
            _state != State.MANUAL_STOPPING &&
            _state != State.MANUAL_QUITTING &&
            _state != State.NETWORK_STOPPING)
            return null;
        return rv;
    }

    /** debug */
    public String getState() {
        return _state.toString();
    }

    public boolean canManualStop() {
        return _state == State.WAITING || _state == State.STARTING || _state == State.RUNNING;
    }

    /**
     *  Stop and don't restart the router, but keep the service
     */
    public void manualStop() {
        System.err.println("manualStop called" +
                           " Current state is: " + _state);
        synchronized (_stateLock) {
            if (!canManualStop())
                return;
            if (_state == State.STARTING)
                _starterThread.interrupt();
            if (_state == State.STARTING || _state == State.RUNNING) {
                _statusBar.replace("Stopping I2P");
                Thread stopperThread = new Thread(new Stopper(State.MANUAL_STOPPING, State.MANUAL_STOPPED));
                stopperThread.start();
            }
        }
    }

    /**
     *  Stop the router and kill the service
     */
    public void manualQuit() {
        System.err.println("manualQuit called" +
                           " Current state is: " + _state);
        synchronized (_stateLock) {
            if (!canManualStop())
                return;
            if (_state == State.STARTING)
                _starterThread.interrupt();
            if (_state == State.STARTING || _state == State.RUNNING) {
                _statusBar.replace("Stopping I2P");
                Thread stopperThread = new Thread(new Stopper(State.MANUAL_QUITTING, State.MANUAL_QUITTED));
                stopperThread.start();
            } else if (_state == State.WAITING) {
                setState(State.MANUAL_QUITTING);
                (new FinalShutdownHook()).run();
            }
        }
    }

    /**
     *  Stop and then spin waiting for a network connection, then restart
     */
    public void networkStop() {
        System.err.println("networkStop called" +
                           " Current state is: " + _state);
        synchronized (_stateLock) {
            if (_state == State.STARTING)
                _starterThread.interrupt();
            if (_state == State.STARTING || _state == State.RUNNING) {
                _statusBar.replace("Network disconnected, stopping I2P");
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
        System.err.println("restart called" +
                           " Current state is: " + _state);
        synchronized (_stateLock) {
            if (!canManualStart())
                return;
            _statusBar.replace("I2P is starting up");
            setState(State.STARTING);
            _starterThread = new Thread(new Starter());
            _starterThread.start();
        }
    }

    // ******** end methods accessed from Activities and Receivers ************

    /**
     *  Turn off the status bar.
     *  Unregister the receiver.
     *  If we were running, fire up the Stopper thread.
     */
    @Override
    public void onDestroy() {
        System.err.println("onDestroy called" +
                           " Current state is: " + _state);

        _handler.removeCallbacks(_updater);
        _statusBar.off();

        I2PReceiver rcvr = _receiver;
        if (rcvr != null) {
            synchronized(rcvr) {
                try {
                    // throws if not registered
                    unregisterReceiver(rcvr);
                } catch (IllegalArgumentException iae) {}
                //rcvr.unbindRouter();
                //_receiver = null;
            }
        }
        synchronized (_stateLock) {
            if (_state == State.STARTING)
                _starterThread.interrupt();
            if (_state == State.STARTING || _state == State.RUNNING) {
              // should this be in a thread?
                _statusBar.replace("I2P is shutting down");
                Thread stopperThread = new Thread(new Stopper(State.STOPPING, State.STOPPED));
                stopperThread.start();
            }
        }
    }

    /**
     *  Transition to the next state.
     *  If we still have a context, shut down the router.
     *  Turn off the status bar.
     *  Then transition to the stop state.
     */
    private class Stopper implements Runnable {
        private final State nextState;
        private final State stopState;

        /** call holding statelock */
        public Stopper(State next, State stop) {
            nextState = next;
            stopState = stop;
            setState(next);
        }

        public void run() {
            System.err.println(MARKER + this + " stopper thread" +
                               " Current state is: " + _state);
            RouterContext ctx = _context;
            if (ctx != null)
                ctx.router().shutdown(Router.EXIT_HARD);
            _statusBar.off();
            System.err.println("********** Router shutdown complete");
            synchronized (_stateLock) {
                if (_state == nextState)
                    setState(stopState);
            }
        }
    }

    /**
     *  First (early) hook.
     *  Update the status bar.
     *  Unregister the receiver.
     */
    private class ShutdownHook implements Runnable {
        public void run() {
            System.err.println(this + " shutdown hook" +
                               " Current state is: " + _state);
            _statusBar.replace("I2P is shutting down");
            I2PReceiver rcvr = _receiver;
            if (rcvr != null) {
                synchronized(rcvr) {
                    try {
                        // throws if not registered
                        unregisterReceiver(rcvr);
                    } catch (IllegalArgumentException iae) {}
                    //rcvr.unbindRouter();
                    //_receiver = null;
                }
            }
            synchronized (_stateLock) {
                // null out to release the memory
                _context = null;
                if (_state == State.STARTING)
                    _starterThread.interrupt();
                if (_state == State.WAITING || _state == State.STARTING ||
                    _state == State.RUNNING)
                    setState(State.STOPPING);
            }
        }
    }

    /**
     *  Second (late) hook.
     *  Turn off the status bar.
     *  Null out the context.
     *  If we were stopped manually, do nothing.
     *  If we were stopped because of no network, start the waiter thread.
     *  If it stopped of unknown causes or from manualQuit(), kill the Service.
     */
    private class FinalShutdownHook implements Runnable {
        public void run() {
            System.err.println(this + " final shutdown hook" +
                               " Current state is: " + _state);
            _statusBar.off();
            //I2PReceiver rcvr = _receiver;

            synchronized (_stateLock) {
                // null out to release the memory
                _context = null;
                Runtime.getRuntime().gc();
                if (_state == State.STARTING)
                    _starterThread.interrupt();
                if (_state == State.MANUAL_STOPPING) {
                    setState(State.MANUAL_STOPPED);
                } else if (_state == State.NETWORK_STOPPING) {
                    // start waiter handler
                    setState(State.WAITING);
                    _handler.postDelayed(new Waiter(), 10*1000);
                } else if (_state == State.STARTING || _state == State.RUNNING ||
                           _state == State.STOPPING) {
                    System.err.println(this + " died of unknown causes");
                    setState(State.STOPPED);
                    stopSelf();
                } else if (_state == State.MANUAL_QUITTING) {
                    setState(State.MANUAL_QUITTED);
                    stopSelf();
                }
            }
        }
    }

    private State getSavedState() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, 0);
        String stateString = prefs.getString(LAST_STATE, State.INIT.toString());
        for (State s : State.values()) {
            if (s.toString().equals(stateString))
                return s;
        }
        return State.INIT;
    }

    private void setState(State s) {
        _state = s;
        saveState();
    }

    /** @return success */
    private boolean saveState() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(LAST_STATE, _state.toString());
        return edit.commit();
    }
}
