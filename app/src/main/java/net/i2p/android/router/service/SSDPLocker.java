package net.i2p.android.router.service;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;

import net.i2p.router.transport.UPnPScannerCallback;

/**
 *  To lock/unlock UPnP, so it works on some phones.
 *  Many many phones don't require this, but do be safe...
 *
 *  @since 0.9.41
 */
public class SSDPLocker implements UPnPScannerCallback {

    private final MulticastLock lock;

    public SSDPLocker(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("ssdp");
        lock.setReferenceCounted(false);
    }

    public void beforeScan() {
        lock.acquire();
    }

    public void afterScan() {
        if (lock.isHeld())
            lock.release();
    }
}
