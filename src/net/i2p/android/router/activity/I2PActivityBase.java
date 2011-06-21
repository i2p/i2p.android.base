package net.i2p.android.router.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import net.i2p.android.router.R;
import net.i2p.android.router.binder.RouterBinder;
import net.i2p.android.router.service.RouterService;
import net.i2p.router.CommSystemFacade;
import net.i2p.router.NetworkDatabaseFacade;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.TunnelManagerFacade;
import net.i2p.router.peermanager.ProfileOrganizer;
import net.i2p.router.transport.FIFOBandwidthLimiter;
import net.i2p.stat.StatManager;

public abstract class I2PActivityBase extends Activity {
    protected String _myDir;
    protected boolean _isBound;
    protected ServiceConnection _connection;
    protected RouterService _routerService;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        System.err.println(this + " onCreate called");
        super.onCreate(savedInstanceState);
        _myDir = getFilesDir().getAbsolutePath();
    }

    @Override
    public void onRestart()
    {
        System.err.println(this + " onRestart called");
        super.onRestart();
    }

    @Override
    public void onStart()
    {
        System.err.println(this + " onStart called");
        super.onStart();
        startRouter();
    }

    protected boolean startRouter() {
        Intent intent = new Intent();
        intent.setClassName(this, "net.i2p.android.router.service.RouterService");
        System.err.println(this + " calling startService");
        ComponentName name = startService(intent);
        if (name == null)
            System.err.println(this + " XXXXXXXXXXXXXXXXXXXX got from startService: " + name);
        System.err.println(this + " got from startService: " + name);
        boolean success = bindRouter();
        if (!success)
            System.err.println(this + " Bind router failed");
        return success;
    }

    @Override
    public void onResume()
    {
        System.err.println(this + " onResume called");
        super.onResume();
    }

    @Override
    public void onPause()
    {
        System.err.println(this + " onPause called");
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        System.err.println(this + " onSaveInstanceState called");
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop()
    {
        System.err.println(this + " onStop called");
        unbindRouter();
        super.onStop();
    }

    @Override
    public void onDestroy()
    {
        System.err.println(this + " onDestroy called");
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu1, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // add/hide items here
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_settings:
            Intent intent = new Intent(I2PActivityBase.this, SettingsActivity.class);
            startActivityForResult(intent, 0);
            return true;
        case R.id.menu_start:
        case R.id.menu_stop:
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    ////// Service stuff

    protected boolean bindRouter() {
        Intent intent = new Intent();
        intent.setClassName(this, "net.i2p.android.router.service.RouterService");
        System.err.println(this + " calling bindService");
        _connection = new RouterConnection();
        boolean success = bindService(intent, _connection, BIND_AUTO_CREATE);
        System.err.println(this + " got from bindService: " + success);
        return success;
    }

    protected void unbindRouter() {
        if (_isBound) {
            unbindService(_connection);
            _isBound = false;
        }
    }

    protected class RouterConnection implements ServiceConnection {

        public void onServiceConnected(ComponentName name, IBinder service) {
            System.err.println(this + " connected to router service");
            RouterBinder binder = (RouterBinder) service;
            _routerService = binder.getService();
            _isBound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            System.err.println(this + " disconnected from router service!!!!!!!");
            _isBound = false;
        }
    }

    ////// Router stuff

    protected RouterContext getRouterContext() {
        if (_routerService == null || !_isBound)
            return null;
        return _routerService.getRouterContext();
    }

    protected Router getRouter() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.router();
    }

    protected NetworkDatabaseFacade getNetDb() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.netDb();
    }

    protected ProfileOrganizer getProfileOrganizer() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.profileOrganizer();
    }

    protected TunnelManagerFacade getTunnelManager() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.tunnelManager();
    }

    protected CommSystemFacade getCommSystem() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.commSystem();
    }

    protected FIFOBandwidthLimiter getBandwidthLimiter() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.bandwidthLimiter();
    }

    protected StatManager getStatManager() {
        RouterContext ctx = getRouterContext();
        if (ctx == null)
            return null;
        return ctx.statManager();
    }
}
