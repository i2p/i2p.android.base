package net.i2p.android.router.addressbook;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.viewpagerindicator.TitlePageIndicator;

import net.i2p.android.router.R;
import net.i2p.android.router.util.NamingServiceUtil;
import net.i2p.android.router.util.Util;
import net.i2p.client.naming.NamingService;

public class AddressbookContainer extends Fragment
        implements AddressbookFragment.OnAddressSelectedListener,
        SearchView.OnQueryTextListener {
    public static final int ADD_WIZARD_REQUEST = 1;
    public static final String ADD_WIZARD_DATA = "add_wizard_data";

    /**
     * Whether or not the container is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    ViewPager mViewPager;
    FragmentPagerAdapter mFragPagerAdapter;

    private static final String FRAGMENT_ROUTER = "router_fragment";
    private static final String FRAGMENT_PRIVATE = "private_fragment";
    private static final int FRAGMENT_ID_ROUTER = 0;
    private static final int FRAGMENT_ID_PRIVATE = 1;
    AddressbookFragment mRouterFrag;
    AddressbookFragment mPrivateFrag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.container_addressbook, container, false);

        if (v.findViewById(R.id.right_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        if (savedInstanceState != null) {
            mRouterFrag = (AddressbookFragment) getChildFragmentManager().getFragment(
                    savedInstanceState, FRAGMENT_ROUTER);
            mPrivateFrag = (AddressbookFragment) getChildFragmentManager().getFragment(
                    savedInstanceState, FRAGMENT_PRIVATE);
        } else if (mTwoPane) {
            // TODO if these were instantiated in the background, wouldn't savedInstanceState != null?
            mRouterFrag = (AddressbookFragment) getChildFragmentManager().findFragmentById(R.id.left_fragment);
            mPrivateFrag = (AddressbookFragment) getChildFragmentManager().findFragmentById(R.id.right_fragment);

            // Set up the two pages
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            if (mRouterFrag == null) {
                mRouterFrag = AddressbookFragment.newInstance(AddressbookFragment.ROUTER_BOOK);
                ft.add(R.id.left_fragment, mRouterFrag);
            }
            if (mPrivateFrag == null) {
                mPrivateFrag = AddressbookFragment.newInstance(AddressbookFragment.PRIVATE_BOOK);
                ft.add(R.id.right_fragment, mPrivateFrag);
            }
            ft.commit();
        }

        if (!mTwoPane) {
            mViewPager = (ViewPager) v.findViewById(R.id.pager);
            TitlePageIndicator pageIndicator = (TitlePageIndicator) v.findViewById(R.id.page_indicator);
            mFragPagerAdapter = new AddressbookPagerAdapter(getActivity(), getChildFragmentManager());
            mViewPager.setAdapter(mFragPagerAdapter);
            pageIndicator.setViewPager(mViewPager);
        }

        return v;
    }

    public class AddressbookPagerAdapter extends FragmentPagerAdapter {
        private static final int NUM_ITEMS = 2;

        private Context mContext;

        public AddressbookPagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case FRAGMENT_ID_ROUTER:
                    return (mRouterFrag = AddressbookFragment.newInstance(AddressbookFragment.ROUTER_BOOK));
                case FRAGMENT_ID_PRIVATE:
                    return (mPrivateFrag = AddressbookFragment.newInstance(AddressbookFragment.PRIVATE_BOOK));
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case FRAGMENT_ID_ROUTER:
                    return mContext.getString(R.string.label_router);
                case FRAGMENT_ID_PRIVATE:
                    return mContext.getString(R.string.label_private);
                default:
                    return null;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.container_addressbook_actions, menu);
        Activity activity = getActivity();
        if (activity != null) {
            SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
            MenuItem searchItem = menu.findItem(R.id.action_search_addressbook);
            SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
            searchView.setOnQueryTextListener(this);
        }
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        setChildMenuVisibility(mRouterFrag, FRAGMENT_ID_ROUTER, menuVisible);
        setChildMenuVisibility(mPrivateFrag, FRAGMENT_ID_PRIVATE, menuVisible);
    }

    private void setChildMenuVisibility(Fragment fragment, int itemNumber, boolean menuVisible) {
        if (fragment != null) {
            if (mViewPager != null)
                menuVisible = menuVisible && mViewPager.getCurrentItem() == itemNumber;
            fragment.setMenuVisibility(menuVisible);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        setChildUserVisibleHint(mRouterFrag, FRAGMENT_ID_ROUTER, isVisibleToUser);
        setChildUserVisibleHint(mPrivateFrag, FRAGMENT_ID_PRIVATE, isVisibleToUser);
    }

    private void setChildUserVisibleHint(Fragment fragment, int itemNumber, boolean isVisibleToUser) {
        if (fragment != null) {
            if (mViewPager != null)
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
        if (mRouterFrag != null)
            getChildFragmentManager().putFragment(outState, FRAGMENT_ROUTER, mRouterFrag);
        if (mPrivateFrag != null)
            getChildFragmentManager().putFragment(outState, FRAGMENT_PRIVATE, mPrivateFrag);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_WIZARD_REQUEST &&
                resultCode == Activity.RESULT_OK) {
            // Save the new entry
            Bundle entryData = data.getExtras().getBundle(ADD_WIZARD_DATA);
            NamingService ns = NamingServiceUtil.getNamingService(Util.getRouterContext(),
                    AddressbookFragment.PRIVATE_BOOK);
            NamingServiceUtil.addFromWizard(getActivity(), ns, entryData, false);
            // The loader will be notified by the NamingService
        }
    }

    // AddressbookFragment.OnAddressSelectedListener

    public void onAddressSelected(CharSequence host) {
        if (Intent.ACTION_PICK.equals(getActivity().getIntent().getAction())) {
            Intent result = new Intent();
            result.setData(Uri.parse("http://" + host));
            getActivity().setResult(Activity.RESULT_OK, result);
            getActivity().finish();
        } else {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("http://" + host));
            startActivity(i);
        }
    }

    // SearchView.OnQueryTextListener

    public boolean onQueryTextChange(String newText) {
        filterAddresses(newText);
        return true;
    }

    public boolean onQueryTextSubmit(String query) {
        filterAddresses(query);
        return true;
    }

    private void filterAddresses(String query) {
        if (mRouterFrag != null)
            mRouterFrag.filterAddresses(query);
        if (mPrivateFrag != null)
            mPrivateFrag.filterAddresses(query);
    }
}
