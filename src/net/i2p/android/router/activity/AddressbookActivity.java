package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.AddressbookFragment;
import net.i2p.android.router.fragment.WebFragment;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

public class AddressbookActivity extends I2PActivityBase
        implements AddressbookFragment.OnAddressSelectedListener,
        SearchView.OnQueryTextListener {
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private static final String SELECTED_TAB = "selected_tab";

    @Override
    protected boolean canUseTwoPanes() {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up action bar for tabs
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Router book tab
        AddressbookFragment rf = new AddressbookFragment();
        Bundle args = new Bundle();
        args.putString(AddressbookFragment.BOOK_NAME, "router");
        rf.setArguments(args);
        Tab tab = actionBar.newTab()
                .setText("Router")
                .setTabListener(new TabListener(rf));
        actionBar.addTab(tab);

        // Private book tab
        AddressbookFragment pf = new AddressbookFragment();
        args = new Bundle();
        args.putString(AddressbookFragment.BOOK_NAME, "private");
        pf.setArguments(args);
        tab = actionBar.newTab()
                .setText("Private")
                .setTabListener(new TabListener(pf));
        actionBar.addTab(tab);

        if (savedInstanceState != null) {
            int selected = savedInstanceState.getInt(SELECTED_TAB);
            actionBar.setSelectedNavigationItem(selected);
        }

        if (findViewById(R.id.detail_fragment) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_TAB,
                getSupportActionBar().getSelectedNavigationIndex());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_addressbook_actions, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.action_search_addressbook);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    // AddressbookFragment.OnAddressSelectedListener

    public void onAddressSelected(CharSequence host) {
        if (getIntent().getAction() == Intent.ACTION_PICK) {
            Intent result = new Intent();
            result.setData(Uri.parse("http://" + host));
            setResult(Activity.RESULT_OK, result);
            finish();
        } else {
            Intent wa = new Intent(this, WebActivity.class);
            wa.putExtra(WebFragment.HTML_URI, "http://" + host + '/');
            startActivity(wa);
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
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        if (f instanceof AddressbookFragment) {
            AddressbookFragment af = (AddressbookFragment) f;
            af.filterAddresses(query);
        }
    }
}
