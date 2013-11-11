package net.i2p.android.router.loader;

import java.lang.reflect.Field;

import net.i2p.android.router.R;
import net.i2p.data.DatabaseEntry;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.data.LeaseSet;
import net.i2p.data.RouterInfo;
import net.i2p.router.RouterContext;
import net.i2p.router.TunnelPoolSettings;

public class NetDbEntry {
    private final boolean mIsRI;
    private final DatabaseEntry mEntry;

    private final String mCountry;

    private final String mNick;

    public static NetDbEntry fromRouterInfo(RouterContext ctx, RouterInfo ri) {
        String country = ctx.commSystem().getCountry(ri.getIdentity().getHash());
        return new NetDbEntry(true, ri, country, "");
    }

    public static NetDbEntry fromLeaseSet(RouterContext ctx, LeaseSet ls) {
        String nick;
        Destination dest = ls.getDestination();
        if (ctx.clientManager().isLocal(dest)) {
            TunnelPoolSettings in = ctx.tunnelManager().getInboundSettings(
                    dest.calculateHash());
            if (in != null && in.getDestinationNickname() != null)
                nick = in.getDestinationNickname();
            else
                nick = dest.toBase64().substring(0, 6);
        } else {
            String host = ctx.namingService().reverseLookup(dest);
            if (host != null)
                nick = host;
            else
                nick = dest.toBase64().substring(0, 6);
        }
        return new NetDbEntry(false, ls, "", nick);
    }

    public NetDbEntry(boolean isRI, DatabaseEntry entry,
            String country,
            String nick) {
        mIsRI = isRI;
        mEntry = entry;

        mCountry = country;

        mNick = nick;
    }

    public boolean isRouterInfo() {
        return mIsRI;
    }

    // General methods

    public Hash getHash() {
        return mEntry.getHash();
    }

    // RouterInfo-specific methods

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

    // LeaseSet-specific methods

    public String getNickname() {
        return mNick;
    }
}
