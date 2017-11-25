package net.i2p.android.router.addressbook;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.eowise.recyclerview.stickyheaders.StickyHeadersBuilder;
import com.eowise.recyclerview.stickyheaders.StickyHeadersItemDecoration;
import com.pnikosis.materialishprogress.ProgressWheel;

import net.i2p.android.router.R;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.service.State;
import net.i2p.android.router.util.Util;
import net.i2p.android.util.AlphanumericHeaderAdapter;
import net.i2p.android.util.FragmentUtils;
import net.i2p.android.widget.DividerItemDecoration;
import net.i2p.android.widget.LoadingRecyclerView;
import net.i2p.router.RouterContext;

import java.util.ArrayList;
import java.util.List;

public class AddressbookFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<List<AddressEntry>> {
    public static final String BOOK_NAME = "book_name";
    public static final String ROUTER_BOOK = "hosts.txt";
    public static final String PRIVATE_BOOK = "privatehosts.txt";

    private static final int ROUTER_LOADER_ID = 1;
    private static final int PRIVATE_LOADER_ID = 2;

    private OnAddressSelectedListener mCallback;

    private LoadingRecyclerView mRecyclerView;
    private AddressEntryAdapter mAdapter;
    private String mBook;
    private String mCurFilter;

    private ImageButton mAddToAddressbook;

    // Container Activity must implement this interface
    public interface OnAddressSelectedListener {
        void onAddressSelected(CharSequence host);
    }

    public static AddressbookFragment newInstance(String book) {
        AddressbookFragment f = new AddressbookFragment();
        Bundle args = new Bundle();
        args.putString(AddressbookFragment.BOOK_NAME, book);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        mCallback = FragmentUtils.getParent(this, OnAddressSelectedListener.class);
        if (mCallback == null)
            throw new ClassCastException("Parent must implement OnAddressSelectedListener");

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list_with_add, container, false);

        mRecyclerView = (LoadingRecyclerView) v.findViewById(R.id.list);
        View empty = v.findViewById(R.id.empty);
        ProgressWheel loading = (ProgressWheel) v.findViewById(R.id.loading);
        mRecyclerView.setLoadingView(empty, loading);

        mAddToAddressbook = (ImageButton) v.findViewById(R.id.promoted_action);
        mAddToAddressbook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent wi = new Intent(getActivity(), AddressbookAddWizardActivity.class);
                getParentFragment().startActivityForResult(wi, AddressbookContainer.ADD_WIZARD_REQUEST);
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mBook = getArguments().getString(BOOK_NAME);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));

        // use a linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Set the adapter for the list view
        mAdapter = new AddressEntryAdapter(getActivity(), mCallback);
        mRecyclerView.setAdapter(mAdapter);

        // Build item decoration and add it to the RecyclerView
        StickyHeadersItemDecoration decoration = new StickyHeadersBuilder()
                .setAdapter(mAdapter)
                .setRecyclerView(mRecyclerView)
                .setStickyHeadersAdapter(new AlphanumericHeaderAdapter(mAdapter))
                .build();
        mRecyclerView.addItemDecoration(decoration);

        // Initialize the adapter in case the RouterService has not been created
        if (Util.getRouterContext() == null)
            mAdapter.setAddresses(null);
    }

    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());

        IntentFilter filter = new IntentFilter();
        filter.addAction(RouterService.LOCAL_BROADCAST_STATE_NOTIFICATION);
        filter.addAction(RouterService.LOCAL_BROADCAST_STATE_CHANGED);
        lbm.registerReceiver(onStateChange, filter);
    }

    private State lastRouterState = null;
    private BroadcastReceiver onStateChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            State state = intent.getParcelableExtra(RouterService.LOCAL_BROADCAST_EXTRA_STATE);
            if (lastRouterState == null || lastRouterState != state) {
                updateState(state);
                lastRouterState = state;
            }
        }
    };

    public void updateState(State state) {
        int loaderId = PRIVATE_BOOK.equals(mBook) ?
                PRIVATE_LOADER_ID : ROUTER_LOADER_ID;

        try {
            LoaderManager manager = getLoaderManager();
            if (state == State.INIT ||
                    state == State.STARTING || // Wait until RouterContext is initialised
                    state == State.STOPPING ||
                    state == State.STOPPED ||
                    state == State.MANUAL_STOPPING ||
                    state == State.MANUAL_STOPPED ||
                    state == State.MANUAL_QUITTING ||
                    state == State.MANUAL_QUITTED)
                manager.destroyLoader(loaderId);
            else {
                mRecyclerView.setLoading(true);
                manager.initLoader(loaderId, null, this);
            }
        } catch (IllegalStateException ise) {
            // Fragment isn't attached to any activity, so ignore state change
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Triggers loader init via updateState() if the router is running
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(RouterService.LOCAL_BROADCAST_REQUEST_STATE));
    }

    @Override
    public void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(onStateChange);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_addressbook_actions, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        RouterContext rCtx = Util.getRouterContext();

        if (mAddToAddressbook != null)
            mAddToAddressbook.setVisibility(rCtx == null ? View.GONE : View.VISIBLE);

        // Only show "Reload subscriptions" for router addressbook
        MenuItem reloadSubs = menu.findItem(R.id.action_reload_subscriptions);
        if (reloadSubs != null) {
            reloadSubs.setVisible(
                    rCtx != null && !PRIVATE_BOOK.equals(mBook));
        }

        // Only allow adding to private book 
        if (!PRIVATE_BOOK.equals(mBook) && mAddToAddressbook != null) {
            mAddToAddressbook.setVisibility(View.GONE);
            mAddToAddressbook = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items

        switch (item.getItemId()) {
            case R.id.action_reload_subscriptions:
                RouterContext rCtx = Util.getRouterContext();
                if (rCtx != null) {
                    rCtx.namingService().requestUpdate(null);
                    Toast.makeText(getActivity(), "Reloading subscriptions...",
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void filterAddresses(String query) {
        mCurFilter = !TextUtils.isEmpty(query) ? query : null;
        if (Util.getRouterContext() != null && mAdapter != null) {
            mRecyclerView.setLoading(true);
            getLoaderManager().restartLoader(PRIVATE_BOOK.equals(mBook) ?
                    PRIVATE_LOADER_ID : ROUTER_LOADER_ID, null, this);
        }
    }

    // LoaderManager.LoaderCallbacks<List<AddressEntry>>

    public Loader<List<AddressEntry>> onCreateLoader(int id, Bundle args) {
        return new AddressEntryLoader(getActivity(), mBook, mCurFilter);
    }

    public void onLoadFinished(Loader<List<AddressEntry>> loader,
                               List<AddressEntry> data) {
        if (loader.getId() == (PRIVATE_BOOK.equals(mBook) ?
                PRIVATE_LOADER_ID : ROUTER_LOADER_ID)) {
            mAdapter.setAddresses(data);
        }
    }

    public void onLoaderReset(Loader<List<AddressEntry>> loader) {
        if (loader.getId() == (PRIVATE_BOOK.equals(mBook) ?
                PRIVATE_LOADER_ID : ROUTER_LOADER_ID)) {
            if (Util.getRouterContext() == null)
                mAdapter.setAddresses(null);
            else
                mAdapter.setAddresses(new ArrayList<AddressEntry>());
        }
    }
}
