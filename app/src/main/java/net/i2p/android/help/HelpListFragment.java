package net.i2p.android.help;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import net.i2p.android.router.R;

public class HelpListFragment extends ListFragment {
    OnEntrySelectedListener mEntrySelectedCallback;

    // Container Activity must implement this interface
    public interface OnEntrySelectedListener {
        void onEntrySelected(int entry);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(ArrayAdapter.createFromResource(getActivity(),
                R.array.help_categories, R.layout.listitem_text));
    }

    @Override
    public void onListItemClick(ListView parent, View view, int pos, long id) {
        super.onListItemClick(parent, view, pos, id);
        mEntrySelectedCallback.onEntrySelected(pos);
    }
}
