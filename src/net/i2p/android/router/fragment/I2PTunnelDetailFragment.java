package net.i2p.android.router.fragment;

import net.i2p.android.router.R;
import net.i2p.android.router.loader.TunnelEntry;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class I2PTunnelDetailFragment extends Fragment {
    public static final String TUNNEL_ID = "tunnel_id";

    private TunnelEntry mTunnel;

    public static I2PTunnelDetailFragment newInstance(int tunnelId) {
        I2PTunnelDetailFragment f = new I2PTunnelDetailFragment();
        Bundle args = new Bundle();
        args.putInt(TUNNEL_ID, tunnelId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TunnelControllerGroup tcg;
        String error;
        try {
            tcg = TunnelControllerGroup.getInstance();
            error = tcg == null ? getResources().getString(R.string.i2ptunnel_not_initialized) : null;
        } catch (IllegalArgumentException iae) {
            tcg = null;
            error = iae.toString();
        }

        if (tcg == null) {
            // Show error
        } else if (getArguments().containsKey(TUNNEL_ID)) {
            int tunnelId = getArguments().getInt(TUNNEL_ID);
            mTunnel = new TunnelEntry(getActivity(),
                    tcg.getControllers().get(tunnelId),
                    tunnelId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_i2ptunnel_detail, container, false);

        if (mTunnel != null) {
            TextView name = (TextView) v.findViewById(R.id.tunnel_name);
            name.setText(mTunnel.getName());

            TextView type = (TextView) v.findViewById(R.id.tunnel_type);
            type.setText(mTunnel.getType());

            TextView description = (TextView) v.findViewById(R.id.tunnel_description);
            description.setText(mTunnel.getDescription());

            TextView targetIfacePort = (TextView) v.findViewById(R.id.tunnel_target_interface_port);
            targetIfacePort.setText(mTunnel.getIfacePort());

            TextView accessIfacePort = (TextView) v.findViewById(R.id.tunnel_access_interface_port);
            accessIfacePort.setText(mTunnel.getIfacePort());
        }

        return v;
    }
}
