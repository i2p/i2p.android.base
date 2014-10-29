package net.i2p.android.router.addressbook;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import net.i2p.android.router.I2PActivityBase;
import net.i2p.android.router.R;
import net.i2p.android.router.web.WebActivity;
import net.i2p.android.router.web.WebFragment;

public class AddressbookActivity extends I2PActivityBase
        implements AddressbookFragment.OnAddressSelectedListener,
        SearchView.OnQueryTextListener {
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private static final String SELECTED_PAGE = "selected_page";
    private static final int PAGE_ROUTER = 0;

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
                R.array.addressbook_pages, android.R.layout.simple_spinner_dropdown_item));

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
            selectPage(PAGE_ROUTER);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SELECTED_PAGE, mSpinner.getSelectedItemPosition());
    }

    private void selectPage(int page) {
        AddressbookFragment f = new AddressbookFragment();
        Bundle args = new Bundle();
        args.putString(AddressbookFragment.BOOK_NAME,
                page == PAGE_ROUTER ?
                        AddressbookFragment.ROUTER_BOOK :
                        AddressbookFragment.PRIVATE_BOOK);
        f.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment, f).commit();
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
        if (Intent.ACTION_PICK.equals(getIntent().getAction())) {
            Intent result = new Intent();
            result.setData(Uri.parse("http://" + host));
            setResult(Activity.RESULT_OK, result);
            finish();
        } else {
            //Intent i = new Intent(Intent.ACTION_VIEW);
            //i.setData(Uri.parse("http://" + host));
            // XXX: Temporarily reverting to inbuilt browser
            // until an alternative browser is ready.
            Intent i = new Intent(this, WebActivity.class);
            i.putExtra(WebFragment.HTML_URI, "http://" + host + '/');
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
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        if (f instanceof AddressbookFragment) {
            AddressbookFragment af = (AddressbookFragment) f;
            af.filterAddresses(query);
        }
    }
}
