package net.i2p.android.router.service;

import net.i2p.BOB.BOB;
import net.i2p.addressbook.DaemonThread;
import net.i2p.android.apps.NewsFetcher;
import net.i2p.android.router.util.Util;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.router.Job;
import net.i2p.router.JobImpl;
import net.i2p.router.RouterContext;
import net.i2p.util.I2PAppThread;

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
    
    private Thread _fetcherThread;
    private DaemonThread _addressbook;
    private Thread _BOB;

    /** this is the delay to load (and start) the clients. */
    private static final long LOAD_DELAY = 90*1000;


    public LoadClientsJob(RouterContext ctx) {
        super(ctx);
        getTiming().setStartAfter(getContext().clock().now() + LOAD_DELAY);
    }

    public String getName() { return "Start Clients"; };

    public void runJob() {
        Job j = new RunI2PTunnel(getContext());
        getContext().jobQueue().addJob(j);

        NewsFetcher fetcher = NewsFetcher.getInstance(getContext());
        _fetcherThread = new I2PAppThread(fetcher, "NewsFetcher", true);
        _fetcherThread.start();

        _addressbook = new DaemonThread(new String[] {"addressbook"});
        _addressbook.setName("Addressbook");
        _addressbook.setDaemon(true);
        _addressbook.start();

        // add other clients here
        Run_BOB bob = new Run_BOB();
        _BOB = new I2PAppThread(bob, "BOB", true);
        _BOB.start();

        getContext().addShutdownTask(new ClientShutdownHook());
    }

    private class Run_BOB implements Runnable {
        public void run() {
            Util.i("BOB starting...");
            BOB.main(null);
            Util.i("BOB Stopped.");
            _BOB = null;
        }
    }

    private class RunI2PTunnel extends JobImpl {

        public RunI2PTunnel(RouterContext ctx) {
            super(ctx);
        }

        public String getName() { return "Start I2P Tunnel"; };

        public void runJob() {
            Util.i("Starting i2ptunnel");
            TunnelControllerGroup tcg = TunnelControllerGroup.getInstance();
            int sz = tcg.getControllers().size();
            Util.i("i2ptunnel started " + sz + " clients");

        }
    }

    private class ClientShutdownHook implements Runnable {
        public void run() {
            Util.i("client shutdown hook");
            // i2ptunnel registers its own hook
            if (_BOB != null)
                BOB.stop();
            if (_fetcherThread != null)
                _fetcherThread.interrupt();
            if (_addressbook != null)
                _addressbook.halt();
        }
    }
}
