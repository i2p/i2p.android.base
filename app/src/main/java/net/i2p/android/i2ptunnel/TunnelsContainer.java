package net.i2p.android.i2ptunnel;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
//import android.support.v4.app.ActivityCompat;
import androidx.core.app.ActivityCompat;
//import android.support.v4.app.ActivityOptionsCompat;
import androidx.core.app.ActivityOptionsCompat;
//import android.support.v4.app.Fragment;
import androidx.fragment.app.Fragment;
//import android.support.v4.app.FragmentManager;
import androidx.fragment.app.FragmentManager;
//import android.support.v4.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentPagerAdapter;
//import android.support.v4.util.Pair;
import androidx.core.util.Pair;
//import android.support.v4.view.ViewPager;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import net.lucode.hackware.magicindicator.MagicIndicator;
import net.lucode.hackware.magicindicator.ViewPagerHelper;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView;
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.SimplePagerTitleView;
import androidx.core.content.ContextCompat;

import net.i2p.android.i2ptunnel.preferences.EditTunnelContainerFragment;
import net.i2p.android.i2ptunnel.util.TunnelUtil;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;
import net.i2p.android.util.FragmentUtils;
import net.i2p.app.ClientAppState;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.i2ptunnel.ui.TunnelConfig;
import net.i2p.router.RouterContext;

import java.util.List;

/**
 *  The top level Fragment of the tunnels tabs.
 *  Creates client and server TunnelListFragments,
 *  the options menu, and the new tunnel wizard button.
 */
