package net.i2p.android.router.log;

import net.i2p.android.router.I2PFragmentBase;
import net.i2p.android.router.R;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LogDetailFragment extends I2PFragmentBase {
    public static final String LOG_ENTRY = "log_entry";

    private String mEntry;

    public static LogDetailFragment newInstance (String entry) {
        LogDetailFragment f = new LogDetailFragment();
        Bundle args = new Bundle();
        args.putString(LOG_ENTRY, entry);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_log_entry, container, false);

        mEntry = getArguments().getString(LOG_ENTRY);
        TextView tv = (TextView) v.findViewById(R.id.log_entry);
        tv.setMovementMethod(new ScrollingMovementMethod());
        tv.setText(mEntry);

        return v;
    }
}
