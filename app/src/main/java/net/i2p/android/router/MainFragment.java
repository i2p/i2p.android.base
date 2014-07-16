package net.i2p.android.router;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.i2p.android.router.dialog.FirstStartDialog;
import net.i2p.android.router.dialog.VersionDialog;
import net.i2p.android.router.service.State;
import net.i2p.android.router.util.LongToggleButton;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataHelper;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.data.LeaseSet;
import net.i2p.router.RouterContext;
import net.i2p.router.TunnelPoolSettings;
import net.i2p.util.Translate;

import java.text.Collator;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainFragment extends I2PFragmentBase {

    private Handler _handler;
    private Runnable _updater;
    private Runnable _oneShotUpdate;
    private String _savedStatus;
    private boolean _keep = true;
    private boolean _startPressed = false;
    private static final String PREF_FIRST_START = "app.router.firstStart";
    private static final String PREF_SHOW_STATS = "i2pandroid.main.showStats";
    protected static final String PROP_NEW_INSTALL = "i2p.newInstall";
    protected static final String PROP_NEW_VERSION = "i2p.newVersion";
    RouterControlListener mCallback;

    // Container Activity must implement this interface
    public interface RouterControlListener {
        public boolean shouldShowOnOff();
        public boolean shouldBeOn();
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

        _keep = true;

        _handler = new Handler();
        _updater = new Updater();
        _oneShotUpdate = new OneShotUpdate();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        final ImageView lightImage = (ImageView) v.findViewById(R.id.main_lights);
        lightImage.setImageResource(R.drawable.routerlogo_0);

        LongToggleButton b = (LongToggleButton) v.findViewById(R.id.router_onoff_button);
        b.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View view) {
                boolean on = ((ToggleButton) view).isChecked();
                if (on) {
                    _startPressed = true;
                    mCallback.onStartRouterClicked();
                    updateOneShot();
                    checkFirstStart();
                } else {
                    if(mCallback.onStopRouterClicked()) {
                        updateOneShot();
                    }
                }
                return true;
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
        boolean showOnOff = mCallback.shouldShowOnOff();
        ToggleButton b = (ToggleButton) getActivity().findViewById(R.id.router_onoff_button);
        b.setVisibility(showOnOff ? View.VISIBLE : View.INVISIBLE);

        boolean isOn = mCallback.shouldBeOn();
        b.setChecked(isOn);

        if (showOnOff && !isOn) {
            // Sometimes the final state message from the RouterService
            // is not received. Ensure that the state image is correct.
            // TODO: Fix the race between RouterService shutdown and
            // IRouterState unbinding.
            updateState(State.INIT);
        }
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

    public void updateState(int newState) {
        final ImageView lightImage = (ImageView) getView().findViewById(R.id.main_lights);
        if (newState == State.INIT ||
                newState == State.STOPPED ||
                newState == State.MANUAL_STOPPED ||
                newState == State.MANUAL_QUITTED ||
                newState == State.NETWORK_STOPPED) {
            lightImage.setImageResource(R.drawable.routerlogo_0);
        } else if (newState == State.STARTING ||
                newState == State.STOPPING ||
                newState == State.MANUAL_STOPPING ||
                newState == State.MANUAL_QUITTING ||
                newState == State.NETWORK_STOPPING) {
            lightImage.setImageResource(R.drawable.routerlogo_1);
        } else if (newState == State.RUNNING) {
            lightImage.setImageResource(R.drawable.routerlogo_2);
        } else if (newState == State.ACTIVE) {
            lightImage.setImageResource(R.drawable.routerlogo_3);
        } else if (newState == State.WAITING) {
            lightImage.setImageResource(R.drawable.routerlogo_4);
        } // Ignore unknown states.
    }

    private void updateStatus() {
        RouterContext ctx = getRouterContext();
        ScrollView sv = (ScrollView) getActivity().findViewById(R.id.main_scrollview);
        LinearLayout vStatus = (LinearLayout) getActivity().findViewById(R.id.main_status);
        TextView vStatusText = (TextView) getActivity().findViewById(R.id.main_status_text);

        if(!Util.isConnected(getActivity())) {
            // Manually set state, RouterService won't be running
            updateState(State.WAITING);
            vStatusText.setText("No Internet connection is available");
            vStatus.setVisibility(View.VISIBLE);
            sv.setVisibility(View.VISIBLE);
        } else if(ctx != null) {
            if(_startPressed) {
                _startPressed = false;
            }

            // Load running tunnels
            loadDestinations(ctx);

            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(PREF_SHOW_STATS, false)) {
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

                String netstatus;
                if (reach == net.i2p.router.CommSystemFacade.STATUS_DIFFERENT) {
                    netstatus = "Symmetric NAT";
                } else if (reach == net.i2p.router.CommSystemFacade.STATUS_HOSED) {
                    netstatus = "Port Failure";
                } else if (reach == net.i2p.router.CommSystemFacade.STATUS_OK) {
                    netstatus = "OK";
                } else if (reach == net.i2p.router.CommSystemFacade.STATUS_REJECT_UNSOLICITED) {
                    netstatus = "Firewalled";
                } else {
                    netstatus = "Unknown";
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
                        "Network: " + netstatus
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

                _savedStatus = status + participate + details;
                vStatusText.setText(_savedStatus);
                vStatus.setVisibility(View.VISIBLE);
            } else {
                vStatus.setVisibility(View.INVISIBLE);
            }
            sv.setVisibility(View.VISIBLE);
        } else {
            // network but no router context
            vStatusText.setText("Not running");
            sv.setVisibility(View.INVISIBLE);
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

    /**
     * Based on net.i2p.router.web.SummaryHelper.getDestinations()
     * @param ctx The RouterContext
     */
    private void loadDestinations(RouterContext ctx) {
        TableLayout dests = (TableLayout) getView().findViewById(R.id.main_tunnels);
        dests.removeAllViews();

        List<Destination> clients = new ArrayList<Destination>(ctx.clientManager().listClients());
        if (!clients.isEmpty()) {
            Collections.sort(clients, new AlphaComparator(ctx));
            for (Destination client : clients) {
                String name = getName(ctx, client);
                Hash h = client.calculateHash();
                TableRow dest = new TableRow(getActivity());
                //dest.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

                // Client or server
                ImageView type = new ImageView(getActivity());
                if (ctx.clientManager().shouldPublishLeaseSet(h))
                    type.setImageDrawable(getActivity().getResources()
                            .getDrawable(R.drawable.server));
                else
                    type.setImageDrawable(getActivity().getResources()
                            .getDrawable(R.drawable.client));
                dest.addView(type);

                // Name
                TextView destName = new TextView(getActivity());
                destName.setText(name);
                //destName.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
                dest.addView(destName);

                // Status
                LeaseSet ls = ctx.netDb().lookupLeaseSetLocally(h);
                if (ls != null && ctx.tunnelManager().getOutboundClientTunnelCount(h) > 0) {
                    long timeToExpire = ls.getEarliestLeaseDate() - ctx.clock().now();
                    if (timeToExpire < 0) {
                        // red or yellow light
                        type.setBackgroundColor(Color.TRANSPARENT);
                    } else {
                        // green light
                        type.setBackgroundColor(Color.GREEN);
                    }
                } else {
                    // yellow light
                    type.setBackgroundColor(Color.TRANSPARENT);
                }

                dests.addView(dest);
            }
        } else {
            TableRow empty = new TableRow(getActivity());
            TextView emptyText = new TextView(getActivity());
            emptyText.setText("No client tunnels are running yet.");
            empty.addView(emptyText);
            dests.addView(empty);
        }
    }

    /** compare translated nicknames - put "shared clients" first in the sort */
    private class AlphaComparator implements Comparator<Destination> {
        private String xsc;
        private RouterContext _ctx;

        public AlphaComparator(RouterContext ctx) {
            _ctx = ctx;
            xsc = _(ctx, "shared clients");
        }

        public int compare(Destination lhs, Destination rhs) {
            String lname = getName(_ctx, lhs);
            String rname = getName(_ctx, rhs);
            if (lname.equals(xsc))
                return -1;
            if (rname.equals(xsc))
                return 1;
            return Collator.getInstance().compare(lname, rname);
        }
    }

    /** translate here so collation works above */
    private String getName(RouterContext ctx, Destination d) {
        TunnelPoolSettings in = ctx.tunnelManager().getInboundSettings(d.calculateHash());
        String name = (in != null ? in.getDestinationNickname() : null);
        if (name == null) {
            TunnelPoolSettings out = ctx.tunnelManager().getOutboundSettings(d.calculateHash());
            name = (out != null ? out.getDestinationNickname() : null);
            if (name == null)
                name = d.calculateHash().toBase64().substring(0,6);
            else
                name = _(ctx, name);
        } else {
            name = _(ctx, name);
        }
        return name;
    }

    private String _(RouterContext ctx, String s) {
        return Translate.getString(s, ctx, "net.i2p.router.web.messages");
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

    private void checkFirstStart() {
        I2PActivityBase ab = (I2PActivityBase) getActivity();
        boolean firstStart = ab.getPref(PREF_FIRST_START, true);
        if (firstStart) {
            FirstStartDialog dialog = new FirstStartDialog();
            dialog.show(getActivity().getSupportFragmentManager(), "firststart");
            ab.setPref(PREF_FIRST_START, false);
        }
    }
}
