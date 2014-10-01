package net.i2p.android.router.log;

import net.i2p.android.router.I2PFragmentBase;
import net.i2p.android.router.R;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_log_actions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_copy_logs:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setText(mEntry);
                } else {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText(
                            getString(R.string.i2p_android_logs), mEntry);
                    clipboard.setPrimaryClip(clip);
                }

                Toast.makeText(getActivity(), R.string.logs_copied_to_clipboard, Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
