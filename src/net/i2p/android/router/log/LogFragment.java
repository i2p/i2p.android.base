package net.i2p.android.router.log;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;
import net.i2p.I2PAppContext;
import net.i2p.android.router.R;

public class LogFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<String>> {
    public static final String LOG_LEVEL = "log_level";
    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    private static final int LEVEL_ERROR = 1;
    private static final int LEVEL_ALL = 2;

    OnEntrySelectedListener mEntrySelectedCallback;
    private LogAdapter mAdapter;
    private TextView mHeaderView;
    private String mLogLevel;
    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;
    private boolean mActivateOnItemClick = false;

    // Container Activity must implement this interface
    public interface OnEntrySelectedListener {
        public void onEntrySelected(String entry);
    }

    public static LogFragment newInstance(String level) {
        LogFragment f = new LogFragment();
        Bundle args = new Bundle();
        args.putString(LOG_LEVEL, level);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mEntrySelectedCallback = (OnEntrySelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnEntrySelectedListener");
        }

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState
                    .getInt(STATE_ACTIVATED_POSITION));
        }

        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(
                mActivateOnItemClick ? ListView.CHOICE_MODE_SINGLE
                        : ListView.CHOICE_MODE_NONE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new LogAdapter(getActivity());
        mLogLevel = getArguments().getString(LOG_LEVEL);

        // set the header
        mHeaderView = (TextView) getActivity().getLayoutInflater().inflate(R.layout.logs_header, null);
        getListView().addHeaderView(mHeaderView, "", false);

        setListAdapter(mAdapter);

        I2PAppContext ctx = I2PAppContext.getCurrentContext();
        if (ctx != null) {
            setEmptyText("ERROR".equals(mLogLevel) ?
                    "No error messages" : "No messages");

            setListShown(false);
            getLoaderManager().initLoader("ERROR".equals(mLogLevel) ?
                    LEVEL_ERROR : LEVEL_ALL, null, this);
        } else
            setEmptyText(getResources().getString(
                    R.string.router_not_running));
    }

    @Override
    public void onListItemClick(ListView parent, View view, int pos, long id) {
        super.onListItemClick(parent, view, pos, id);
        String entry = mAdapter.getItem(pos);
        mEntrySelectedCallback.onEntrySelected(entry);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        mActivateOnItemClick = activateOnItemClick;
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
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

    // LoaderManager.LoaderCallbacks<List<String>>

    public Loader<List<String>> onCreateLoader(int id, Bundle args) {
        return new LogLoader(getActivity(),
                I2PAppContext.getCurrentContext(), mLogLevel);
    }

    public void onLoadFinished(Loader<List<String>> loader,
            List<String> data) {
        if (loader.getId() == ("ERROR".equals(mLogLevel) ?
                LEVEL_ERROR : LEVEL_ALL)) {
            mAdapter.setData(data);
            String header = getHeader(data.size(), (mLogLevel == "ERROR"));
            mHeaderView.setText(header);

            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }
        }
    }

    public void onLoaderReset(Loader<List<String>> loader) {
        if (loader.getId() == ("ERROR".equals(mLogLevel) ?
                LEVEL_ERROR : LEVEL_ALL)) {
            mAdapter.setData(null);
        }
    }
}
