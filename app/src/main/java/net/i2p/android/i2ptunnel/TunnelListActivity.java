package net.i2p.android.i2ptunnel;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import net.i2p.android.router.I2PActivityBase;
import net.i2p.android.router.R;

public class TunnelListActivity extends I2PActivityBase implements
        TunnelListFragment.OnTunnelSelectedListener,
        TunnelDetailFragment.TunnelDetailListener {
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private static final String SELECTED_PAGE = "selected_page";
    private static final int PAGE_CLIENT = 0;

    private Spinner mSpinner;

    @Override
    protected boolean canUseTwoPanes() {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSpinner = (Spinner) findViewById(R.id.main_spinner);
        mSpinner.setVisibility(View.VISIBLE);

        mSpinner.setAdapter(ArrayAdapter.createFromResource(this,
                R.array.i2ptunnel_pages, android.R.layout.simple_spinner_dropdown_item));

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
            selectPage(PAGE_CLIENT);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_PAGE, mSpinner.getSelectedItemPosition());
    }

    private void selectPage(int page) {
        TunnelListFragment f = new TunnelListFragment();
        Bundle args = new Bundle();
        args.putBoolean(TunnelListFragment.SHOW_CLIENT_TUNNELS, page == PAGE_CLIENT);
        f.setArguments(args);

        // In two-pane mode, list items should be given the
        // 'activated' state when touched.
        if (mTwoPane)
            f.setActivateOnItemClick(true);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment, f).commit();
    }

    // TunnelListFragment.OnTunnelSelectedListener

    public void onTunnelSelected(int tunnelId) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            TunnelDetailFragment detailFrag = TunnelDetailFragment.newInstance(tunnelId);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment, detailFrag).commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, TunnelDetailActivity.class);
            detailIntent.putExtra(TunnelDetailFragment.TUNNEL_ID, tunnelId);
            startActivity(detailIntent);
        }
    }

    // TunnelDetailFragment.TunnelDetailListener

    @Override
    public void onEditTunnel(int tunnelId) {
        EditTunnelFragment editFrag = EditTunnelFragment.newInstance(tunnelId);
        getSupportFragmentManager().beginTransaction()
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
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.detail_fragment, detailFrag).commit();
            } else {
                TunnelDetailFragment detailFrag = (TunnelDetailFragment) getSupportFragmentManager().findFragmentById(R.id.detail_fragment);
                getSupportFragmentManager().beginTransaction()
                        .remove(detailFrag).commit();
            }
        }
    }
}
