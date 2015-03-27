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

import net.i2p.android.router.R;

public class AddressbookContainer extends Fragment
        implements AddressbookFragment.OnAddressSelectedListener,
        SearchView.OnQueryTextListener {
    /**
     * Whether or not the container is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    FragmentPagerAdapter mFragPagerAdapter;

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

            // Set up the two pages
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            if (getChildFragmentManager().findFragmentById(R.id.left_fragment) == null)
                ft.add(R.id.left_fragment, AddressbookFragment.newInstance(AddressbookFragment.ROUTER_BOOK));
            if (getChildFragmentManager().findFragmentById(R.id.right_fragment) == null)
                ft.add(R.id.right_fragment, AddressbookFragment.newInstance(AddressbookFragment.PRIVATE_BOOK));
            ft.commit();
        } else {
            ViewPager viewPager = (ViewPager) v.findViewById(R.id.pager);
            mFragPagerAdapter = new AddressbookPagerAdapter(getActivity(), getChildFragmentManager());
            viewPager.setAdapter(mFragPagerAdapter);
        }

        return v;
    }

    public static class AddressbookPagerAdapter extends FragmentPagerAdapter {
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
                case 0:
                    return AddressbookFragment.newInstance(AddressbookFragment.ROUTER_BOOK);
                case 1:
                    return AddressbookFragment.newInstance(AddressbookFragment.PRIVATE_BOOK);
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return mContext.getString(R.string.label_router);
                case 1:
                    return mContext.getString(R.string.label_private);
                default:
                    return null;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.activity_addressbook_actions, menu);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.action_search_addressbook);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setOnQueryTextListener(this);
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
        Fragment f = getChildFragmentManager().findFragmentById(R.id.main_fragment);
        if (f instanceof AddressbookFragment) {
            AddressbookFragment af = (AddressbookFragment) f;
            af.filterAddresses(query);
        }
    }
}
