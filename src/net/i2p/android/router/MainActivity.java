package net.i2p.android.router;

import java.io.File;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import net.i2p.android.router.R;
import net.i2p.android.router.service.IRouterState;
import net.i2p.android.router.service.IRouterStateCallback;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.util.Util;

public class MainActivity extends I2PActivityBase implements
        MainFragment.RouterControlListener,
        VersionDialog.VersionDialogListener {
    IRouterState mStateService = null;
    MainFragment mMainFragment = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start with the home view
        if (savedInstanceState == null) {
            mMainFragment = new MainFragment();
            mMainFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment, mMainFragment).commit();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        Util.d("Initializing...");
        InitActivities init = new InitActivities(this);
        init.debugStuff();
        init.initialize();
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mStateService != null) {
            try {
                if (mStateService.isStarted()) {
                    // Update for the current state.
                    Util.d("Fetching state.");
                    String curState = mStateService.getState();
                    Message msg = mHandler.obtainMessage(STATE_MSG);
                    msg.getData().putString("state", curState);
                    mHandler.sendMessage(msg);
                } else {
                    Util.d("StateService not started yet");
                }
            } catch (RemoteException e) {}
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu);
        inflater.inflate(R.menu.activity_base_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;

        case R.id.menu_about:
            AboutDialog dialog = new AboutDialog();
            dialog.show(getSupportFragmentManager(), "about");
            return true;

        case R.id.menu_help:
            Intent hi = new Intent(MainActivity.this, HelpActivity.class);
            hi.putExtra(HelpActivity.REFERRER, "main");
            startActivity(hi);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStop() {
        if (mStateService != null) {
            try {
                mStateService.unregisterCallback(mStateCallback);
            } catch (RemoteException e) {}
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

        super.onRouterBind(svc);
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
                    String curState = mStateService.getState();
                    Message msg = mHandler.obtainMessage(STATE_MSG);
                    msg.getData().putString("state", curState);
                    mHandler.sendMessage(msg);
                } else {
                    // Unbind
                    unbindService(mStateConnection);
                    mStateService = null;
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
        public void stateChanged(String newState) throws RemoteException {
            Message msg = mHandler.obtainMessage(STATE_MSG);
            msg.getData().putString("state", newState);
            mHandler.sendMessage(msg);
        }
    };

    private static final int STATE_MSG = 1;

    private Handler mHandler = new Handler() {
        private String lastRouterState = null;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case STATE_MSG:
                String state = msg.getData().getString("state");
                if (lastRouterState == null || !lastRouterState.equals(state)) {
                    if (mMainFragment == null)
                        mMainFragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment);
                    if (mMainFragment != null) {
                        mMainFragment.updateState(state);
                        lastRouterState = state;
                    }
                }
                break;
            default:
                super.handleMessage(msg);
            }
        }
    };

    // MainFragment.RouterControlListener

    public boolean shouldShowOnOff() {
        RouterService svc = _routerService;
        return (((svc == null) || (!_isBound) || svc.canManualStart())
                && Util.isConnected(this))
                || (svc != null && _isBound && svc.canManualStop());
    }

    public boolean shouldBeOn() {
        RouterService svc = _routerService;
        return svc != null && _isBound && svc.canManualStop();
    }

    public void onStartRouterClicked() {
        RouterService svc = _routerService;
        if(svc != null && _isBound) {
            setPref(PREF_AUTO_START, true);
            svc.manualStart();
        } else {
            (new File(_myDir, "wrapper.log")).delete();
            startRouter();
        }
    }

    public boolean onStopRouterClicked() {
        RouterService svc = _routerService;
        if(svc != null && _isBound) {
            setPref(PREF_AUTO_START, false);
            svc.manualQuit();
            return true;
        }
        return false;
    }

    // VersionDialog.VersionDialogListener

    public void onFirstRun() {
        mDrawerLayout.openDrawer(mDrawerList);
    }
}
