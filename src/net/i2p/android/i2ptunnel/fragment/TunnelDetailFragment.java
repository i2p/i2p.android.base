package net.i2p.android.i2ptunnel.fragment;

import java.util.List;

import net.i2p.android.i2ptunnel.activity.TunnelWizardActivity;
import net.i2p.android.i2ptunnel.fragment.TunnelListFragment.OnTunnelSelectedListener;
import net.i2p.android.i2ptunnel.loader.TunnelEntry;
import net.i2p.android.i2ptunnel.util.TunnelUtil;
import net.i2p.android.router.R;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class TunnelDetailFragment extends Fragment {
    public static final String TUNNEL_ID = "tunnel_id";

    OnTunnelDeletedListener mCallback;
    private TunnelControllerGroup mGroup;
    private TunnelEntry mTunnel;

    public static TunnelDetailFragment newInstance(int tunnelId) {
        TunnelDetailFragment f = new TunnelDetailFragment();
        Bundle args = new Bundle();
        args.putInt(TUNNEL_ID, tunnelId);
        f.setArguments(args);
        return f;
    }

    // Container Activity must implement this interface
    public interface OnTunnelDeletedListener {
        public void onTunnelDeleted(int tunnelId, int numTunnelsLeft);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnTunnelDeletedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTunnelDeletedListener");
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        String error;
        try {
            mGroup = TunnelControllerGroup.getInstance();
            error = mGroup == null ? getResources().getString(R.string.i2ptunnel_not_initialized) : null;
        } catch (IllegalArgumentException iae) {
            mGroup = null;
            error = iae.toString();
        }

        if (mGroup == null) {
            // Show error
        } else if (getArguments().containsKey(TUNNEL_ID)) {
            int tunnelId = getArguments().getInt(TUNNEL_ID);
            mTunnel = new TunnelEntry(getActivity(),
                    mGroup.getControllers().get(tunnelId),
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_i2ptunnel_detail_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_delete_tunnel:
                DialogFragment dg = new DialogFragment() {
                    @Override
                    public Dialog onCreateDialog(Bundle savedInstanceState) {
                        return new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.i2ptunnel_delete_confirm_message)
                            .setPositiveButton(R.string.i2ptunnel_delete_confirm_button,
                                    new DialogInterface.OnClickListener() {

                                        public void onClick(DialogInterface dialog, int which) {
                                            List<String> msgs = TunnelUtil.deleteTunnel(
                                                    getActivity(), mGroup, mTunnel.getId());
                                            dialog.dismiss();
                                            Toast.makeText(getActivity().getApplicationContext(),
                                                    msgs.get(0), Toast.LENGTH_LONG).show();
                                            mCallback.onTunnelDeleted(mTunnel.getId(),
                                                    mGroup.getControllers().size());
                                        }
                                    })
                            .setNegativeButton(android.R.string.cancel, null)
                            .create();
                    }
                };
                dg.show(getFragmentManager(), "delete_tunnel_dialog");
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
