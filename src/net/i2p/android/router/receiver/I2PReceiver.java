package net.i2p.android.router.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

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
        System.out.println("Got broadcast: " + intent);

        ConnectivityManager cm = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
}
