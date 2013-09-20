package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.NetDbSummaryTableFragment;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;

public class NetDbActivity extends I2PActivityBase {
    static final int TAB_SUMMARY = 0;
    static final int TAB_ROUTERS = 1;
    static final int TAB_LEASESETS = 2;
    private static final String SELECTED_TAB = "selected_tab";

    NetDbPagerAdapter mNetDbPagerAdapter;
    ViewPager mViewPager;

    @Override
    protected boolean useViewPager() {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up action bar for tabs
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Set up NetDbPagerAdapter containing the categories
        mNetDbPagerAdapter = new NetDbPagerAdapter(getSupportFragmentManager());

        // Set up ViewPager for swiping between categories
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mNetDbPagerAdapter);
        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        mViewPager.setCurrentItem(position);
                    }
                });

        // Set up TabListener to update NetDbPagerAdapter with the
        // current section to display categories for
        ActionBar.TabListener tabListener = new ActionBar.TabListener() {

            public void onTabSelected(Tab tab, FragmentTransaction ft) {
                mNetDbPagerAdapter.setCurrentSection(tab.getPosition());
            }

            public void onTabUnselected(Tab tab, FragmentTransaction ft) {
                mNetDbPagerAdapter.setCurrentSection(TAB_SUMMARY);
            }

            public void onTabReselected(Tab tab, FragmentTransaction ft) {
                // User selected the already selected tab.
            }
        };

        actionBar.addTab(
                actionBar.newTab()
                        .setText("Statistics")
                        .setTabListener(tabListener));
        actionBar.addTab(
                actionBar.newTab()
                        .setText("Routers")
                        .setTabListener(tabListener));
        actionBar.addTab(
                actionBar.newTab()
                        .setText("LeaseSets")
                        .setTabListener(tabListener));

        if (savedInstanceState != null) {
            int selected = savedInstanceState.getInt(SELECTED_TAB);
            actionBar.setSelectedNavigationItem(selected);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_TAB,
                getSupportActionBar().getSelectedNavigationIndex());
    }

    public class NetDbPagerAdapter extends FragmentStatePagerAdapter {
        private int mSection;

        public NetDbPagerAdapter(FragmentManager fm) {
            super(fm);
            mSection = TAB_SUMMARY;
        }

        public void setCurrentSection(int section) {
            // Change the ViewPager item (in case the
            // new section doesn't have as many items)
            mViewPager.setCurrentItem(0);
            // Update the section
            mSection = section;
            notifyDataSetChanged();
        }

        @Override
        public Fragment getItem(int i) {
            switch (mSection) {
            case TAB_ROUTERS:
            case TAB_LEASESETS:
            default: // TAB_SUMMARY
                return NetDbSummaryTableFragment.newInstance(i);
            }
        }

        @Override
        public int getCount() {
            switch (mSection) {
            case TAB_ROUTERS:
                return 2;
            case TAB_LEASESETS:
                return 1;
            default: // TAB_SUMMARY
                return 3;
            }
        }

        @Override
        public CharSequence getPageTitle(int i) {
            switch (mSection) {
            case TAB_ROUTERS:
            case TAB_LEASESETS:
                return "CAT " + (i + 1);
            default: // TAB_SUMMARY
                switch (i) {
                case 1:
                    return "Countries";
                case 2:
                    return "Transports";
                default:
                    return "Versions";
                }
            } 
        }
    }
}
