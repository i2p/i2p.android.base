package net.i2p.android.router.activity;

import net.i2p.android.router.R;
import net.i2p.android.router.fragment.AddressbookFragment;
import net.i2p.android.router.fragment.WebFragment;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;

public class AddressbookActivity extends I2PActivityBase
        implements AddressbookFragment.OnAddressSelectedListener,
        SearchView.OnQueryTextListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Start with the base view
        if (savedInstanceState == null) {
            AddressbookFragment f = new AddressbookFragment();
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_content, f).commit();
        }
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
        WebFragment f = new WebFragment();
        Bundle args = new Bundle();
        args.putString(WebFragment.HTML_URI, "http://" + host + '/');
        f.setArguments(args);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.main_content, f)
            .addToBackStack(null)
            .commit();
        
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
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_content);
        if (f instanceof AddressbookFragment) {
            AddressbookFragment af = (AddressbookFragment) f;
            af.filterAddresses(query);
        }
    }
}
