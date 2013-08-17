package net.i2p.android.router.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import net.i2p.I2PAppContext;
import net.i2p.android.router.R;
import net.i2p.android.router.activity.AddressbookSettingsActivity;
import net.i2p.client.naming.NamingService;

public class AddressbookFragment extends ListFragment {
    private ArrayAdapter<String> mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Grab context if router has started, otherwise create new
        // FIXME dup contexts, locking, ...
        I2PAppContext ctx = I2PAppContext.getCurrentContext();
        if (ctx == null) {
            Properties props = new Properties();
            String myDir = getActivity().getFilesDir().getAbsolutePath();
            props.setProperty("i2p.dir.base", myDir);
            props.setProperty("i2p.dir.config", myDir);
            ctx = new I2PAppContext(props);
        }

        // get the names
        NamingService ns = ctx.namingService();
        // After router shutdown we get nothing... why?
        Set<String> names = ns.getNames();

        // set the header
        int sz = names.size();
        if (sz > 0) {
            TextView tv = (TextView) getActivity().getLayoutInflater().inflate(R.layout.addressbook_header, null);
            if (sz > 1)
                tv.setText(sz + " hosts in address book.");
            else
                tv.setText("1 host in address book.");
            getListView().addHeaderView(tv, "", false);
        }
        // set the empty text
        setEmptyText("No hosts in address book, or your router is not up.");

        // set the list
        List<String> nameList = new ArrayList<String>(names);
        Collections.sort(nameList);
        mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.addressbook_list_item, nameList);
        setListAdapter(mAdapter);
    }

    @Override
    public void onListItemClick(ListView parent, View view, int pos, long id) {
        CharSequence host = ((TextView) view).getText();
        WebFragment f = new WebFragment();
        Bundle args = new Bundle();
        args.putString(WebFragment.HTML_URI, "http://" + host + '/');
        f.setArguments(args);
        getActivity().getSupportFragmentManager()
                     .beginTransaction()
                     .replace(R.id.main_content, f)
                     .addToBackStack(null)
                     .commit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	inflater.inflate(R.menu.fragment_addressbook_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        
        switch (item.getItemId()) {
            //case R.id.action_add_to_addressbook:
            //    return true;
            case R.id.action_addressbook_settings:
                Intent intent = new Intent(getActivity(), AddressbookSettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void filterAddresses(String query) {
        mAdapter.getFilter().filter(query);
    }
}
