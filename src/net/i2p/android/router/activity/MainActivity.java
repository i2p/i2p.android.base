package net.i2p.android.router.activity;

import java.io.File;

import android.os.Bundle;
import net.i2p.android.router.R;
import net.i2p.android.router.fragment.MainFragment;
import net.i2p.android.router.fragment.VersionDialog;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.util.Util;

public class MainActivity extends I2PActivityBase implements
        MainFragment.RouterControlListener,
        VersionDialog.VersionDialogListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start with the home view
        if (savedInstanceState == null) {
            MainFragment mainFragment = new MainFragment();
            mainFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment, mainFragment).commit();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        Util.i("Initializing...");
        InitActivities init = new InitActivities(this);
        init.debugStuff();
        init.initialize();
        super.onPostCreate(savedInstanceState);
    }

    // MainFragment.RouterControlListener

    public boolean shouldShowStart() {
        RouterService svc = _routerService;
        return ((svc == null) || (!_isBound) || svc.canManualStart())
                && Util.isConnected(this);
    }

    public boolean shouldShowStop() {
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
