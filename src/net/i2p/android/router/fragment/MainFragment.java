package net.i2p.android.router.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.text.DecimalFormat;
import net.i2p.android.router.R;
import net.i2p.android.router.activity.I2PActivityBase;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataHelper;
import net.i2p.router.RouterContext;

public class MainFragment extends I2PFragmentBase {

    private Handler _handler;
    private Runnable _updater;
    private Runnable _oneShotUpdate;
    private String _savedStatus;
    private String _ourVersion;
    private boolean _keep = true;
    private boolean _startPressed = false;
    protected static final String PROP_NEW_INSTALL = "i2p.newInstall";
    protected static final String PROP_NEW_VERSION = "i2p.newVersion";
    RouterControlListener mCallback;

    // Container Activity must implement this interface
    public interface RouterControlListener {
        public boolean shouldShowStart();
        public boolean shouldShowStop();
        public void onStartRouterClicked();
        public boolean onStopRouterClicked();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (RouterControlListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement RouterControlListener");
        }

    }

    /**
     * Called when the fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Init stuff here so settings work.
        if(savedInstanceState != null) {
            String saved = savedInstanceState.getString("status");
            if(saved != null) {
                _savedStatus = saved;
            }
        }
        _ourVersion = Util.getOurVersion(getActivity());

        _keep = true;

        _handler = new Handler();
        _updater = new Updater();
        _oneShotUpdate = new OneShotUpdate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        Button b = (Button) v.findViewById(R.id.news_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                getActivity().getSupportFragmentManager()
                             .beginTransaction()
                             .replace(R.id.main_fragment, new NewsFragment())
                             .addToBackStack(null)
                             .commit();
            }
        });

        b = (Button) v.findViewById(R.id.website_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                //intent.setData((new Uri.Builder()).scheme("http").authority("www.i2p2.de").path("/").build());
                WebFragment f = new WebFragment();
                Bundle args = new Bundle();
                args.putString(WebFragment.HTML_URI, "http://www.i2p2.de/");
                f.setArguments(args);
                getActivity().getSupportFragmentManager()
                             .beginTransaction()
                             .replace(R.id.main_fragment, f)
                             .addToBackStack(null)
                             .commit();
            }
        });

        b = (Button) v.findViewById(R.id.faq_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                //intent.setData((new Uri.Builder()).scheme("http").authority("www.i2p2.de").path("/faq").build());
                WebFragment f = new WebFragment();
                Bundle args = new Bundle();
                args.putString(WebFragment.HTML_URI, "http://www.i2p2.de/faq");
                f.setArguments(args);
                getActivity().getSupportFragmentManager()
                             .beginTransaction()
                             .replace(R.id.main_fragment, f)
                             .addToBackStack(null)
                             .commit();
            }
        });

        b = (Button) v.findViewById(R.id.welcome_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                WebFragment f = new WebFragment();
                Bundle args = new Bundle();
                args.putInt(WebFragment.HTML_RESOURCE_ID, R.raw.welcome_html);
                f.setArguments(args);
                getActivity().getSupportFragmentManager()
                             .beginTransaction()
                             .replace(R.id.main_fragment, f)
                             .addToBackStack(null)
                             .commit();
            }
        });

        /*
         * hidden, unused b = (Button) v.findViewById(R.id.router_stop_button);
         * b.setOnClickListener(new View.OnClickListener() { public void
         * onClick(View view) { RouterService svc = _routerService; if (svc !=
         * null && _isBound) { setPref(PREF_AUTO_START, false);
         * svc.manualStop(); updateOneShot(); } } });
         */

