package net.i2p.android.router.addressbook;

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

    /**
     * See item 8 from Josh Bloch's "Effective Java".
     *
     * @return the hashcode of this AddressEntry
     */
    @Override
    public int hashCode() {
        return 37 * mHostName.hashCode() + mDest.hashCode();
    }
}