public class TunnelsContainer extends Fragment implements
        FragmentUtils.TwoPaneProvider,
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
    MagicIndicator mPageIndicator;
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

    private static boolean showActions() {
        RouterContext rCtx = Util.getRouterContext();
        TunnelControllerGroup tcg = TunnelControllerGroup.getInstance();
        return rCtx != null && tcg != null &&
               tcg.getState() == ClientAppState.RUNNING;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.container_tunnels, container, false);

        mViewPager = v.findViewById(R.id.pager);
        mPageIndicator = v.findViewById(R.id.magic_indicator);
        
        mNewTunnel = v.findViewById(R.id.promoted_action);
        mNewTunnel.setVisibility(showActions() ? View.VISIBLE : View.GONE);

        if (v.findViewById(R.id.detail_fragment) != null) {
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
        
        // Initialize ViewPager and adapter
        mFragPagerAdapter = new TunnelsPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mFragPagerAdapter);

        setupMagicIndicator();

        // Setup New Tunnel button
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

        public TunnelsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case FRAGMENT_ID_CLIENT:
                    return (mClientFrag = TunnelListFragment.newInstance(true));
                case FRAGMENT_ID_SERVER:
                    return (mServerFrag = TunnelListFragment.newInstance(false));
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case FRAGMENT_ID_CLIENT:
                    return getActivity().getString(R.string.label_i2ptunnel_client);
                case FRAGMENT_ID_SERVER:
                    return getActivity().getString(R.string.label_i2ptunnel_server);
                default:
                    return null;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_i2ptunnel_list_actions, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean showActions = showActions();

        menu.findItem(R.id.action_start_all_tunnels).setVisible(showActions);
        menu.findItem(R.id.action_stop_all_tunnels).setVisible(showActions);
        menu.findItem(R.id.action_restart_all_tunnels).setVisible(showActions);

        // Was causing a NPE in version 4745238 (0.9.31)
        if (mNewTunnel != null) {
            mNewTunnel.setVisibility(showActions ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        TunnelControllerGroup tcg = TunnelControllerGroup.getInstance();
        if (tcg == null)
            return false;

        // Handle presses on the action bar items
        List<String> msgs;
        switch (item.getItemId()) {
            case R.id.action_start_all_tunnels:
                msgs = tcg.startAllControllers();
                break;
            case R.id.action_stop_all_tunnels:
                msgs = tcg.stopAllControllers();
                break;
            case R.id.action_restart_all_tunnels:
                // Do a manual stop-start cycle, because tcg.restartAllControllers() happens in the
                // foreground, whereas tcg.startAllControllers() fires off threads for starting.
                msgs = tcg.stopAllControllers();
                msgs.addAll(tcg.startAllControllers());
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        // TODO: Do something with the other messages
        if (msgs.size() > 0)
            Toast.makeText(getActivity().getApplicationContext(),
                    msgs.get(0), Toast.LENGTH_LONG).show();
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TUNNEL_WIZARD_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                Bundle tunnelData = data.getExtras().getBundle(TUNNEL_WIZARD_DATA);
                // ticket #2483
                if (tunnelData == null)
                    return;
                // TODO fetch earlier
                TunnelControllerGroup tcg = TunnelControllerGroup.getInstance();
                if (tcg == null) {
                    // router went away
                    Toast.makeText(getActivity().getApplicationContext(),
                                   R.string.router_not_running, Toast.LENGTH_LONG).show();
                    return;
                }
                TunnelConfig cfg = TunnelUtil.createConfigFromWizard(getActivity(), tcg, tunnelData);
                TunnelEntry tunnel = TunnelEntry.createNewTunnel(getActivity(), tcg, cfg);

                if (tunnel != null) {
                    if (tunnel.isClient() && mClientFrag != null)
                        mClientFrag.addTunnel(tunnel);
                    else if (mServerFrag != null)
                        mServerFrag.addTunnel(tunnel);
                }
            }
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

    // FragmentUtils.TwoPaneProvider

    public boolean isTwoPane() {
        return mTwoPane;
    }

    // TunnelListFragment.OnTunnelSelectedListener

    public final void onTunnelSelected(int tunnelId, Pair<View, String>[] pairs) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            try {
                TunnelDetailFragment detailFrag = TunnelDetailFragment.newInstance(tunnelId);
                getChildFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment, detailFrag)
                    .commitNow(); // Use commitNow() to execute synchronously
            } catch (Exception e) {
                Log.e("TunnelsContainer", "Failed to update detail fragment", e);
            }
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(getActivity(), TunnelDetailActivity.class);
            detailIntent.putExtra(TunnelDetailFragment.TUNNEL_ID, tunnelId);

            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    getActivity(), pairs);
            ActivityCompat.startActivity(getActivity(), detailIntent, options.toBundle());
        }
    }

    // TunnelDetailFragment.TunnelDetailListener

    @Override
    public void onEditTunnel(int tunnelId) {
        Fragment editFrag = EditTunnelContainerFragment.newInstance(tunnelId);
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

    private void setupMagicIndicator() {
        if (mPageIndicator == null || getContext() == null) {
            return;
        }
    
        CommonNavigator commonNavigator = new CommonNavigator(getContext());
        commonNavigator.setAdjustMode(true);  // Add this line for better spacing
        commonNavigator.setAdapter(new CommonNavigatorAdapter() {
            @Override
            public int getCount() {
                return mFragPagerAdapter.getCount();
            }
    
            @Override
            public IPagerTitleView getTitleView(Context context, final int index) {
                SimplePagerTitleView simplePagerTitleView = new ColorTransitionPagerTitleView(context);
                simplePagerTitleView.setText(mFragPagerAdapter.getPageTitle(index));
                simplePagerTitleView.setTextSize(16); // Add this line to increase text size
                simplePagerTitleView.setNormalColor(ContextCompat.getColor(context, 
                    R.color.primary_text_disabled_material_dark));
                simplePagerTitleView.setSelectedColor(ContextCompat.getColor(context,
                    R.color.primary_text_default_material_dark));
                simplePagerTitleView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mViewPager.setCurrentItem(index);
                    }
                });
                return simplePagerTitleView;
            }
    
            @Override
            public IPagerIndicator getIndicator(Context context) {
                LinePagerIndicator indicator = new LinePagerIndicator(context);
                indicator.setMode(LinePagerIndicator.MODE_WRAP_CONTENT);
                indicator.setColors(ContextCompat.getColor(context, R.color.primary));
                indicator.setLineHeight(dpToPx(context, 3));
                return indicator;
            }
        });
    
        mPageIndicator.setNavigator(commonNavigator);
        ViewPagerHelper.bind(mPageIndicator, mViewPager);
    }
    
    private int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
