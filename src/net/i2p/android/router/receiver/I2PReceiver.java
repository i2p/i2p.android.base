package net.i2p.android.router.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import net.i2p.android.router.R;

public class I2PReceiver extends BroadcastReceiver {
    private final Context _context;

    public I2PReceiver(Context context) {
        super();
        _context = context;
        IntentFilter intents = new IntentFilter();
        intents.addAction(Intent.ACTION_TIME_CHANGED);
        intents.addAction(Intent.ACTION_TIME_TICK);  // once per minute, for testing
        intents.addAction(Intent.ACTION_SCREEN_OFF);
        intents.addAction(Intent.ACTION_SCREEN_ON);
        intents.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(this, intents);
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        System.out.println("Got broadcast: " + action);

        if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean failover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
            boolean noConn = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            NetworkInfo other = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

            System.out.println("No conn? " + noConn + " failover? " + failover + 
                               " info: " + info + " other: " + other);
            //ConnectivityManager cm = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }
    }
}
