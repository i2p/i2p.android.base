package net.i2p.android.router;

import java.io.File;
import java.util.List;
import java.util.Properties;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import net.i2p.android.router.R;
import net.i2p.android.router.dialog.AboutDialog;
import net.i2p.android.router.dialog.TextResourceDialog;
import net.i2p.android.router.service.IRouterState;
import net.i2p.android.router.service.IRouterStateCallback;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.util.Util;
import net.i2p.router.RouterContext;
import net.i2p.util.OrderedProperties;

public class MainActivity extends I2PActivityBase implements
        MainFragment.RouterControlListener {
    IRouterState mStateService = null;
    MainFragment mMainFragment = null;
    private boolean mAutoStartFromIntent = false;

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

        // Open nav drawer if the user has never opened it themselves
        if (!getPref(PREF_NAV_DRAWER_OPENED, false))
            mDrawerLayout.openDrawer(mDrawerList);
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
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if (!prefs.getBoolean("i2cp.disableInterface", false)) {
                // Inverted, see Util.getPropertiesFromPreferences()
                // Ask user if we should enable I2CP
                DialogFragment df = new DialogFragment() {
                    @Override
                    public Dialog onCreateDialog(Bundle savedInstanceState) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(R.string.enable_i2cp)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                enableI2CP();
                                // I2P must be restarted
                                autoStart(true);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                setResult(RESULT_CANCELED);
                                finish();
                            }
                        });
                        return builder.create();
                    }
                };
                df.show(getSupportFragmentManager(), "enablei2cp");
            } else
                autoStart(false);
        }
    }

    private void enableI2CP() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("i2cp.disableInterface", true).commit();

        // Apply new config if we are running.
        List<RouterContext> contexts = RouterContext.listContexts();
        if ( !((contexts == null) || (contexts.isEmpty())) ) {
            RouterContext _context = contexts.get(0);
            _context.router().saveConfig("i2cp.disableInterface", "false");
        } else {
            // Merge in new config settings, write the file.
            Properties props = new OrderedProperties();
            props.setProperty("i2cp.disableInterface", "false");
            InitActivities init = new InitActivities(this);
            init.mergeResourceToFile(R.raw.router_config, "router.config", props);
        }
    }

    private void autoStart(boolean restartIfStarted) {
        if (canStart()) {
            if (Util.isConnected(this)) {
                mAutoStartFromIntent = true;
                onStartRouterClicked();
            } else {
                // Not connected to a network
                // TODO: Notify user
            }
        } else if (restartIfStarted) {
            // TODO: Stop and start
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

        // TODO: Unhide when Help page finished
        //case R.id.menu_help:
        //    Intent hi = new Intent(MainActivity.this, HelpActivity.class);
        //    hi.putExtra(HelpActivity.REFERRER, "main");
        //    startActivity(hi);
        //    return true;

        // TODO: Remove when help page finished
        case R.id.menu_help_licenses:
            Intent lic = new Intent(MainActivity.this, LicenseActivity.class);
            startActivity(lic);
            return true;
        case R.id.menu_help_release_notes:
            TextResourceDialog rDdialog = new TextResourceDialog();
            Bundle args = new Bundle();
            args.putString(TextResourceDialog.TEXT_DIALOG_TITLE,
                    getResources().getString(R.string.label_release_notes));
            args.putInt(TextResourceDialog.TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
            rDdialog.setArguments(args);
            rDdialog.show(getSupportFragmentManager(), "release_notes");
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

                    if ("RUNNING".equals(state) && mAutoStartFromIntent) {
                        setResult(RESULT_OK);
                        finish();
                    }
                }
                break;
            default:
                super.handleMessage(msg);
            }
        }
    };

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
        return (canStart() && Util.isConnected(this)) || canStop();
    }

    public boolean shouldBeOn() {
        String action = getIntent().getAction();
        return (canStop()) ||
                (action != null && action.equals("net.i2p.android.router.START_I2P"));
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
}
