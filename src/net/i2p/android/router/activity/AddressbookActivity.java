package net.i2p.android.router.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import net.i2p.android.router.R;
import net.i2p.I2PAppContext;
import net.i2p.client.naming.NamingService;

public class AddressbookActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        Set<String> names = ns.getNames();

        // set the header
        TextView tv = (TextView) getLayoutInflater().inflate(R.layout.addressbook_header, null);
        tv.setText(names.size() + " hosts in address book. Start typing to filter.");
        ListView lv = getListView();
        lv.addHeaderView(tv, "", false);
        lv.setTextFilterEnabled(true);

        // set the list
        List<String> nameList = new ArrayList(names);
        Collections.sort(nameList);
        setListAdapter(new ArrayAdapter<String>(this, R.layout.addressbook_list_item, nameList));

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
}
