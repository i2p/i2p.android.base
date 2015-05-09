package net.i2p.android.router.service;

import android.content.Context;

import net.i2p.BOB.BOB;
import net.i2p.I2PAppContext;
import net.i2p.addressbook.DaemonThread;
import net.i2p.android.apps.NewsFetcher;
import net.i2p.android.router.util.Notifications;
import net.i2p.android.router.util.Util;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.router.Job;
import net.i2p.router.JobImpl;
import net.i2p.router.RouterContext;
import net.i2p.util.I2PAppThread;

import java.io.IOException;

/**
 * Load the clients we want.
 *
 * We can't use LoadClientAppsJob (reading in clients.config) directly
 * because Class.forName() needs a PathClassLoader argument -
 * http://doandroids.com/blogs/2010/6/10/android-classloader-dynamic-loading-of/
 * ClassLoader cl = new PathClassLoader(_apkPath, ClassLoader.getSystemClassLoader());
 *
 * We can't extend LoadClientAppsJob to specify a class loader,
 * even if we use it only for Class.forName() and not for
 * setContextClassLoader(), because I2PTunnel still
 * can't find the existing static RouterContext due to the new class loader.
 *
 * Also, if we load them that way, we can't register a shutdown hook.
 *
 * So fire off the ones we want here, without a clients.config file and
 * without using Class.forName().
 *
 */
class LoadClientsJob extends JobImpl {

    private Context mCtx;
    private Notifications _notif;
    private DaemonThread _addressbook;
    private BOB _bob;

    /** this is the delay to load (and start) the clients. */
    private static final long LOAD_DELAY = 90*1000;


    public LoadClientsJob(Context ctx, RouterContext rCtx, Notifications notif) {
        super(rCtx);
        mCtx = ctx;
        _notif = notif;
        getTiming().setStartAfter(getContext().clock().now() + LOAD_DELAY);
    }

    public String getName() { return "Start Clients"; }

    public void runJob() {
        Job j = new RunI2PTunnel(getContext());
        getContext().jobQueue().addJob(j);

        Thread t = new I2PAppThread(new StatSummarizer(), "StatSummarizer", true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.start();

        NewsFetcher fetcher = NewsFetcher.getInstance(mCtx, getContext(), _notif);
        t = new I2PAppThread(fetcher, "NewsFetcher", true);
        t.start();

        _addressbook = new DaemonThread(new String[] {"addressbook"});
        _addressbook.setName("Addressbook");
        _addressbook.setDaemon(true);
        _addressbook.start();

        // add other clients here
        _bob = new BOB(I2PAppContext.getGlobalContext(), null, new String[0]);
        try {
            _bob.startup();
        } catch (IOException ioe) {}

        getContext().addShutdownTask(new ClientShutdownHook());
    }

    private class RunI2PTunnel extends JobImpl {

        public RunI2PTunnel(RouterContext ctx) {
            super(ctx);
        }

        public String getName() { return "Start I2P Tunnel"; }

        public void runJob() {
            Util.d("Starting i2ptunnel");
            TunnelControllerGroup tcg = TunnelControllerGroup.getInstance();
            try {
                tcg.startup();
                int sz = tcg.getControllers().size();
                Util.d("i2ptunnel started " + sz + " clients");
            } catch (IllegalArgumentException iae) {
                Util.e("i2ptunnel failed to start", iae);
            }

        }
    }

    private class ClientShutdownHook implements Runnable {
        public void run() {
            Util.d("client shutdown hook");
            // i2ptunnel registers its own hook
            // StatSummarizer registers its own hook
            // NewsFetcher registers its own hook
            if (_bob != null)
                _bob.shutdown(null);
            if (_addressbook != null)
                _addressbook.halt();
        }
    }
}
