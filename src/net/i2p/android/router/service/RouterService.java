package net.i2p.android.router.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

import net.i2p.android.router.R;
import net.i2p.android.router.binder.RouterBinder;
import net.i2p.android.router.receiver.I2PReceiver;
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
    private enum State {INIT, WAITING, STARTING, RUNNING, STOPPING, STOPPED,
                        MANUAL_STOPPING, MANUAL_STOPPED,
                        NETWORK_STOPPING, NETWORK_STOPPED, RESTARTING}

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

    private static final String MARKER = "**************************************  ";

    @Override
    public void onCreate() {
        System.err.println(this + " onCreate called" +
                           " Current state is: " + _state);

        (new File(getFilesDir(), "wrapper.log")).delete();
        _myDir = getFilesDir().getAbsolutePath();
        Init init = new Init(this);
        init.debugStuff();
        init.initialize();
        //_apkPath = init.getAPKPath();
        _statusBar = new StatusBar(this);
        _binder = new RouterBinder(this);
        _handler = new Handler();
        _updater = new Updater();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.err.println(this + " onStart called" +
                           " Current state is: " + _state);
        synchronized (_stateLock) {
            if (_state != State.INIT)
                //return START_STICKY;
                return START_NOT_STICKY;
            _receiver = new I2PReceiver(this);
            if (_receiver.isConnected()) {
                _statusBar.update("I2P is starting up");
                _state = State.STARTING;
                _starterThread = new Thread(new Starter());
                _starterThread.start();
            } else {
                _statusBar.update("I2P is waiting for a network connection");
                _state = State.WAITING;
                _starterThread = new Thread(new Waiter());
                _starterThread.start();
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
            System.err.println(MARKER + this + " waiter thread" +
                           " Current state is: " + _state);
            while (_state == State.WAITING) {
                try {
                    Thread.sleep(30*1000);
                } catch (InterruptedException ie) {
                    break;
                }

                if (_receiver.isConnected()) {
                    synchronized (_stateLock) {
                        if (_state != State.WAITING)
                            break;
                        _statusBar.update("Network connected, I2P is starting up");
                        _state = State.STARTING;
                        _starterThread = new Thread(new Starter());
                        _starterThread.start();
                    }
                    break;
                }
            }
            System.err.println("waiter finished");
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
                _state = State.RUNNING;
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

        _statusBar.update(status, details);
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        System.err.println("onBind called" +
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
            _state != State.NETWORK_STOPPING)
            return null;
        return rv;
    }

    public boolean canManualStop() {
        return _state == State.WAITING || _state == State.STARTING || _state == State.RUNNING;
    }

    /**
     *  Stop and don't restart
     */
    public void manualStop() {
        System.err.println("manualStop called" +
                           " Current state is: " + _state);
        synchronized (_stateLock) {
            if (!canManualStop())
                return;
            if (_state == State.WAITING || _state == State.STARTING)
                _starterThread.interrupt();
            if (_state == State.STARTING || _state == State.RUNNING) {
                _statusBar.update("Stopping I2P");
                Thread stopperThread = new Thread(new Stopper(State.MANUAL_STOPPING, State.MANUAL_STOPPED));
                stopperThread.start();
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
            if (_state == State.WAITING || _state == State.STARTING)
                _starterThread.interrupt();
            if (_state == State.STARTING || _state == State.RUNNING) {
                _statusBar.update("Network disconnected, stopping I2P");
                // don't change state, let the shutdown hook do it
                Thread stopperThread = new Thread(new Stopper(State.NETWORK_STOPPING, State.NETWORK_STOPPING));
                stopperThread.start();
            }
        }
    }

    public boolean canManualStart() {
        return _state == State.MANUAL_STOPPED || _state == State.STOPPED;
    }

    public void manualStart() {
        System.err.println("restart called" +
                           " Current state is: " + _state);
        synchronized (_stateLock) {
            if (!canManualStart())
                return;
            _statusBar.update("I2P is starting up");
            _state = State.STARTING;
            _starterThread = new Thread(new Starter());
            _starterThread.start();
        }
    }

    // ******** end methods accessed from Activities and Receivers ************

    @Override
    public void onDestroy() {
        System.err.println("onDestroy called" +
                           " Current state is: " + _state);

        _handler.removeCallbacks(_updater);
        _statusBar.off(this);

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
            if (_state == State.WAITING || _state == State.STARTING)
                _starterThread.interrupt();
            if (_state == State.STARTING || _state == State.RUNNING) {
              // should this be in a thread?
                _statusBar.update("I2P is stopping");
                Thread stopperThread = new Thread(new Stopper(State.STOPPING, State.STOPPED));
                stopperThread.start();
            }
        }
    }

    private class Stopper implements Runnable {
        private final State nextState;
        private final State stopState;

        /** call holding statelock */
        public Stopper(State next, State stop) {
            nextState = next;
            stopState = stop;
            _state = next;
        }

        public void run() {
            System.err.println(MARKER + this + " stopper thread" +
                               " Current state is: " + _state);
            RouterContext ctx = _context;
            if (ctx != null)
                ctx.router().shutdown(Router.EXIT_HARD);
            _statusBar.off(RouterService.this);
            System.err.println("********** Router shutdown complete");
            synchronized (_stateLock) {
                if (_state == nextState)
                    _state = stopState;
            }
        }
    }

    private class ShutdownHook implements Runnable {
        public void run() {
            System.err.println(this + " shutdown hook" +
                               " Current state is: " + _state);
            _statusBar.update("I2P is shutting down");
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
                if (_state == State.WAITING || _state == State.STARTING)
                    _starterThread.interrupt();
                if (_state == State.WAITING || _state == State.STARTING ||
                    _state == State.RUNNING)
                    _state = State.STOPPING;
            }
        }
    }

    private class FinalShutdownHook implements Runnable {
        public void run() {
            System.err.println(this + " final shutdown hook" +
                               " Current state is: " + _state);
            _statusBar.off(RouterService.this);
            I2PReceiver rcvr = _receiver;

            synchronized (_stateLock) {
                // null out to release the memory
                _context = null;
                if (_state == State.WAITING || _state == State.STARTING)
                    _starterThread.interrupt();
                if (_state == State.MANUAL_STOPPING) {
                    _state = State.MANUAL_STOPPED;
                } else if (_state == State.NETWORK_STOPPING) {
                    // start waiter thread
                    _state = State.WAITING;
                    _starterThread = new Thread(new Waiter());
                    _starterThread.start();
                } else if (_state == State.STARTING || _state == State.RUNNING ||
                           _state == State.STOPPING) {
                    _state = State.STOPPED;
                    stopSelf();
                }
            }
        }
    }
}
