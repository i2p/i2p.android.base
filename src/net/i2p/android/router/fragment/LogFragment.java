package net.i2p.android.router.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.Collections;
import java.util.List;
import net.i2p.I2PAppContext;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Util;

public class LogFragment extends ListFragment {

    String _logLevel;
    private Handler _handler;
    private Runnable _updater;
    private ArrayAdapter<String> _adap;
    private TextView _headerView;

    public static final String LOG_LEVEL = "log_level";
    private static final int MAX = 250;

    public static LogFragment newInstance(String level) {
        LogFragment f = new LogFragment();
        Bundle args = new Bundle();
        args.putString(LOG_LEVEL, level);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        // Grab context if router has started, otherwise create new
        // FIXME dup contexts, locking, ...
        List<String> msgs;
        String header;
        I2PAppContext ctx = I2PAppContext.getCurrentContext();
        if (ctx != null) {
            Bundle args = getArguments();
            _logLevel = args.getString(LOG_LEVEL);
            if ("ERROR".equals(_logLevel)) {
                msgs = ctx.logManager().getBuffer().getMostRecentCriticalMessages();
            } else {
                msgs = ctx.logManager().getBuffer().getMostRecentMessages();
            }
            int sz = msgs.size();
            header = getHeader(sz, ("ERROR".equals(_logLevel)));
            if (sz > 1) {
                Collections.reverse(msgs);
            }
        } else {
            //msgs = Collections.EMPTY_LIST;
            msgs = Collections.emptyList();
            header = "No messages, router has not started yet.";
        }

        // set the header
        _headerView = (TextView) getActivity().getLayoutInflater().inflate(R.layout.logs_header, null);
        _headerView.setText(header);
        ListView lv = getListView();
        lv.addHeaderView(_headerView, "", false);
        _adap = new ArrayAdapter<String>(getActivity(), R.layout.logs_list_item, msgs);
        setListAdapter(_adap);

/***
        // set the callback
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView parent, View view, int pos, long id) {
                // make it bigger or something
            }
        });
***/

        _handler = new Handler();
        _updater = new Updater();
    }

    @Override
    public void onStart() {
        super.onStart();
        _handler.removeCallbacks(_updater);
        _handler.postDelayed(_updater, 10*1000);
    }

    @Override
    public void onStop() {
        super.onStop();
        _handler.removeCallbacks(_updater);
    }

    private class Updater implements Runnable {
        private int counter;

        public void run() {
            I2PAppContext ctx = I2PAppContext.getCurrentContext();
            if (ctx != null) {
                List<String> msgs;
                if ("ERROR".equals(_logLevel)) {
                    msgs = ctx.logManager().getBuffer().getMostRecentCriticalMessages();
                } else {
                    msgs = ctx.logManager().getBuffer().getMostRecentMessages();
                }
		int sz = msgs.size();
                if (sz > 0) {
                    Collections.reverse(msgs);
                    String oldNewest = _adap.getCount() > 0 ? _adap.getItem(0) : null;
                    boolean changed = false;
                    for (int i = 0; i < sz; i++) {
                        String newItem = msgs.get(i);
                        if (newItem.equals(oldNewest))
                            break;
                        _adap.insert(newItem, i);
                        changed = true;
                    }
                    int newSz = _adap.getCount();
                    for (int i = newSz - 1; i > MAX; i--) {
                        _adap.remove(_adap.getItem(i));
                    }
                    if (changed) {
                        // fixme update header
                        newSz = _adap.getCount();
                        String header = getHeader(newSz, (_logLevel == "ERROR"));
                        _headerView.setText(header);
                        _adap.notifyDataSetChanged();
                    }
                }
            }
            // LogWriter only processes queue every 10 seconds
            _handler.postDelayed(this, 10*1000);
        }
    }

    /** fixme plurals */
    private static String getHeader(int sz, boolean errorsOnly) {
        if (errorsOnly) {
            if (sz == 0)
                return "No error messages";
            if (sz == 1)
                return "1 error message";
            return sz + " error messages, newest first";
        }
        if (sz == 0)
            return "No messages";
        if (sz == 1)
            return "1 message";
        return sz + " messages, newest first";
    }
}
