package net.i2p.android.router.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import net.i2p.client.naming.NamingService;

public class AddressbookActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addressbook);

        // Grab context if router has started, otherwise create new
        // FIXME dup contexts, locking, ...
        I2PAppContext ctx = I2PAppContext.getCurrentContext();
        if (ctx == null) {
            Properties props = new Properties();
            String myDir = getFilesDir().getAbsolutePath();
            props.setProperty("i2p.dir.base", myDir);
            props.setProperty("i2p.dir.config", myDir);
            ctx = new I2PAppContext(props);
        }

        // get the names
        NamingService ns = ctx.namingService();
        // After router shutdown we get nothing... why?
        Set<String> names = ns.getNames();

        // set the header
        TextView tv = (TextView) getLayoutInflater().inflate(R.layout.addressbook_header, null);
        int sz = names.size();
        if (sz > 1)
            tv.setText(sz + " hosts in address book. Start typing to filter.");
        else if (sz > 0)
            tv.setText("1 host in address book.");
        else
            tv.setText("No hosts in address book, or your router is not up.");
        ListView lv = (ListView) findViewById(R.id.addressbook_list);
        lv.addHeaderView(tv, "", false);
        lv.setTextFilterEnabled(sz > 1);

        // set the list
        List<String> nameList = new ArrayList<String>(names);
        Collections.sort(nameList);
        lv.setAdapter(new ArrayAdapter<String>(this, R.layout.addressbook_list_item, nameList));

        // set the callback
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int pos, long id) {
                CharSequence host = ((TextView) view).getText();
                Intent intent = new Intent(view.getContext(), WebActivity.class);
                intent.setData(Uri.parse("http://" + host + '/'));
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.activity_addressbook_actions, menu);
    	return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_add_to_addressbook:
                return true;
            case R.id.action_addressbook_settings:
                Intent intent = new Intent(this, AddressbookSettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
