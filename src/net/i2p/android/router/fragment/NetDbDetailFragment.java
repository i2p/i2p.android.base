package net.i2p.android.router.fragment;

import java.util.Map;
import java.util.Set;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.NetDbListFragment.OnEntrySelectedListener;
import net.i2p.android.router.loader.NetDbEntry;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataFormatException;
import net.i2p.data.DataHelper;
import net.i2p.data.Hash;
import net.i2p.data.Lease;
import net.i2p.data.LeaseSet;
import net.i2p.data.RouterAddress;
import net.i2p.data.RouterInfo;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class NetDbDetailFragment extends I2PFragmentBase {
    public static final String IS_RI = "is_routerinfo";
    public static final String ENTRY_HASH = "entry_hash";

    OnEntrySelectedListener mEntrySelectedCallback;
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
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mEntrySelectedCallback = (OnEntrySelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnEntrySelectedListener");
        }

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
    public void onRouterConnectionReady() {
        if (getRouterContext() != null && mEntry == null)
            loadEntry();
    }

    private void loadEntry() {
        if (getNetDb().isInitialized()) {
            Hash hash = new Hash();
            try {
                hash.fromBase64(getArguments().getString(ENTRY_HASH));
                if (getArguments().getBoolean(IS_RI)) {
                    // Load RouterInfo
                    RouterInfo ri = getNetDb().lookupRouterInfoLocally(hash);
                    if (ri != null)
                        loadRouterInfo(ri);
                    // TODO: Handle null case in UI
                } else {
                    // Load LeaseSet
                    LeaseSet ls = getNetDb().lookupLeaseSetLocally(hash);
                    if (ls != null)
                        loadLeaseSet(ls);
                    // TODO: Handle null case in UI
                }
            } catch (DataFormatException e) {
                Util.e(e.toString());
            }
        }
    }

    private void loadRouterInfo(RouterInfo ri) {
        mEntry = NetDbEntry.fromRouterInfo(getRouterContext(), ri);

        if (mEntry.isUs())
            getActivity().setTitle("Our info");
        else
            getActivity().setTitle("Peer info");

        TextView entryHash = (TextView) getView().findViewById(R.id.dbentry_hash);
        entryHash.setText(mEntry.getHash().toBase64());

        if (mEntry.isUs() && getRouter().isHidden()) {
            TextView pubLabel = (TextView) getView().findViewById(R.id.label_ri_published);
            pubLabel.setText("Hidden, Updated:");
        }

        TextView published = (TextView) getView().findViewById(R.id.ri_published);
        long age = getRouterContext().clock().now() - ri.getPublished();
        if (age > 0) {
            published.setText(DataHelper.formatDuration(age) + " ago");
        } else {
            // shouldn't happen
            published.setText(DataHelper.formatDuration(0-age) + " ago???");
        }

        LinearLayout addresses = (LinearLayout) getView().findViewById(R.id.ri_addresses);
        for (RouterAddress addr : ri.getAddresses()) {
            addAddress(addresses, addr);
        }

        TableLayout stats = (TableLayout) getView().findViewById(R.id.ri_stats);
        @SuppressWarnings("unchecked")
        Map<String, String> p = ri.getOptionsMap();
        for (Map.Entry<String,String> e : (Set<Map.Entry<String,String>>) p.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            addTableRow(stats, DataHelper.stripHTML(key), DataHelper.stripHTML(val));
        }
    }

    private void addAddress(LinearLayout addresses, RouterAddress addr) {
        TableLayout table = new TableLayout(getActivity());

        String style = addr.getTransportStyle();
        addTableRow(table, "Style", style);

        int cost = addr.getCost();
        if (!((style.equals("SSU") && cost == 5) || (style.equals("NTCP") && cost == 10)))
            addTableRow(table, "cost", ""+cost);

        @SuppressWarnings("unchecked")
        Map<String, String> p = addr.getOptionsMap();
        for (Map.Entry<String,String> e : (Set<Map.Entry<String,String>>) p.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            addTableRow(table, DataHelper.stripHTML(key), DataHelper.stripHTML(val));
        }

        addresses.addView(table);
    }

    private void loadLeaseSet(LeaseSet ls) {
        mEntry = NetDbEntry.fromLeaseSet(getRouterContext(), ls);

        getActivity().setTitle("LeaseSet");

        TextView nickname = (TextView) getView().findViewById(R.id.ls_nickname);
        nickname.setText(mEntry.getNickname());

        TextView type = (TextView) getView().findViewById(R.id.ls_type);
        if (mEntry.isLocal()) {
            if (mEntry.isUnpublished())
                type.setText("Local Unpublished Destination");
            else
                type.setText("Local Destination");
        }

        TextView entryHash = (TextView) getView().findViewById(R.id.dbentry_hash);
        entryHash.setText(mEntry.getHash().toBase64());

        TextView expiry = (TextView) getView().findViewById(R.id.ls_expiry);
        long exp = ls.getLatestLeaseDate() - getRouterContext().clock().now();
        if (exp > 0) {
            expiry.setText(DataHelper.formatDuration(exp));
        } else {
            TextView expiryLabel = (TextView) getView().findViewById(R.id.label_ls_expiry);
            expiryLabel.setText("Expired:");
            expiry.setText(DataHelper.formatDuration(exp) + " ago");
        }

        LinearLayout leases = (LinearLayout) getView().findViewById(R.id.ls_leases);
        for (int i = 0; i < ls.getLeaseCount(); i++) {
            Lease lease = ls.getLease(i);
            addLease(leases, lease, i);
        }
    }

    private void addLease(LinearLayout leases, Lease lease, int i) {
        TableLayout table = new TableLayout(getActivity());

        addTableRow(table, "Lease", ""+(i+1));

        TableRow gateway = new TableRow(getActivity());
        gateway.setPadding(10, 0, 0, 0);

        TextView gatewayLabel = new TextView(getActivity());
        gatewayLabel.setText("Gateway");

        Button gatewayButton = new Button(getActivity());
        gatewayButton.setText(lease.getGateway().toBase64().substring(0, 4));
        final Hash gatewayHash = lease.getGateway();
        gatewayButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                mEntrySelectedCallback.onEntrySelected(
                        true, gatewayHash);
            }
        });

        gateway.addView(gatewayLabel);
        gateway.addView(gatewayButton);

        table.addView(gateway);

        addTableRow(table, "Tunnel", ""+lease.getTunnelId().getTunnelId());

        leases.addView(table);
    }

    private void addTableRow(TableLayout table, String key, String val) {
        TableRow row;
        TextView tl1, tl2;

        row = new TableRow(getActivity());
        row.setPadding(10, 0, 0, 0);

        tl1 = new TextView(getActivity());
        tl2 = new TextView(getActivity());

        tl1.setText(key);
        tl2.setText(val);

        row.addView(tl1);
        row.addView(tl2);

        table.addView(row);
    }
}
