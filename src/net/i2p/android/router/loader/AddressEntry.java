package net.i2p.android.router.loader;

public class AddressEntry {
    private final String mHostName;

    public AddressEntry(String hostName) {
        mHostName = hostName;
    }

    public String getHostName() {
        return mHostName;
    }
}
