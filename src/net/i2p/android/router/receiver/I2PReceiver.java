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
import net.i2p.android.router.util.Util;

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
        IntentFilter intents = new IntentFilter();
        intents.addAction(Intent.ACTION_TIME_CHANGED);
        intents.addAction(Intent.ACTION_TIME_TICK);  // once per minute, for testing
        intents.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        @SuppressWarnings("LeakingThisInConstructor")
        Intent registerReceiver = context.registerReceiver(this, intents);
        _wasConnected = Util.isConnected(context);
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Util.w("Got broadcast: " + action);

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean failover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
            boolean noConn = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            NetworkInfo other = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

         /*****
            Util.w("No conn? " + noConn + " failover? " + failover +
                               " info: " + info + " other: " + other);
            printInfo(info);
            printInfo(other);
            getInfo();
          *****/
        }

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ||
            action.equals(Intent.ACTION_TIME_TICK)) {
            boolean connected = Util.isConnected(context);
            if (_wasConnected && !connected) {
                // notify + 2 timer ticks
                if (++_unconnectedCount >= 3) {
                    RouterService svc = _routerService;
                    if (_isBound && svc != null) {
                        Util.w("********* Network down, already bound");
                        svc.networkStop();
                    } else {
                        Util.w("********* Network down, binding to router");
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

/****
    private static void printInfo(NetworkInfo ni) {
        if (ni == null) {
            Util.w("Network info is null");
            return;
        }
        Util.w(
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
****/

    private boolean bindRouter() {
        Intent intent = new Intent();
        intent.setClassName(_context, "net.i2p.android.router.service.RouterService");
        Util.w(this + " calling bindService");
        _connection = new RouterConnection();
        boolean success = _context.bindService(intent, _connection, 0);
        Util.w(this + " got from bindService: " + success);
        return success;
    }

    /** unused */
    public void unbindRouter() {
        if (_connection != null)
            _context.unbindService(_connection);
    }

    private class RouterConnection implements ServiceConnection {

        /** Stops the router when connected */
        public void onServiceConnected(ComponentName name, IBinder service) {
            RouterBinder binder = (RouterBinder) service;
            _routerService = binder.getService();
            _isBound = true;
            _unconnectedCount = 0;
            _wasConnected = false;
            Util.w("********* Network down, stopping router");
            _routerService.networkStop();
            // this doesn't work here... TODO where to unbind
            //_context.unbindService(this);
        }

        public void onServiceDisconnected(ComponentName name) {
            _isBound = false;
            _routerService = null;
            Util.w("********* Receiver unbinding from router");
        }
    }
}
