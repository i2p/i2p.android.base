package net.i2p.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import net.i2p.android.help.HelpActivity;
import net.i2p.android.i2ptunnel.TunnelsContainer;
import net.i2p.android.router.ConsoleContainer;
import net.i2p.android.router.MainFragment;
import net.i2p.android.router.R;
import net.i2p.android.router.SettingsActivity;
import net.i2p.android.router.addressbook.AddressbookContainer;
import net.i2p.android.router.service.IRouterState;
import net.i2p.android.router.service.IRouterStateCallback;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.service.State;
import net.i2p.android.router.util.Connectivity;
import net.i2p.android.router.util.Util;
import net.i2p.android.util.MemoryFragmentPagerAdapter;
import net.i2p.android.widget.SlidingTabLayout;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * The main activity of the app. Contains a ViewPager that holds the three main
 * views:
 * <ul>
 * <li>The console</li>
 * <li>The addressbook</li>
 * <li>The tunnel manager</li>
 * </ul>
 */
public class I2PActivity extends I2PActivityBase implements
        MainFragment.RouterControlListener {
    ViewPager mViewPager;
    ViewPagerAdapter mViewPagerAdapter;

    IRouterState mStateService = null;
    private boolean mAutoStartFromIntent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewpager);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPagerAdapter = new ViewPagerAdapter(this, getSupportFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);

        SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        // Center the tabs in the layout
        slidingTabLayout.setDistributeEvenly(true);
        // Customize tab color
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return getResources().getColor(R.color.accent);
            }
        });
        // Give the SlidingTabLayout the ViewPager
        slidingTabLayout.setViewPager(mViewPager);
    }

    public static class ViewPagerAdapter extends MemoryFragmentPagerAdapter {
        private static final int NUM_ITEMS = 3;

        private Context mContext;

        public ViewPagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new ConsoleContainer();
                case 1:
                    return new AddressbookContainer();
                case 2:
                    return new TunnelsContainer();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return mContext.getString(R.string.label_console);
                case 1:
                    return mContext.getString(R.string.label_addresses);
                case 2:
                    return mContext.getString(R.string.label_tunnels);
                default:
                    return null;
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        Util.d("Initializing...");
        InitActivities init = new InitActivities(this);
        init.debugStuff();
        init.initialize();
        super.onPostCreate(savedInstanceState);
        handleIntents();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntents();
    }

    private void handleIntents() {
        if (getIntent() == null)
            return;

        Intent intent = getIntent();
        String action = intent.getAction();

        if (action == null)
            return;

        if (action.equals("net.i2p.android.router.START_I2P")) {
            if (mViewPager.getCurrentItem() != 0)
                mViewPager.setCurrentItem(0, false);
            autoStart();
        }
    }

    private void autoStart() {
        if (canStart()) {
            if (Connectivity.isConnected(this)) {
                mAutoStartFromIntent = true;
                onStartRouterClicked();
            } else {
                // Not connected to a network
                // TODO: Notify user
            }
        } else {
            // TODO: Notify user
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_base_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.menu_help:
                Intent hi = new Intent(this, HelpActivity.class);
                switch (mViewPager.getCurrentItem()) {
                    case 1:
                        hi.putExtra(HelpActivity.CATEGORY, HelpActivity.CAT_ADDRESSBOOK);
                        break;
                    case 2:
                        hi.putExtra(HelpActivity.CATEGORY, HelpActivity.CAT_I2PTUNNEL);
                        break;
                }
                startActivity(hi);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mStateService != null) {
            try {
                if (mStateService.isStarted()) {
                    // Update for the current state.
                    Util.d("Fetching state.");
                    State curState = mStateService.getState();
                    Message msg = mHandler.obtainMessage(STATE_MSG);
                    msg.getData().putParcelable(MSG_DATA, curState);
                    mHandler.sendMessage(msg);
                } else {
                    Util.d("StateService not started yet");
                }
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public void onStop() {
        if (mStateService != null) {
            try {
                mStateService.unregisterCallback(mStateCallback);
            } catch (RemoteException e) {
            }
        }
        if (mTriedBindState)
            unbindService(mStateConnection);
        mTriedBindState = false;
        super.onStop();
    }

    @Override
    protected void onRouterBind(RouterService svc) {
        if (mStateService == null) {
            // Try binding for state updates.
            // Don't auto-create the RouterService.
            Intent intent = new Intent(IRouterState.class.getName());
            intent.setClassName(this, "net.i2p.android.router.service.RouterService");
            mTriedBindState = bindService(intent,
                    mStateConnection, 0);
            Util.d("Bind to IRouterState successful: " + mTriedBindState);
        }
    }

    private boolean mTriedBindState;
    private ServiceConnection mStateConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mStateService = IRouterState.Stub.asInterface(service);
            Util.d("StateService bound");
            try {
                if (mStateService.isStarted()) {
                    mStateService.registerCallback(mStateCallback);
                    // Update for the current state.
                    Util.d("Fetching state.");
                    State curState = mStateService.getState();
                    Message msg = mHandler.obtainMessage(STATE_MSG);
                    msg.getData().putParcelable(MSG_DATA, curState);
                    mHandler.sendMessage(msg);
                } else {
                    // Unbind
                    unbindService(mStateConnection);
                    mStateService = null;
                    mTriedBindState = false;
                }
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mStateService = null;
        }
    };

    private IRouterStateCallback mStateCallback = new IRouterStateCallback.Stub() {
        /**
         * This is called by the RouterService regularly to tell us about
         * new states.  Note that IPC calls are dispatched through a thread
         * pool running in each process, so the code executing here will
         * NOT be running in our main thread like most other things -- so,
         * to update the UI, we need to use a Handler to hop over there.
         */
        public void stateChanged(State newState) throws RemoteException {
            Message msg = mHandler.obtainMessage(STATE_MSG);
            msg.getData().putParcelable(MSG_DATA, newState);
            mHandler.sendMessage(msg);
        }
    };

    private static final int STATE_MSG = 1;
    private static final String MSG_DATA = "state";

    private Handler mHandler = new StateHandler(new WeakReference<>(this));

    private static class StateHandler extends Handler {
        WeakReference<I2PActivity> mReference;

        public StateHandler(WeakReference<I2PActivity> reference) {
            mReference = reference;
        }

        private State lastRouterState = null;

        @Override
        public void handleMessage(Message msg) {
            I2PActivity parent = mReference.get();
            if (parent == null)
                return;

            switch (msg.what) {
                case STATE_MSG:
                    State state = msg.getData().getParcelable(MSG_DATA);
                    if (lastRouterState == null || lastRouterState != state) {
                        if (parent.mViewPagerAdapter != null) {
                            ((ConsoleContainer) parent.mViewPagerAdapter.getFragment(0)).updateState(state);
                            lastRouterState = state;
                        }

                        if (state == State.RUNNING && parent.mAutoStartFromIntent) {
                            parent.setResult(Activity.RESULT_OK);
                            parent.finish();
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private boolean canStart() {
        RouterService svc = _routerService;
        return (svc == null) || (!_isBound) || svc.canManualStart();
    }

    private boolean canStop() {
        RouterService svc = _routerService;
        return svc != null && _isBound && svc.canManualStop();
    }

    // MainFragment.RouterControlListener

    public boolean shouldShowOnOff() {
        return (canStart() && Connectivity.isConnected(this)) || canStop();
    }

    public boolean shouldBeOn() {
        String action = getIntent().getAction();
        return (canStop()) ||
                (action != null && action.equals("net.i2p.android.router.START_I2P"));
    }

    public void onStartRouterClicked() {
        RouterService svc = _routerService;
        if (svc != null && _isBound) {
            setPref(PREF_AUTO_START, true);
            svc.manualStart();
        } else {
            (new File(Util.getFileDir(this), "wrapper.log")).delete();
            startRouter();
        }
    }

    public boolean onStopRouterClicked() {
        RouterService svc = _routerService;
        if (svc != null && _isBound) {
            setPref(PREF_AUTO_START, false);
            svc.manualQuit();
            return true;
        }
        return false;
    }
}
