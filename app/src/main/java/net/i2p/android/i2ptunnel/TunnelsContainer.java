package net.i2p.android.i2ptunnel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
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
    FragmentPagerAdapter mFragPagerAdapter;

    private static final String FRAGMENT_CLIENT = "client_fragment";
    private static final String FRAGMENT_SERVER = "server_fragment";
    private static final int FRAGMENT_ID_CLIENT = 0;
    private static final int FRAGMENT_ID_SERVER = 1;
    TunnelListFragment mClientFrag;
    TunnelListFragment mServerFrag;

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

        if (savedInstanceState != null) {
            mClientFrag = (TunnelListFragment) getChildFragmentManager().getFragment(
                    savedInstanceState, FRAGMENT_CLIENT);
            mServerFrag = (TunnelListFragment) getChildFragmentManager().getFragment(
                    savedInstanceState, FRAGMENT_SERVER);
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

    public class TunnelsPagerAdapter extends FragmentPagerAdapter {
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
                case FRAGMENT_ID_CLIENT:
                    mClientFrag = TunnelListFragment.newInstance(true);
                    if (mTwoPane)
                        mClientFrag.setActivateOnItemClick(true);
                    return mClientFrag;
                case FRAGMENT_ID_SERVER:
                    mServerFrag = TunnelListFragment.newInstance(false);
                    if (mTwoPane)
                        mServerFrag.setActivateOnItemClick(true);
                    return mServerFrag;
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case FRAGMENT_ID_CLIENT:
                    return mContext.getString(R.string.label_i2ptunnel_client);
                case FRAGMENT_ID_SERVER:
                    return mContext.getString(R.string.label_i2ptunnel_server);
                default:
                    return null;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

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

                if (tunnel.isClient() && mClientFrag != null)
                    mClientFrag.addTunnel(tunnel);
                else if (mServerFrag != null)
                    mServerFrag.addTunnel(tunnel);
            }
        }
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        setChildMenuVisibility(mClientFrag, FRAGMENT_ID_CLIENT, menuVisible);
        setChildMenuVisibility(mServerFrag, FRAGMENT_ID_SERVER, menuVisible);
    }

    private void setChildMenuVisibility(Fragment fragment, int itemNumber, boolean menuVisible) {
        if (fragment != null) {
            menuVisible = menuVisible && mViewPager.getCurrentItem() == itemNumber;
            fragment.setMenuVisibility(menuVisible);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        setChildUserVisibleHint(mClientFrag, FRAGMENT_ID_CLIENT, isVisibleToUser);
        setChildUserVisibleHint(mServerFrag, FRAGMENT_ID_SERVER, isVisibleToUser);
    }

    private void setChildUserVisibleHint(Fragment fragment, int itemNumber, boolean isVisibleToUser) {
        if (fragment != null) {
            isVisibleToUser = isVisibleToUser && mViewPager.getCurrentItem() == itemNumber;
            fragment.setUserVisibleHint(isVisibleToUser);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Since the pager fragments don't have known tags or IDs, the only way to persist the
        // reference is to use putFragment/getFragment. Remember, we're not persisting the exact
        // Fragment instance. This mechanism simply gives us a way to persist access to the
        // 'current' fragment instance for the given fragment (which changes across orientation
        // changes).
        //
        // The outcome of all this is that the "Refresh" menu button refreshes the stream across
        // orientation changes.
        if (mClientFrag != null)
            getChildFragmentManager().putFragment(outState, FRAGMENT_CLIENT, mClientFrag);
        if (mServerFrag != null)
            getChildFragmentManager().putFragment(outState, FRAGMENT_SERVER, mServerFrag);
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
