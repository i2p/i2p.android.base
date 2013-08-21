package net.i2p.android.router.activity;

import android.os.Bundle;
import net.i2p.android.router.R;
import net.i2p.android.router.fragment.MainFragment;
import net.i2p.android.router.fragment.VersionDialog;
import net.i2p.android.router.util.Util;

public class MainActivity extends I2PActivityBase
                          implements VersionDialog.VersionDialogListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start with the home view
        if (savedInstanceState == null) {
            MainFragment mainFragment = new MainFragment();
            mainFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_content, mainFragment).commit();
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

    // VersionDialog.VersionDialogListener

    public void onFirstRun() {
        mDrawerLayout.openDrawer(mDrawerList);
    }
}
