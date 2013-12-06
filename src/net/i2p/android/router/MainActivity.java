package net.i2p.android.router;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import net.i2p.android.router.R;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
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
