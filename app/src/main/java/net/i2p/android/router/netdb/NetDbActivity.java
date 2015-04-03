package net.i2p.android.router.netdb;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import net.i2p.android.I2PActivityBase;
import net.i2p.android.router.R;
import net.i2p.data.Hash;

public class NetDbActivity extends I2PActivityBase implements
        NetDbListFragment.OnEntrySelectedListener {
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private static final String SELECTED_PAGE = "selected_page";
    private static final int PAGE_STATS = 0;
    private static final int PAGE_ROUTERS = 1;

    private Spinner mSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multipane);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSpinner = (Spinner) findViewById(R.id.main_spinner);
        mSpinner.setVisibility(View.VISIBLE);

        mSpinner.setAdapter(ArrayAdapter.createFromResource(this,
                R.array.netdb_pages, android.R.layout.simple_spinner_dropdown_item));

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectPage(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        if (findViewById(R.id.detail_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        if (savedInstanceState != null) {
            int selected = savedInstanceState.getInt(SELECTED_PAGE);
            mSpinner.setSelection(selected);
        } else
            selectPage(PAGE_STATS);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_PAGE, mSpinner.getSelectedItemPosition());
    }

    private void selectPage(int page) {
        Fragment f;
        if (page == PAGE_STATS)
            f = new NetDbSummaryPagerFragment();
        else {
            f = new NetDbListFragment();
            Bundle args = new Bundle();
            args.putBoolean(NetDbListFragment.SHOW_ROUTERS, page == PAGE_ROUTERS);
            f.setArguments(args);
            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            if (mTwoPane)
                ((NetDbListFragment) f).setActivateOnItemClick(true);
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment, f).commit();
    }

    // NetDbListFragment.OnEntrySelectedListener

    public void onEntrySelected(boolean isRouterInfo, Hash entryHash) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            NetDbDetailFragment detailFrag = NetDbDetailFragment.newInstance(
                    isRouterInfo, entryHash);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment, detailFrag).commit();

            // If we are coming from a LS to a RI, change the tab
            int currentTab = mSpinner.getSelectedItemPosition();
            if (isRouterInfo && currentTab != PAGE_ROUTERS)
                selectPage(PAGE_ROUTERS);
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, NetDbDetailActivity.class);
            detailIntent.putExtra(NetDbDetailFragment.IS_RI, isRouterInfo);
            detailIntent.putExtra(NetDbDetailFragment.ENTRY_HASH,
                    entryHash.toBase64());
            startActivity(detailIntent);
        }
    }
}
