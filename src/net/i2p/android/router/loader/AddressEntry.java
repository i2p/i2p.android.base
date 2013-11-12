package net.i2p.android.router.loader;

public class AddressEntry implements Comparable<Object> {
    private final String mHostName;

    public AddressEntry(String hostName) {
        mHostName = hostName;
    }

    public String getHostName() {
        return mHostName;
    }

    public int compareTo(Object another) {
        if (another instanceof AddressEntry)
            return -1;
        return mHostName.compareTo(((AddressEntry) another).getHostName());
    }
}
