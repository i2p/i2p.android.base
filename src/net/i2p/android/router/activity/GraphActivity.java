package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.GraphFragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

public class GraphActivity extends I2PActivityBase {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up action bar for drop-down list
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        mDrawerToggle.setDrawerIndicatorEnabled(false);

        SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.graph_list, android.R.layout.simple_spinner_dropdown_item);

        ActionBar.OnNavigationListener mNavigationListener = new ActionBar.OnNavigationListener() {
            String[] rates = getResources().getStringArray(R.array.graph_list);
            
            public boolean onNavigationItemSelected(int position, long itemId) {
                String rateName = rates[position];
                long period = (60 * 1000);
                GraphFragment f = GraphFragment.newInstance(rateName, period);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_fragment, f, rates[position]).commit();
                return true;
            }
        };

        actionBar.setListNavigationCallbacks(mSpinnerAdapter, mNavigationListener);
    }
}
