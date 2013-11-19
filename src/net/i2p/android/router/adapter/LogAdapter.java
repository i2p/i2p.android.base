package net.i2p.android.router.adapter;

import java.util.List;

import net.i2p.android.router.R;

import android.content.Context;
import android.widget.ArrayAdapter;

public class LogAdapter extends ArrayAdapter<String> {

    public LogAdapter(Context context) {
        super(context, R.layout.logs_list_item);
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
