package net.i2p.android.router.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import net.i2p.android.router.util.Util;

import net.i2p.android.router.I2PConstants;
import net.i2p.android.router.service.RouterService;

public class OnBootReceiver extends BroadcastReceiver implements I2PConstants {
    public static final String PREF_START_ON_BOOT = ANDROID_PREF_PREFIX + "startOnBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Util.e("spoofed BOOT_COMPLETED");
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean startOnBoot = prefs.getBoolean(PREF_START_ON_BOOT, false);

        if (startOnBoot) {
            Intent routerService = new Intent(context, RouterService.class);
            // Ticket #2404
            try {
                context.startService(routerService);
            } catch (IllegalStateException ex) {
                Util.e("Error: ", ex);
            }
        }
    }
}
