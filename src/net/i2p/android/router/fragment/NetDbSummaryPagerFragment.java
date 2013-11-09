package net.i2p.android.router.fragment;

import net.i2p.android.router.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class NetDbSummaryPagerFragment extends Fragment {
    NetDbPagerAdapter mNetDbPagerAdapter;
    ViewPager mViewPager;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.parentfragment_viewpager, container, false);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up NetDbPagerAdapter containing the categories
        mNetDbPagerAdapter = new NetDbPagerAdapter(getChildFragmentManager());

        // Set up ViewPager for swiping between categories
        mViewPager = (ViewPager) getActivity().findViewById(R.id.pager);
        mViewPager.setAdapter(mNetDbPagerAdapter);
        mViewPager.setOnPageChangeListener(
                new ViewPager.SimpleOnPageChangeListener() {
                    @Override
                    public void onPageSelected(int position) {
                        mViewPager.setCurrentItem(position);
                    }
                });
    }

    public class NetDbPagerAdapter extends FragmentStatePagerAdapter {
        public NetDbPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return NetDbSummaryTableFragment.newInstance(i);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int i) {
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
