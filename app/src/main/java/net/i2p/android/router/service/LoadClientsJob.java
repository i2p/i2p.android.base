package net.i2p.android.router.service;

import android.app.Notification;
import android.content.Context;

//import net.i2p.BOB.BOB;
import net.i2p.addressbook.DaemonThread;

import android.content.Intent;
import android.os.Looper;
import android.preference.PreferenceManager;
import net.i2p.android.apps.NewsFetcher;
import net.i2p.android.router.service.AndroidSAMSecureSession;
import net.i2p.android.router.util.Notifications;
import net.i2p.android.router.util.Util;
import net.i2p.i2ptunnel.TunnelControllerGroup;
import net.i2p.router.Job;
import net.i2p.router.JobImpl;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.util.I2PAppThread;
import net.i2p.sam.SAMBridge;
import net.i2p.sam.SAMSecureSessionInterface;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Load the clients we want.
 *
 * We can't use LoadClientAppsJob (reading in clients.config) directly
 * because Class.forName() needs a PathClassLoader argument -
 * http://doandroids.com/blogs/2010/6/10/android-classloader-dynamic-loading-of/
 * ClassLoader cl = new PathClassLoader(_apkPath,
 * ClassLoader.getSystemClassLoader());
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

    private final Context mCtx;
    private final RouterService _routerService;
    private final Notifications _notif;
    private DaemonThread _addressbook;
    public SAMBridge SAM_BRIDGE;
    private final StatusBar _statusBar;
    // private BOB _bob;

    /** this is the delay to load (and start) the clients. */
    private static final long LOAD_DELAY = 60 * 1000;

    public LoadClientsJob(Context ctx, RouterContext rCtx, Notifications notif, StatusBar status) {
        super(rCtx);
        mCtx = ctx;
        _routerService = null;
        _notif = notif;
        getTiming().setStartAfter(getContext().clock().now() + LOAD_DELAY);
        _statusBar = status;
    }

    public LoadClientsJob(Context ctx, RouterContext rCtx, RouterService rSvc, Notifications notif, StatusBar status) {
        super(rCtx);
        mCtx = ctx;
        _routerService = rSvc;
        _notif = notif;
        getTiming().setStartAfter(getContext().clock().now() + LOAD_DELAY);
        _statusBar = status;
    }

    public LoadClientsJob(Context ctx, RouterContext rCtx, Notifications notif) {
        super(rCtx);
        mCtx = ctx;
        _routerService = null;
        _notif = notif;
        getTiming().setStartAfter(getContext().clock().now() + LOAD_DELAY);
        _statusBar = null;
    }

    public String getName() {
        return "Start Clients";
    }

    public void runJob() {
        Job jtunnel = new RunI2PTunnel(getContext());
        getContext().jobQueue().addJob(jtunnel);

        Thread t = new I2PAppThread(new StatSummarizer(), "StatSummarizer", true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.start();

        // add other clients here
        // _bob = new BOB(I2PAppContext.getGlobalContext(), null, new String[0]);
        // try {
        // _bob.startup();
        // } catch (IOException ioe) {}
        boolean useSAM = PreferenceManager.getDefaultSharedPreferences(mCtx).getBoolean("i2pandroid.client.SAM", true);
        Util.i("SAM API " + useSAM);
        if (useSAM) {
            Job jsam = new RunI2PSAM(getContext());
            getContext().jobQueue().addJob(jsam);
            Util.i("SAM API started successfully" + useSAM);
        } else {
            Util.i("SAM API disabled, not starting " + useSAM);
        }
        getContext().addShutdownTask(new ClientShutdownHook());
    }

    private class RunI2PTunnel extends JobImpl {

        public RunI2PTunnel(RouterContext ctx) {
            super(ctx);
        }

        public String getName() {
            return "Start I2P Tunnel";
        }

        public void runJob() {
            if (!getContext().router().isRunning()) {
                if (getContext().router().isAlive()) {
                    requeue(1000);
                } else {
                    Util.e("Router stopped before i2ptunnel could start");
                }
                return;
            }
            Util.d("Starting i2ptunnel");
            TunnelControllerGroup tcg = TunnelControllerGroup.getInstance(getContext());
            try {
                tcg.startup();
                int sz = tcg.getControllers().size();
                Util.d("i2ptunnel started " + sz + " clients");

                // no use starting these until i2ptunnel starts
                RouterContext ctx = getContext();
                NewsFetcher fetcher = NewsFetcher.getInstance(mCtx, getContext(), _notif);
                ctx.routerAppManager().addAndStart(fetcher, new String[0]);

                _addressbook = new DaemonThread(new String[] { "addressbook" });
                _addressbook.setName("Addressbook");
                _addressbook.setDaemon(true);
                _addressbook.start();
            } catch (IllegalArgumentException iae) {
                Util.e("i2ptunnel failed to start", iae);
            }

        }
    }

    private class RunI2PSAM extends JobImpl {

        public RunI2PSAM(RouterContext ctx) {
            super(ctx);
        }

        public String getName() {
            return "Start SAM API";
        }

        public void runJob() {
            if (!getContext().router().isRunning()) {
                if (getContext().router().isAlive()) {
                    requeue(1000);
                } else {
                    Util.e("Router stopped before SAM API could start");
                }
                return;
            }
            Util.d("Starting SAM");
            try {
                Util.i("Starting the SAM API");
                Looper.prepare();
                //AndroidSAMSecureSession _androidSecureSession = new AndroidSAMSecureSession(mCtx, _routerService, _statusBar);
                AndroidSAMSecureSession _androidSecureSession = AndroidSAMSecureSession.create(mCtx, _routerService, _statusBar);
                SAMSecureSessionInterface _secureSession = _androidSecureSession;
                SAM_BRIDGE = new SAMBridge("127.0.0.1",
                 7656,
                 false,
                 SAM_PROPERTIES(),
                 "sam.keys",
                 new File("sam_config"),
                 _secureSession);
                SAM_BRIDGE.run();
            } catch (IOException e) {
                Util.e(e.toString());
                e.printStackTrace();
            }
        }

        public Properties SAM_PROPERTIES() throws IOException {
            Util.i("Getting the default properties");
            Properties sam_properties = new Properties();
            return sam_properties;
        }
    }

    private class ClientShutdownHook implements Runnable {
        public void run() {
            Util.d("client shutdown hook");
            // i2ptunnel registers its own hook
            // StatSummarizer registers its own hook
            // NewsFetcher registers its own hook
            // if (_bob != null)
            // _bob.shutdown(null);
            if (SAM_BRIDGE != null)
                SAM_BRIDGE.shutdown(null);
            if (_addressbook != null)
                _addressbook.halt();
        }
    }
}
