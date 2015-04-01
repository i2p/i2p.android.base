package net.i2p.android.router.log;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import net.i2p.android.I2PActivityBase;
import net.i2p.android.router.R;
import net.i2p.android.router.SettingsActivity;

public class LogActivity extends I2PActivityBase implements
        LogFragment.OnEntrySelectedListener {
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private static final String SELECTED_LEVEL = "selected_level";

    private String[] mLevels;
    private Spinner mSpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multipane);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        mLevels = getResources().getStringArray(R.array.log_level_list);

        mSpinner = (Spinner) findViewById(R.id.main_spinner);
        mSpinner.setVisibility(View.VISIBLE);


        if (findViewById(R.id.detail_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        mSpinner.setAdapter(ArrayAdapter.createFromResource(this,
                R.array.log_level_list, android.R.layout.simple_spinner_dropdown_item));

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectLevel(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        if (savedInstanceState != null) {
            int selected = savedInstanceState.getInt(SELECTED_LEVEL);
            mSpinner.setSelection(selected);
        } else
            selectLevel(0);
    }

    private void selectLevel(int i) {
        String level = mLevels[i];
        LogFragment f = LogFragment.newInstance(level);
        // In two-pane mode, list items should be given the
        // 'activated' state when touched.
        if (mTwoPane)
            f.setActivateOnItemClick(true);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment, f, level).commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_base_actions, menu);
        // Help menu not needed (yet), hide
        // TODO: Unhide when Help finished
        //menu.findItem(R.id.menu_help).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            Intent intent = new Intent(LogActivity.this, SettingsActivity.class);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                intent.setAction("net.i2p.android.router.PREFS_LOGGING");
            } else { // TODO: Test if this works, fix if not
                Bundle args = new Bundle();
                args.putString("settings", "logging");
                intent.putExtras(args);
            }
            startActivity(intent);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_LEVEL, mSpinner.getSelectedItemPosition());
    }

    // LogFragment.OnEntrySelectedListener

    public void onEntrySelected(String entry) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            LogDetailFragment detailFrag = LogDetailFragment.newInstance(entry);
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.detail_fragment, detailFrag).commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, LogDetailActivity.class);
            detailIntent.putExtra(LogDetailFragment.LOG_ENTRY, entry);
            startActivity(detailIntent);
        }
    }
}
