package net.i2p.android.router;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar.Tab;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import net.i2p.android.i2ptunnel.TunnelListActivity;
import net.i2p.android.router.R;
import net.i2p.android.router.addressbook.AddressbookActivity;
import net.i2p.android.router.log.LogActivity;
import net.i2p.android.router.netdb.NetDbActivity;
import net.i2p.android.router.service.RouterBinder;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.stats.PeersActivity;
import net.i2p.android.router.stats.RateGraphActivity;
import net.i2p.android.router.util.Util;
import net.i2p.android.router.web.WebActivity;
import net.i2p.android.router.web.WebFragment;
import net.i2p.router.RouterContext;

public abstract class I2PActivityBase extends ActionBarActivity implements
        I2PFragmentBase.RouterContextProvider {
    /**
     * Navigation drawer variables
     */
    protected DrawerLayout mDrawerLayout;
    protected ListView mDrawerList;
    protected ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mActivityTitles;

    /**
     * Router variables
     */
    protected String _myDir;
    protected boolean _isBound;
    protected boolean _triedBind;
    protected ServiceConnection _connection;
    protected RouterService _routerService;
    private SharedPreferences _sharedPrefs;

    private static final String SHARED_PREFS = "net.i2p.android.router";
    protected static final String PREF_AUTO_START = "autoStart";
    /** true leads to a poor install experience, very slow to paint the screen */
    protected static final boolean DEFAULT_AUTO_START = false;
    protected static final String PREF_NAV_DRAWER_OPENED = "navDrawerOpened";

    /**
     * Override this in subclasses that need a ViewPager, such as a
     * category view.
     * @return whether this Activity needs a ViewPager.
     */
    protected boolean useViewPager() {
        return false;
    }

    /**
     * Override this in subclasses that can use two panes, such as a
     * list/detail class.
     * @return whether this Activity can use a two-pane layout.
     */
    protected boolean canUseTwoPanes() {
        return false;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Util.d(this + " onCreate called");
        super.onCreate(savedInstanceState);
        _sharedPrefs = getSharedPreferences(SHARED_PREFS, 0);
        _myDir = getFilesDir().getAbsolutePath();

        // If the Activity wants to use a ViewPager, provide it.
        // If the Activity can make use of two panes (if available),
        // load the layout that will enable them. Otherwise, load the
        // layout that will only ever have a single pane.
        if (useViewPager())
            setContentView(R.layout.activity_navdrawer_viewpager);
        else if (canUseTwoPanes())
            setContentView(R.layout.activity_navdrawer);
        else
            setContentView(R.layout.activity_navdrawer_onepane);

        mTitle = mDrawerTitle = getTitle();
        mActivityTitles = getResources().getStringArray(R.array.navdrawer_activity_titles);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.drawer);

        // Set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, mActivityTitles));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // Enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            private boolean wasDragged = false;

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                // Don't mark as opened if the user closed by dragging
                // but uses the action bar icon to open
                wasDragged = false;
                getSupportActionBar().setTitle(mTitle);
                supportInvalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View view) {
                if (wasDragged && !getPref(PREF_NAV_DRAWER_OPENED, false))
                    setPref(PREF_NAV_DRAWER_OPENED, true);
                getSupportActionBar().setTitle(mDrawerTitle);
                supportInvalidateOptionsMenu();
            }

            /** Called when the drawer motion state changes. */
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_DRAGGING)
                    wasDragged = true;
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
            selectItem(pos);
        }
    }

    private void selectItem(int pos) {
        switch (pos) {
        case 1:
            Intent ab = new Intent(I2PActivityBase.this, AddressbookActivity.class);
            startActivity(ab);
            break;
        case 2:
            Intent itb = new Intent(I2PActivityBase.this, TunnelListActivity.class);
            startActivity(itb);
            break;
        case 3:
            Intent log = new Intent(I2PActivityBase.this, LogActivity.class);
            startActivity(log);
            break;
        case 4:
            Intent active = new Intent(I2PActivityBase.this, RateGraphActivity.class);
            startActivity(active);
            break;
        case 5:
            Intent peers = new Intent(I2PActivityBase.this, PeersActivity.class);
            startActivity(peers);
            break;
        case 6:
            Intent netdb = new Intent(I2PActivityBase.this, NetDbActivity.class);
            startActivity(netdb);
            break;
        case 7:
            Intent wp = new Intent(I2PActivityBase.this, WebActivity.class);
            wp.putExtra(WebFragment.HTML_RESOURCE_ID, R.raw.welcome_html);
            startActivity(wp);
            break;
        case 8:
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment, new NewsFragment())
                .addToBackStack(null)
                .commit();
            break;
        default:
            Intent main = new Intent(I2PActivityBase.this, MainActivity.class);
            startActivity(main);
            break;
        }
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void onRestart()
    {
        Util.d(this + " onRestart called");
        super.onRestart();
    }

    @Override
    public void onStart()
    {
        Util.d(this + " onStart called");
        super.onStart();
        if (_sharedPrefs.getBoolean(PREF_AUTO_START, DEFAULT_AUTO_START))
            startRouter();
        else
            bindRouter(false);
    }

    /** @param def default */
    public boolean getPref(String pref, boolean def) {
        return _sharedPrefs.getBoolean(pref, def);
    }

    /** @param def default */
    public String getPref(String pref, String def) {
        return _sharedPrefs.getString(pref, def);
    }

    /** @return success */
    public boolean setPref(String pref, boolean val) {
        SharedPreferences.Editor edit = _sharedPrefs.edit();
        edit.putBoolean(pref, val);
        return edit.commit();
    }

    /** @return success */
    public boolean setPref(String pref, String val) {
        SharedPreferences.Editor edit = _sharedPrefs.edit();
        edit.putString(pref, val);
        return edit.commit();
    }

    @Override
    public void onResume()
    {
        Util.d(this + " onResume called");
        super.onResume();
    }

    @Override
    public void onPause()
    {
        Util.d(this + " onPause called");
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        Util.d(this + " onSaveInstanceState called");
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop()
    {
        Util.d(this + " onStop called");
        unbindRouter();
        super.onStop();
    }

    @Override
    public void onDestroy()
    {
        Util.d(this + " onDestroy called");
        super.onDestroy();
    }

    /**
     * Called whenever we call invalidateOptionsMenu()
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        onDrawerChange(drawerOpen);

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Override in subclass with e.g.
     * menu.findItem(R.id.action_add_to_addressbook).setVisible(!drawerOpen);
     * @param drawerOpen true if the drawer is open
     */
    protected void onDrawerChange(boolean drawerOpen) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if(mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle action buttons and overflow
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggle
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public static class TabListener implements ActionBar.TabListener {
        protected Fragment mFragment;

        public TabListener(Fragment fragment) {
            mFragment = fragment;
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            ft.replace(R.id.main_fragment, mFragment);
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            ft.remove(mFragment);
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            // User selected the already selected tab.
        }
    }

    ////// Service stuff

    /**
     *  Start the service and bind to it
     */
    protected boolean startRouter() {
        Intent intent = new Intent();
        intent.setClassName(this, "net.i2p.android.router.service.RouterService");
        Util.d(this + " calling startService");
        ComponentName name = startService(intent);
        if (name == null)
            Util.d(this + " XXXXXXXXXXXXXXXXXXXX got from startService: " + name);
        Util.d(this + " got from startService: " + name);
        boolean success = bindRouter(true);
        if (!success)
            Util.d(this + " Bind router failed");
        return success;
    }

    /**
     *  Bind only
     */
    protected boolean bindRouter(boolean autoCreate) {
        Intent intent = new Intent(RouterBinder.class.getName());
        intent.setClassName(this, "net.i2p.android.router.service.RouterService");
        Util.d(this + " calling bindService");
        _connection = new RouterConnection();
        _triedBind = bindService(intent, _connection, autoCreate ? BIND_AUTO_CREATE : 0);
        Util.d(this + " bindService: auto create? " + autoCreate + " success? " + _triedBind);
        return _triedBind;
    }

    protected void unbindRouter() {
        Util.d(this + " unbindRouter called with _isBound:" + _isBound + " _connection:" + _connection + " _triedBind:" + _triedBind);
        if (_triedBind && _connection != null)
                unbindService(_connection);

        _triedBind = false;
        _connection = null;
        _routerService = null;
        _isBound = false;
    }

    /**
     * Class for interacting with the main interface of the RouterService.
     */
    protected class RouterConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName name, IBinder service) {
            Util.d(this + " connected to router service");
            RouterBinder binder = (RouterBinder) service;
            RouterService svc = binder.getService();
            _routerService = svc;
            _isBound = true;
            onRouterBind(svc);
        }

        public void onServiceDisconnected(ComponentName name) {
            Util.d(this + " disconnected from router service!!!!!!!");
            // save memory
            _routerService = null;
            _isBound = false;
            onRouterUnbind();
        }
    }

    /** callback from ServiceConnection, override as necessary */
    protected void onRouterBind(RouterService svc) {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.main_fragment);
        if (f instanceof I2PFragmentBase)
            ((I2PFragmentBase) f).onRouterBind();
        else if (f instanceof I2PFragmentBase.RouterContextUser)
            ((I2PFragmentBase.RouterContextUser) f).onRouterBind();

        if (canUseTwoPanes()) {
            f = getSupportFragmentManager().findFragmentById(R.id.detail_fragment);
            if (f instanceof I2PFragmentBase)
                ((I2PFragmentBase) f).onRouterBind();
            else if (f instanceof I2PFragmentBase.RouterContextUser)
                ((I2PFragmentBase.RouterContextUser) f).onRouterBind();
        }
    }

    /** callback from ServiceConnection, override as necessary */
    protected void onRouterUnbind() {}

    // I2PFragmentBase.RouterContextProvider

    public RouterContext getRouterContext() {
        RouterService svc = _routerService;
        if (svc == null || !_isBound)
            return null;
        return svc.getRouterContext();
    }
}
