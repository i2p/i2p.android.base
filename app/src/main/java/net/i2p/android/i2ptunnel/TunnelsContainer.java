package net.i2p.android.i2ptunnel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import net.i2p.android.i2ptunnel.util.TunnelUtil;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import net.i2p.android.util.MemoryFragmentPagerAdapter;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.i2ptunnel.ui.TunnelConfig;

public class TunnelsContainer extends Fragment implements
        TunnelListFragment.OnTunnelSelectedListener,
        TunnelDetailFragment.TunnelDetailListener {
    static final int TUNNEL_WIZARD_REQUEST = 1;
    public static final String TUNNEL_WIZARD_DATA = "tunnel_wizard_data";

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    ViewPager mViewPager;
    MemoryFragmentPagerAdapter mFragPagerAdapter;

    private ImageButton mNewTunnel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.container_tunnels, container, false);

        mViewPager = (ViewPager) v.findViewById(R.id.pager);
        mNewTunnel = (ImageButton) v.findViewById(R.id.promoted_action);

        if (v.findViewById(R.id.detail_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFragPagerAdapter = new TunnelsPagerAdapter(getActivity(), getChildFragmentManager(), mTwoPane);
        mViewPager.setAdapter(mFragPagerAdapter);

        mNewTunnel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent wi = new Intent(getActivity(), TunnelWizardActivity.class);
                startActivityForResult(wi, TUNNEL_WIZARD_REQUEST);
            }
        });
    }

    public static class TunnelsPagerAdapter extends MemoryFragmentPagerAdapter {
        private static final int NUM_ITEMS = 2;

        private Context mContext;
        private boolean mTwoPane;

        public TunnelsPagerAdapter(Context context, FragmentManager fm, boolean twoPane) {
            super(fm);
            mContext = context;
            mTwoPane = twoPane;
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    TunnelListFragment cf = TunnelListFragment.newInstance(true);
                    if (mTwoPane)
                        cf.setActivateOnItemClick(true);
                    return cf;
                case 1:
                    TunnelListFragment sf = TunnelListFragment.newInstance(false);
                    if (mTwoPane)
                        sf.setActivateOnItemClick(true);
                    return sf;
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return mContext.getString(R.string.label_i2ptunnel_client);
                case 1:
                    return mContext.getString(R.string.label_i2ptunnel_server);
                default:
                    return null;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_i2ptunnel_list_actions, menu);
        if (Util.getRouterContext() == null) {
            mNewTunnel.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TUNNEL_WIZARD_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Bundle tunnelData = data.getExtras().getBundle(TUNNEL_WIZARD_DATA);
                // TODO fetch earlier
                TunnelControllerGroup tcg = TunnelControllerGroup.getInstance();
                TunnelConfig cfg = TunnelUtil.createConfigFromWizard(getActivity(), tcg, tunnelData);
                TunnelEntry tunnel = TunnelEntry.createNewTunnel(getActivity(), tcg, cfg);

                TunnelListFragment f = (TunnelListFragment) mFragPagerAdapter.getFragment(tunnel.isClient() ? 0 : 1);
                f.addTunnel(tunnel);
            }
        }
    }

    // TunnelListFragment.OnTunnelSelectedListener

    public void onTunnelSelected(int tunnelId) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            TunnelDetailFragment detailFrag = TunnelDetailFragment.newInstance(tunnelId);
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment, detailFrag).commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(getActivity(), TunnelDetailActivity.class);
            detailIntent.putExtra(TunnelDetailFragment.TUNNEL_ID, tunnelId);
            startActivity(detailIntent);
        }
    }

    // TunnelDetailFragment.TunnelDetailListener

    @Override
    public void onEditTunnel(int tunnelId) {
        EditTunnelFragment editFrag = EditTunnelFragment.newInstance(tunnelId);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.detail_fragment, editFrag)
                .addToBackStack("")
                .commit();
    }

    public void onTunnelDeleted(int tunnelId, int numTunnelsLeft) {
        // Should only get here in two-pane mode, but just to be safe:
        if (mTwoPane) {
            if (numTunnelsLeft > 0) {
                TunnelDetailFragment detailFrag = TunnelDetailFragment.newInstance(
                        (tunnelId > 0 ? tunnelId - 1 : 0));
                getChildFragmentManager().beginTransaction()
                        .replace(R.id.detail_fragment, detailFrag).commit();
            } else {
                TunnelDetailFragment detailFrag = (TunnelDetailFragment) getChildFragmentManager().findFragmentById(R.id.detail_fragment);
                getChildFragmentManager().beginTransaction()
                        .remove(detailFrag).commit();
            }
        }
    }
}
