package net.i2p.android.router.addressbook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.i2p.addressbook.Daemon;
import net.i2p.android.help.HelpActivity;
import net.i2p.android.router.I2PFragmentBase;
import net.i2p.android.router.I2PFragmentBase.RouterContextProvider;
import net.i2p.android.router.R;
import net.i2p.android.router.util.NamingServiceUtil;
import net.i2p.client.naming.NamingService;
import net.i2p.router.RouterContext;

import java.util.List;

public class AddressbookFragment extends ListFragment implements
        I2PFragmentBase.RouterContextUser,
        LoaderManager.LoaderCallbacks<List<AddressEntry>> {
    public static final String BOOK_NAME = "book_name";
    public static final String ROUTER_BOOK = "hosts.txt";
    public static final String PRIVATE_BOOK = "privatehosts.txt";
    public static final String ADD_WIZARD_DATA = "add_wizard_data";

    private static final int ADD_WIZARD_REQUEST = 1;

    private static final int ROUTER_LOADER_ID = 1;
    private static final int PRIVATE_LOADER_ID = 2;

    private boolean mOnActivityCreated;
    private RouterContextProvider mRouterContextProvider;
    private OnAddressSelectedListener mCallback;
    private AddressEntryAdapter mAdapter;
    private String mBook;
    private String mCurFilter;

    private ImageButton mAddToAddressbook;

    // Set in onActivityResult()
    private Intent mAddWizardData;

    // Container Activity must implement this interface
    public interface OnAddressSelectedListener {
        public void onAddressSelected(CharSequence host);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mRouterContextProvider = (RouterContextProvider) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement RouterContextProvider");
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnAddressSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnAddressSelectedListener");
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create the list fragment's content view by calling the super method
        final View listFragmentView = super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(R.layout.fragment_list_with_add, container, false);
        FrameLayout listContainer = (FrameLayout) v.findViewById(R.id.list_container);
        listContainer.addView(listFragmentView);

        mAddToAddressbook = (ImageButton) v.findViewById(R.id.promoted_action);
        mAddToAddressbook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent wi = new Intent(getActivity(), AddressbookAddWizardActivity.class);
                startActivityForResult(wi, ADD_WIZARD_REQUEST);
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new AddressEntryAdapter(getActivity());
        mBook = getArguments().getString(BOOK_NAME);

        // Set adapter to null before setting the header
        setListAdapter(null);

        TextView v = new TextView(getActivity());
        v.setTag("addressbook_header");
        getListView().addHeaderView(v);

        setListAdapter(mAdapter);

        mOnActivityCreated = true;
        if (getRouterContext() != null)
            onRouterConnectionReady();
        else
            setEmptyText(getResources().getString(
                    R.string.router_not_running));
    }

    public void onRouterConnectionReady() {
        // Show actions
        if (mSearchAddressbook != null)
            mSearchAddressbook.setVisible(true);
        if (mAddToAddressbook != null && mAddToAddressbook.getVisibility() != View.VISIBLE)
            mAddToAddressbook.setVisibility(View.VISIBLE);

        if (mAddWizardData != null) {
            // Save the new entry
            Bundle entryData = mAddWizardData.getExtras().getBundle(ADD_WIZARD_DATA);
            NamingService ns = NamingServiceUtil.getNamingService(getRouterContext(), mBook);
            boolean success = NamingServiceUtil.addFromWizard(getActivity(), ns, entryData, false);
            if (success) {
                // Reload the list
                setListShown(false);
                getLoaderManager().restartLoader(PRIVATE_LOADER_ID, null, this);
            }
        } else {
            setEmptyText("No hosts in address book " + mBook);

            setListShown(false);
            getLoaderManager().initLoader(PRIVATE_BOOK.equals(mBook) ?
                    PRIVATE_LOADER_ID : ROUTER_LOADER_ID, null, this);
        }
    }

    @Override
    public void onListItemClick(ListView parent, View view, int pos, long id) {
        CharSequence host = ((TextView) view).getText();
        mCallback.onAddressSelected(host);
    }

    private MenuItem mSearchAddressbook;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_addressbook_actions, menu);

        mSearchAddressbook = menu.findItem(R.id.action_search_addressbook);

        // Hide until needed
        if (getRouterContext() == null) {
            mSearchAddressbook.setVisible(false);
            mAddToAddressbook.setVisibility(View.GONE);
        }

        // Only allow adding to private book 
        if (!PRIVATE_BOOK.equals(mBook)) {
            mAddToAddressbook.setVisibility(View.GONE);
            mAddToAddressbook = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items

        switch (item.getItemId()) {
            case R.id.action_reload_subscriptions:
                Daemon.wakeup();
                Toast.makeText(getActivity(), "Reloading subscriptions...",
                        Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_addressbook_settings:
                Intent si = new Intent(getActivity(), AddressbookSettingsActivity.class);
                startActivity(si);
                return true;
            case R.id.action_addressbook_help:
                Intent hi = new Intent(getActivity(), HelpActivity.class);
                hi.putExtra(HelpActivity.CATEGORY, HelpActivity.CAT_ADDRESSBOOK);
                startActivity(hi);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ADD_WIZARD_REQUEST &&
                resultCode == Activity.RESULT_OK &&
                PRIVATE_BOOK.equals(mBook)) {
            mAddWizardData = data;
        }
    }

    public void filterAddresses(String query) {
        mCurFilter = !TextUtils.isEmpty(query) ? query : null;
        if (getRouterContext() != null && mAdapter != null) {
            setListShown(false);
            getLoaderManager().restartLoader(PRIVATE_BOOK.equals(mBook) ?
                    PRIVATE_LOADER_ID : ROUTER_LOADER_ID, null, this);
        }
    }

    // Duplicated from I2PFragmentBase because this extends ListFragment
    private RouterContext getRouterContext() {
        return mRouterContextProvider.getRouterContext();
    }

    // I2PFragmentBase.RouterContextUser

    public void onRouterBind() {
        if (mOnActivityCreated)
            onRouterConnectionReady();
    }

    // LoaderManager.LoaderCallbacks<List<AddressEntry>>

    public Loader<List<AddressEntry>> onCreateLoader(int id, Bundle args) {
        return new AddressEntryLoader(getActivity(),
                getRouterContext(), mBook, mCurFilter);
    }

    public void onLoadFinished(Loader<List<AddressEntry>> loader,
                               List<AddressEntry> data) {
        if (loader.getId() == (PRIVATE_BOOK.equals(mBook) ?
                PRIVATE_LOADER_ID : ROUTER_LOADER_ID)) {
            mAdapter.setData(data);

            TextView v = (TextView) getListView().findViewWithTag("addressbook_header");
            if (mCurFilter != null)
                v.setText(getActivity().getResources().getString(
                        R.string.addressbook_search_header,
                        data.size()));
            else
                v.setText("");

            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }
        }
    }

    public void onLoaderReset(Loader<List<AddressEntry>> loader) {
        if (loader.getId() == (PRIVATE_BOOK.equals(mBook) ?
                PRIVATE_LOADER_ID : ROUTER_LOADER_ID)) {
            mAdapter.setData(null);
        }
    }
}
