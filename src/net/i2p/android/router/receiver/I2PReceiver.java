package net.i2p.android.router.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;

import net.i2p.android.router.binder.RouterBinder;
import net.i2p.android.router.service.RouterService;

public class I2PReceiver extends BroadcastReceiver {
    private final Context _context;
    private boolean _isBound;
    private boolean _wasConnected;
    private int _unconnectedCount;
    private RouterService _routerService;
    private ServiceConnection _connection;

    /**
     *  Registers itself
     */
    public I2PReceiver(Context context) {
        super();
        _context = context;
        getInfo();
        IntentFilter intents = new IntentFilter();
        intents.addAction(Intent.ACTION_TIME_CHANGED);
        intents.addAction(Intent.ACTION_TIME_TICK);  // once per minute, for testing
        intents.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(this, intents);
        _wasConnected = isConnected();
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        System.err.println("Got broadcast: " + action);

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean failover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
            boolean noConn = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            NetworkInfo other = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

         /*****
            System.err.println("No conn? " + noConn + " failover? " + failover + 
                               " info: " + info + " other: " + other);
            printInfo(info);
            printInfo(other);
            getInfo();
          *****/
        }

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
            action.equals(Intent.ACTION_TIME_TICK)) {
            boolean connected = isConnected();
            if (_wasConnected && !connected) {
                // notify + 2 timer ticks
                if (++_unconnectedCount >= 3) {
                    if (_isBound) {
                        System.err.println("********* Network down, already bound");
                        _routerService.networkStop();
                    } else {
                        System.err.println("********* Network down, binding to router");
                        // connection will call networkStop()
                        bindRouter();
                    }
                }
            } else {
                _wasConnected = connected;
                _unconnectedCount = 0;
            }
        }
    }

    public boolean isConnected() {
        NetworkInfo current = getInfo();
        return current != null && current.isConnected();
    }

    private NetworkInfo getInfo() {
        ConnectivityManager cm = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo current = cm.getActiveNetworkInfo();
        System.err.println("Current network info:");
        printInfo(current);
        return current;
    }

    private static void printInfo(NetworkInfo ni) {
        if (ni == null) {
            System.err.println("Network info is null");
            return;
        }
        System.err.println(
             "state: " + ni.getState() +
             " detail: " + ni.getDetailedState() +
             " extrainfo: " + ni.getExtraInfo() +
             " reason: " + ni.getReason() +
             " typename: " + ni.getTypeName() +
             " available: " + ni.isAvailable() +
             " connected: " + ni.isConnected() +
             " conorcon: " + ni.isConnectedOrConnecting() +
             " failover: " + ni.isFailover());

    }

    private boolean bindRouter() {
        Intent intent = new Intent();
        intent.setClassName(_context, "net.i2p.android.router.service.RouterService");
        System.err.println(this + " calling bindService");
        _connection = new RouterConnection();
        boolean success = _context.bindService(intent, _connection, 0);
        System.err.println(this + " got from bindService: " + success);
        return success;
    }

    /** unused */
    public void unbindRouter() {
        if (_connection != null)
            _context.unbindService(_connection);
    }

    private class RouterConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName name, IBinder service) {
            RouterBinder binder = (RouterBinder) service;
            _routerService = binder.getService();
            _isBound = true;
            _unconnectedCount = 0;
            _wasConnected = false;
            System.err.println("********* Network down, stopping router");
            _routerService.networkStop();
            // this doesn't work here... TODO where to unbind
            //_context.unbindService(this);
        }

        public void onServiceDisconnected(ComponentName name) {
            _isBound = false;
            System.err.println("********* Receiver unbinding from router");
        }
    }
}
