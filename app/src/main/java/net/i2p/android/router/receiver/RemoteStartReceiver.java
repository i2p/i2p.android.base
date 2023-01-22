package net.i2p.android.router.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.util.Util;

public class RemoteStartReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if(Util.getRouterContext() == null){
            Intent rsIntent = new Intent(context, RouterService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                context.startForegroundService(rsIntent);
            } else {
                context.startService(rsIntent);
            }
            Toast.makeText(context, "Starting I2P Router", Toast.LENGTH_SHORT).show();
        }
    }
}
