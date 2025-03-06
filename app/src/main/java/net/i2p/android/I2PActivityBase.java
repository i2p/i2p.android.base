package net.i2p.android;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import net.i2p.android.router.service.RouterBinder;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.util.Util;
import net.i2p.android.util.LocaleManager;

public abstract class I2PActivityBase extends AppCompatActivity {
    /**
     * Router variables
     */
    protected boolean _isBound;
    protected boolean _triedBind;
    protected ServiceConnection _connection;
    protected RouterService _routerService;
    private SharedPreferences _sharedPrefs;

    private static final String SHARED_PREFS = "net.i2p.android.router";
    protected static final String PREF_AUTO_START = "autoStart";
    /**
     * true leads to a poor install experience, very slow to paint the screen
     */
    protected static final boolean DEFAULT_AUTO_START = false;

    private final LocaleManager localeManager = new LocaleManager();

    /**
     * Called when the activity is first created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Util.d(this + " onCreate called");
        localeManager.onCreate(this);
        super.onCreate(savedInstanceState);
        _sharedPrefs = getSharedPreferences(SHARED_PREFS, 0);
    }

    @Override
    public void onRestart() {
        Util.d(this + " onRestart called");
        super.onRestart();
    }

    @Override
    public void onStart() {
        Util.d(this + " onStart called");
        super.onStart();
        if (_sharedPrefs.getBoolean(PREF_AUTO_START, DEFAULT_AUTO_START))
            startRouter();
        else
            bindRouter(false);
    }

    /**
     * @param def default
     */
    public boolean getPref(String pref, boolean def) {
        return _sharedPrefs.getBoolean(pref, def);
    }

    /**
     * @param def default
     */
    public String getPref(String pref, String def) {
        return _sharedPrefs.getString(pref, def);
    }

    /**
     * @return success
     */
    public boolean setPref(String pref, boolean val) {
        SharedPreferences.Editor edit = _sharedPrefs.edit();
        edit.putBoolean(pref, val);
        return edit.commit();
    }

    /**
     * @return success
     */
    public boolean setPref(String pref, String val) {
        SharedPreferences.Editor edit = _sharedPrefs.edit();
        edit.putString(pref, val);
        return edit.commit();
    }

    @Override
    public void onResume() {
        Util.d(this + " onResume called");
        super.onResume();
        localeManager.onResume(this);
    }

    public void notifyLocaleChanged() {
        localeManager.onResume(this);
    }

    @Override
    public void onPause() {
        Util.d(this + " onPause called");
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Util.d(this + " onSaveInstanceState called");
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        Util.d(this + " onStop called");
        unbindRouter();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Util.d(this + " onDestroy called");
        super.onDestroy();
    }

    ////// Service stuff

    /**
     * Start the service and bind to it
     */
    protected boolean startRouter() {
        Intent intent = new Intent();
        intent.setClassName(this, "net.i2p.android.router.service.RouterService");
        Util.d(this + " calling startService");
        ComponentName name = startService(intent);
        if (name == null)
            Util.d(this + " XXXXXXXXXXXXXXXXXXXX got null from startService!");
        Util.d(this + " got from startService: " + name);
        boolean success = bindRouter(true);
        if (!success)
            Util.d(this + " Bind router failed");
        return success;
    }

    /**
     * Bind only
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

    /**
     * callback from ServiceConnection, override as necessary
     */
    protected void onRouterBind(RouterService svc) {
    }

    /**
     * callback from ServiceConnection, override as necessary
     */
    protected void onRouterUnbind() {
    }
}
