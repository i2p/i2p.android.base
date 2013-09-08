package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.LogFragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

public class LogActivity extends I2PActivityBase {
    private static final String SELECTED_LEVEL = "selected_level";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up action bar for drop-down list
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        mDrawerToggle.setDrawerIndicatorEnabled(false);

        SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.log_level_list, android.R.layout.simple_spinner_dropdown_item);

        ActionBar.OnNavigationListener mNavigationListener = new ActionBar.OnNavigationListener() {
            String[] levels = getResources().getStringArray(R.array.log_level_list);
            
            public boolean onNavigationItemSelected(int position, long itemId) {
                String level = levels[position];
                LogFragment f = LogFragment.newInstance(level);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_fragment, f, levels[position]).commit();
                return true;
            }
        };

        actionBar.setListNavigationCallbacks(mSpinnerAdapter, mNavigationListener);

        if (savedInstanceState != null) {
            int selected = savedInstanceState.getInt(SELECTED_LEVEL);
            actionBar.setSelectedNavigationItem(selected);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_LEVEL,
                getSupportActionBar().getSelectedNavigationIndex());
    }
}