        b = (Button) v.findViewById(R.id.router_start_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                _startPressed = true;
                mCallback.onStartRouterClicked();
                updateOneShot();
            }
        });

        b = (Button) v.findViewById(R.id.router_quit_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                if(mCallback.onStopRouterClicked()) {
                    updateOneShot();
                }
            }
        });

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        _handler.removeCallbacks(_updater);
        _handler.removeCallbacks(_oneShotUpdate);
        if(_savedStatus != null) {
            TextView tv = (TextView) getActivity().findViewById(R.id.main_status_text);
            tv.setText(_savedStatus);
        }
        checkDialog();
        _handler.postDelayed(_updater, 100);
    }

    @Override
    public void onStop() {
        super.onStop();
        _handler.removeCallbacks(_updater);
        _handler.removeCallbacks(_oneShotUpdate);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateOneShot();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(_savedStatus != null) {
            outState.putString("status", _savedStatus);
        }
        super.onSaveInstanceState(outState);
    }

    private void updateOneShot() {
        _handler.postDelayed(_oneShotUpdate, 100);
    }

    private class OneShotUpdate implements Runnable {

        public void run() {
            updateVisibility();
            updateStatus();
        }
    }

    private class Updater implements Runnable {

        private int counter;
        private final int delay = 1000;
        private final int toloop = delay / 500;
        public void run() {
            updateVisibility();
            if(counter++ % toloop == 0) {
                updateStatus();
            }
            //_handler.postDelayed(this, 2500);
            _handler.postDelayed(this, delay);
        }
    }

    private void updateVisibility() {
        boolean showStart = mCallback.shouldShowStart();
        Button start = (Button) getActivity().findViewById(R.id.router_start_button);
        start.setVisibility(showStart ? View.VISIBLE : View.INVISIBLE);

        boolean showStop = mCallback.shouldShowStop();
        // Old stop but leave in memory. Always hide for now.
        // Button stop = (Button) findViewById(R.id.router_stop_button);
        // stop.setVisibility( /* showStop ? View.VISIBLE : */ View.INVISIBLE);

        Button quit = (Button) getActivity().findViewById(R.id.router_quit_button);
        quit.setVisibility(showStop ? View.VISIBLE : View.INVISIBLE);
    }

    public boolean onBackPressed() {
        RouterContext ctx = getRouterContext();
        // RouterService svc = _routerService; Which is better to use?!
        _keep = Util.isConnected(getActivity()) && (ctx != null || _startPressed);
        Util.d("*********************************************************");
        Util.d("Back pressed, Keep? " + _keep);
        Util.d("*********************************************************");
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(!_keep) {
            Thread t = new Thread(new KillMe());
            t.start();
        }
    }

    private class KillMe implements Runnable {

        public void run() {
            Util.d("*********************************************************");
            Util.d("KillMe started!");
            Util.d("*********************************************************");
            try {
                Thread.sleep(500); // is 500ms long enough?
            } catch(InterruptedException ex) {
            }
            System.exit(0);
        }
    }

    private void updateStatus() {
        RouterContext ctx = getRouterContext();
        TextView tv = (TextView) getActivity().findViewById(R.id.main_status_text);

        if(!Util.isConnected(getActivity())) {
            tv.setText("Router version: " + _ourVersion + "\nNo Internet connection is available");
            tv.setVisibility(View.VISIBLE);
        } else if(ctx != null) {
            if(_startPressed) {
                _startPressed = false;
            }
            short reach = ctx.commSystem().getReachabilityStatus();
            int active = ctx.commSystem().countActivePeers();
            int known = Math.max(ctx.netDb().getKnownRouters() - 1, 0);
            int inEx = ctx.tunnelManager().getFreeTunnelCount();
            int outEx = ctx.tunnelManager().getOutboundTunnelCount();
            int inCl = ctx.tunnelManager().getInboundClientTunnelCount();
            int outCl = ctx.tunnelManager().getOutboundClientTunnelCount();
            int part = ctx.tunnelManager().getParticipatingCount();
            double dLag = ctx.statManager().getRate("jobQueue.jobLag").getRate(60000).getAverageValue();
            String jobLag = DataHelper.formatDuration((long) dLag);
            String msgDelay = DataHelper.formatDuration(ctx.throttle().getMessageDelay());
            String uptime = DataHelper.formatDuration(ctx.router().getUptime());

            String netstatus = "Unknown";
            if(reach == net.i2p.router.CommSystemFacade.STATUS_DIFFERENT) {
                netstatus = "Different";
            }
            if(reach == net.i2p.router.CommSystemFacade.STATUS_HOSED) {
                netstatus = "Hosed";
            }
            if(reach == net.i2p.router.CommSystemFacade.STATUS_OK) {
                netstatus = "OK";
            }
            if(reach == net.i2p.router.CommSystemFacade.STATUS_REJECT_UNSOLICITED) {
                netstatus = "Reject Unsolicited";
            }
            String tunnelStatus = ctx.throttle().getTunnelStatus();
            //ctx.commSystem().getReachabilityStatus();
            double inBW = ctx.bandwidthLimiter().getReceiveBps() / 1024;
            double outBW = ctx.bandwidthLimiter().getSendBps() / 1024;

            // control total width
            DecimalFormat fmt;
            if(inBW >= 1000 || outBW >= 1000) {
                fmt = new DecimalFormat("#0");
            } else if(inBW >= 100 || outBW >= 100) {
                fmt = new DecimalFormat("#0.0");
            } else {
                fmt = new DecimalFormat("#0.00");
            }

            double kBytesIn = ctx.bandwidthLimiter().getTotalAllocatedInboundBytes() / 1024;
            double kBytesOut = ctx.bandwidthLimiter().getTotalAllocatedOutboundBytes() / 1024;

            // control total width
            DecimalFormat kBfmt;
            if(kBytesIn >= 1000 || kBytesOut >= 1000) {
                kBfmt = new DecimalFormat("#0");
            } else if(kBytesIn >= 100 || kBytesOut >= 100) {
                kBfmt = new DecimalFormat("#0.0");
            } else {
                kBfmt = new DecimalFormat("#0.00");
            }

            String status =
                    "ROUTER STATUS"
                    + "\nNetwork: " + netstatus
                    + "\nPeers active/known: " + active + " / " + known
                    + "\nExploratory Tunnels in/out: " + inEx + " / " + outEx
                    + "\nClient Tunnels in/out: " + inCl + " / " + outCl;


            // Need to see if we have the participation option set to on.
            // I thought there was a router method for that? I guess not! WHY NOT?
            // It would be easier if we had a number to test status.
            String participate = "\nParticipation: " + tunnelStatus +" (" + part + ")";

            String details =
                    "\nBandwidth in/out: " + fmt.format(inBW) + " / " + fmt.format(outBW) + " KBps"
                    + "\nData usage in/out: " + kBfmt.format(kBytesIn) + " / " + kBfmt.format(kBytesOut) + " KB"
                    + "\nMemory: " + DataHelper.formatSize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                    + "B / " + DataHelper.formatSize(Runtime.getRuntime().maxMemory()) + 'B'
                    + "\nJob Lag: " + jobLag
                    + "\nMsg Delay: " + msgDelay
                    + "\nUptime: " + uptime;

            _savedStatus = "Router version: " + _ourVersion + "\n" + status + participate + details;
            tv.setText(_savedStatus);
            tv.setVisibility(View.VISIBLE);
        } else {
            // network but no router context
            tv.setText("Router version: " + _ourVersion + "\n");
            //tv.setVisibility(View.INVISIBLE);
            /**
             * **
             * RouterService svc = _routerService; String status = "connected? "
             * + Util.isConnected(this) + "\nMemory: " +
             * DataHelper.formatSize(Runtime.getRuntime().totalMemory() -
             * Runtime.getRuntime().freeMemory()) + "B / " +
             * DataHelper.formatSize(Runtime.getRuntime().maxMemory()) + 'B' +
             * "\nhave ctx? " + (ctx != null) + "\nhave svc? " + (svc != null) +
             * "\nis bound? " + _isBound + "\nsvc state: " + (svc == null ?
             * "null" : svc.getState()) + "\ncan start? " + (svc == null ?
             * "null" : svc.canManualStart()) + "\ncan stop? " + (svc == null ?
             * "null" : svc.canManualStop()); tv.setText(status);
             * tv.setVisibility(View.VISIBLE);
          ***
             */
        }
    }

    private void checkDialog() {
        VersionDialog dialog = new VersionDialog();
        String oldVersion = ((I2PActivityBase) getActivity()).getPref(PREF_INSTALLED_VERSION, "??");
        if(oldVersion.equals("??")) {
            Bundle args = new Bundle();
            args.putInt(VersionDialog.DIALOG_TYPE, VersionDialog.DIALOG_NEW_INSTALL);
            dialog.setArguments(args);
            dialog.show(getActivity().getSupportFragmentManager(), "newinstall");
        } else {
            String currentVersion = Util.getOurVersion(getActivity());
            if(!oldVersion.equals(currentVersion)) {
                Bundle args = new Bundle();
                args.putInt(VersionDialog.DIALOG_TYPE, VersionDialog.DIALOG_NEW_VERSION);
                dialog.setArguments(args);
                dialog.show(getActivity().getSupportFragmentManager(), "newversion");
            }
        }
    }
}
