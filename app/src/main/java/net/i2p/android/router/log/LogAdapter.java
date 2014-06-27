package net.i2p.android.router.log;

import java.util.List;

import net.i2p.android.router.R;

import android.content.Context;
import android.widget.ArrayAdapter;

public class LogAdapter extends ArrayAdapter<String> {

    public LogAdapter(Context context) {
        super(context, R.layout.listitem_logs);
    }

    public void setData(List<String> entries) {
        clear();
        if (entries != null) {
            for (String entry : entries) {
                add(entry);
            }
        }
    }
}
