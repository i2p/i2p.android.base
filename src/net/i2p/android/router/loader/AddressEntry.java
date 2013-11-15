package net.i2p.android.router.loader;

import net.i2p.data.Destination;

public class AddressEntry {
    private final String mHostName;
    private final Destination mDest;

    public AddressEntry(String hostName, Destination dest) {
        mHostName = hostName;
        mDest = dest;
    }

    public String getHostName() {
        return mHostName;
    }

    public Destination getDestination() {
        return mDest;
    }
}
