package net.i2p.android.router.binder;

import android.os.Binder;

import net.i2p.android.router.service.RouterService;

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
