package net.i2p.android.router.service;

import android.os.Binder;

public class RouterBinder extends Binder {

    private final RouterService _routerService;

    public RouterBinder(RouterService service) {
         super();
         _routerService = service;
    }

    public RouterService getService() {
        return _routerService;
    }
}
