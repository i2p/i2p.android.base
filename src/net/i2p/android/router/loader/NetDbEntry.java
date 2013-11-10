package net.i2p.android.router.loader;

import java.lang.reflect.Field;

import net.i2p.android.router.R;
import net.i2p.data.LeaseSet;
import net.i2p.data.RouterInfo;

public class NetDbEntry {
    private final boolean mIsRI;

    private final RouterInfo mRI;
    private final String mCountry;

    private final LeaseSet mLS;
    private final String mNick;

    public static NetDbEntry fromRouterInfo(RouterInfo ri, String country) {
        return new NetDbEntry(true, ri, country, null, "");
    }

    public static NetDbEntry fromLeaseSet(LeaseSet ls, String nick) {
        return new NetDbEntry(false, null, "", ls, nick);
    }

    public NetDbEntry(boolean isRI,
            RouterInfo ri, String country,
            LeaseSet ls, String nick) {
        mIsRI = isRI;

        mRI = ri;
        mCountry = country;

        mLS = ls;
        mNick = nick;
    }

    public boolean isRouterInfo() {
        return mIsRI;
    }

    public RouterInfo getRouterInfo() {
        return mRI;
    }

    public LeaseSet getLeaseSet() {
        return mLS;
    }

    public String getHash() {
        if (mIsRI) {
            return mRI.getIdentity().getHash().toBase64();
        } else {
            return mLS.getDestination().calculateHash().toBase64();
        }
    }

    public int getCountryIcon() {
        // http://daniel-codes.blogspot.com/2009/12/dynamically-retrieving-resources-in.html
        try {
            Class res = R.drawable.class;
            Field field = res.getField("flag_" + mCountry);
            return field.getInt(null);
        }
        catch (Exception e) {
            return 0;
        }
    }

    public String getNickname() {
        return mNick;
    }
}
