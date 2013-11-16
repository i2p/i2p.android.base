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

    private final boolean mIsUs;
    private final String mCountry;

    private final String mNick;
    private final boolean mLocal;
    private final boolean mUnpublished;

    public static NetDbEntry fromRouterInfo(RouterContext ctx, RouterInfo ri) {
        Hash us = ctx.routerHash();
        boolean isUs = ri.getHash().equals(us);
        // XXX Disabled, no GeoIP file
        String country = "";//ctx.commSystem().getCountry(ri.getIdentity().getHash());
        return new NetDbEntry(ri, isUs, country);
    }

    public static NetDbEntry fromLeaseSet(RouterContext ctx, LeaseSet ls) {
        String nick = "";
        boolean local = false;
        boolean unpublished = false;
        Destination dest = ls.getDestination();
        Hash key = dest.calculateHash();
        if (ctx.clientManager().isLocal(dest)) {
            local = true;
            if (! ctx.clientManager().shouldPublishLeaseSet(key))
                unpublished = true;
            TunnelPoolSettings in = ctx.tunnelManager().getInboundSettings(key);
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
        return new NetDbEntry(ls, nick, local, unpublished);
    }

    public NetDbEntry(RouterInfo ri,
            boolean isUs, String country) {
        mIsRI = true;
        mEntry = ri;

        mIsUs = isUs;
        mCountry = country;

        mNick = "";
        mLocal = mUnpublished = false;
    }

    public NetDbEntry(LeaseSet ls,
        String nick, boolean local, boolean unpublished) {
        mIsRI = false;
        mEntry = ls;

        mNick = nick;
        mLocal = local;
        mUnpublished = unpublished;

        mIsUs = false;
        mCountry = "";
    }

    public boolean isRouterInfo() {
        return mIsRI;
    }

    // General methods

    public Hash getHash() {
        return mEntry.getHash();
    }

    // RouterInfo-specific methods

    public boolean isUs() {
        return mIsUs;
    }

    public int getCountryIcon() {
        // http://daniel-codes.blogspot.com/2009/12/dynamically-retrieving-resources-in.html
        try {
            Class<R.drawable> res = R.drawable.class;
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

    public boolean isLocal() {
        return mLocal;
    }

    public boolean isUnpublished() {
        return mUnpublished;
    }
}
