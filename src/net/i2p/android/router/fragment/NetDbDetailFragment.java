package net.i2p.android.router.fragment;

import net.i2p.android.router.R;
import net.i2p.android.router.loader.NetDbEntry;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataFormatException;
import net.i2p.data.Hash;
import net.i2p.data.LeaseSet;
import net.i2p.data.RouterInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class NetDbDetailFragment extends I2PFragmentBase {
    public static final String IS_RI = "is_routerinfo";
    public static final String ENTRY_HASH = "entry_hash";

    private NetDbEntry mEntry;

    public static NetDbDetailFragment newInstance(boolean isRI, Hash hash) {
        NetDbDetailFragment f = new NetDbDetailFragment();
        Bundle args = new Bundle();
        args.putBoolean(IS_RI, isRI);
        args.putString(ENTRY_HASH, hash.toBase64());
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v;
        if (getArguments().getBoolean(IS_RI)) {
            v = inflater.inflate(R.layout.fragment_netdb_router_detail, container, false);
        } else {
            v = inflater.inflate(R.layout.fragment_netdb_leaseset_detail, container, false);
        }
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getRouterContext() != null && mEntry == null)
            loadEntry();
    }

    // Called by NetDbDetailActivity
    public void onRouterBind() {
        if (mEntry == null)
            loadEntry();
    }

    private void loadEntry() {
        if (getNetDb().isInitialized()) {
            Hash hash = new Hash();
            try {
                hash.fromBase64(getArguments().getString(ENTRY_HASH));
                if (getArguments().getBoolean(IS_RI)) {
                    RouterInfo ri = getNetDb().lookupRouterInfoLocally(hash);
                    mEntry = NetDbEntry.fromRouterInfo(getRouterContext(), ri);
                } else {
                    LeaseSet ls = getNetDb().lookupLeaseSetLocally(hash);
                    mEntry = NetDbEntry.fromLeaseSet(getRouterContext(), ls);

                    TextView nickname = (TextView) getView().findViewById(R.id.ls_nickname);
                    nickname.setText(mEntry.getNickname());
                }

                TextView entryHash = (TextView) getView().findViewById(R.id.dbentry_hash);
                entryHash.setText(hash.toBase64());
            } catch (DataFormatException e) {
                Util.e(e.toString());
            }
        }
    }
}
