package net.i2p.android.router.activity;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import net.i2p.I2PAppContext;
import net.i2p.android.router.R;

public class LogActivity extends ListActivity {

    boolean errorsOnly;

    final static String ERRORS_ONLY = "errors_only";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Grab context if router has started, otherwise create new
        // FIXME dup contexts, locking, ...
        List<String> msgs;
        String header;
        I2PAppContext ctx = I2PAppContext.getCurrentContext();
        if (ctx != null) {
            Intent intent = getIntent();
            errorsOnly = intent.getBooleanExtra(ERRORS_ONLY, false);
            if (errorsOnly) {
                msgs = ctx.logManager().getBuffer().getMostRecentCriticalMessages();
            } else {
                msgs = ctx.logManager().getBuffer().getMostRecentMessages();
            }
            int sz = msgs.size();
            if (sz == 0) {
                header = "No messages";
            } else if (sz == 1) {
                header = "1 message";
            } else {
                header = sz + " messages, newest first";
                Collections.reverse(msgs);
            }
        } else {
            msgs = Collections.EMPTY_LIST;
            header = "No messages";
        }

        // set the header
        TextView tv = (TextView) getLayoutInflater().inflate(R.layout.logs_header, null);
        tv.setText(header);
        ListView lv = getListView();
        lv.addHeaderView(tv, "", false);
        setListAdapter(new ArrayAdapter<String>(this, R.layout.logs_list_item, msgs));

        // set the callback
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int pos, long id) {
                // make it bigger or something
            }
        });
    }
}
