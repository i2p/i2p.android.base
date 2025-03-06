package net.i2p.android.i2ptunnel.preferences;

import android.os.Bundle;
//import android.support.v4.app.Fragment;
import androidx.fragment.app.Fragment;
//import android.support.v4.app.FragmentManager;
import androidx.fragment.app.FragmentManager;
//import android.support.v7.widget.Toolbar;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.i2p.android.router.R;

/**
 * A shim that emulates EditTunnelActivity to provide a Toolbar with navigation
 * in two-pane mode.
 */
public class EditTunnelContainerFragment extends Fragment {
    private static final String ARG_TUNNEL_ID = "tunnelId";

    public static EditTunnelContainerFragment newInstance(int tunnelId) {
        EditTunnelContainerFragment f = new EditTunnelContainerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TUNNEL_ID, tunnelId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_single_fragment, container, false);

        // Set the action bar
        Toolbar toolbar = (Toolbar) v.findViewById(R.id.main_toolbar);
        toolbar.setTitle(R.string.edit_tunnel);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Try and navigate back through the edit tunnel fragments.
                // Otherwise, pop us back off.
                FragmentManager fragmentManager = getChildFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 0)
                    fragmentManager.popBackStack();
                else
                    getFragmentManager().popBackStack();
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState == null) {
            int tunnelId = getArguments().getInt(ARG_TUNNEL_ID);
            BaseTunnelPreferenceFragment editFrag = GeneralTunnelPreferenceFragment.newInstance(tunnelId);
            getChildFragmentManager().beginTransaction()
                .add(R.id.fragment, editFrag).commit();
        }
    }
}
