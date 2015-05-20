package net.i2p.android.router;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.i2p.android.I2PActivityBase;
import net.i2p.android.help.BrowserConfigActivity;
import net.i2p.android.router.dialog.FirstStartDialog;
import net.i2p.android.router.service.RouterService;
import net.i2p.android.router.service.State;
import net.i2p.android.router.util.Connectivity;
import net.i2p.android.router.util.LongToggleButton;
import net.i2p.android.router.util.Util;
import net.i2p.data.DataHelper;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import net.i2p.data.LeaseSet;
import net.i2p.router.RouterContext;
import net.i2p.router.TunnelPoolSettings;

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

    private ImageView mConsoleLights;
    private LongToggleButton mOnOffButton;
    private LinearLayout vGracefulButtons;
    private ScrollView mScrollView;
    private View vStatusContainer;
    private ImageView vNetStatusLevel;
    private TextView vNetStatusText;
    private View vNonNetStatus;
    private TextView vUptime;
    private TextView vActive;
    private TextView vKnown;
    private TableLayout vTunnels;
    private LinearLayout vAdvStatus;
    private TextView vAdvStatusText;

    private boolean _keep = true;
    private boolean _startPressed = false;
    private static final String PREF_CONFIGURE_BROWSER = "app.dialog.configureBrowser";
    private static final String PREF_FIRST_START = "app.router.firstStart";
    private static final String PREF_SHOW_STATS = "i2pandroid.main.showStats";
    protected static final String PROP_NEW_INSTALL = "i2p.newInstall";
    protected static final String PROP_NEW_VERSION = "i2p.newVersion";
    RouterControlListener mCallback;

    // Container Activity must implement this interface
    public interface RouterControlListener {
        boolean shouldShowOnOff();

        boolean shouldBeOn();

        void onStartRouterClicked();

        boolean onStopRouterClicked();

        /**
         * @since 0.9.19
         */
        boolean isGracefulShutdownInProgress();

        /**
         * @since 0.9.19
         */
        boolean onGracefulShutdownClicked();

        /**
         * @since 0.9.19
         */
        boolean onCancelGracefulShutdownClicked();
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
        if (savedInstanceState != null) {
            lastRouterState = savedInstanceState.getParcelable("lastState");
            String saved = savedInstanceState.getString("status");
            if (saved != null) {
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

        mConsoleLights = (ImageView) v.findViewById(R.id.console_lights);
        mOnOffButton = (LongToggleButton) v.findViewById(R.id.router_onoff_button);
        vGracefulButtons = (LinearLayout) v.findViewById(R.id.router_graceful_buttons);
        mScrollView = (ScrollView) v.findViewById(R.id.main_scrollview);
        vStatusContainer = v.findViewById(R.id.status_container);
        vNetStatusLevel = (ImageView) v.findViewById(R.id.console_net_status_level);
        vNetStatusText = (TextView) v.findViewById(R.id.console_net_status_text);
        vNonNetStatus = v.findViewById(R.id.console_non_net_status_container);
        vUptime = (TextView) v.findViewById(R.id.console_uptime);
        vActive = (TextView) v.findViewById(R.id.console_active);
        vKnown = (TextView) v.findViewById(R.id.console_known);
        vTunnels = (TableLayout) v.findViewById(R.id.main_tunnels);
        vAdvStatus = (LinearLayout) v.findViewById(R.id.console_advanced_status);
        vAdvStatusText = (TextView) v.findViewById(R.id.console_advanced_status_text);

        updateState(lastRouterState);

        mOnOffButton.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View view) {
                boolean on = ((ToggleButton) view).isChecked();
                if (on) {
                    _startPressed = true;
                    mCallback.onStartRouterClicked();
                    updateOneShot();
                    checkFirstStart();
                } else if (mCallback.onGracefulShutdownClicked())
                    updateOneShot();
                return true;
            }
        });

        Button gb = (Button) v.findViewById(R.id.button_shutdown_now);
        gb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mCallback.isGracefulShutdownInProgress())
                    if (mCallback.onStopRouterClicked())
                        updateOneShot();
                return true;
            }
        });
        gb = (Button) v.findViewById(R.id.button_cancel_graceful);
        gb.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mCallback.isGracefulShutdownInProgress())
                    if (mCallback.onCancelGracefulShutdownClicked())
                        updateOneShot();
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
        if (_savedStatus != null) {
            TextView tv = (TextView) getActivity().findViewById(R.id.console_advanced_status_text);
            tv.setText(_savedStatus);
        }
        checkDialog();
        _handler.postDelayed(_updater, 100);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());

        IntentFilter filter = new IntentFilter();
        filter.addAction(RouterService.LOCAL_BROADCAST_STATE_NOTIFICATION);
        filter.addAction(RouterService.LOCAL_BROADCAST_STATE_CHANGED);
        lbm.registerReceiver(onStateChange, filter);

        lbm.sendBroadcast(new Intent(RouterService.LOCAL_BROADCAST_REQUEST_STATE));
    }

    private State lastRouterState;
    private BroadcastReceiver onStateChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            State state = intent.getParcelableExtra(RouterService.LOCAL_BROADCAST_EXTRA_STATE);
            if (lastRouterState == null || lastRouterState != state) {
                updateState(state);
                // If we have stopped, clear the status info immediately
                if (Util.isStopped(state)) {
                    updateOneShot();
                }
                lastRouterState = state;
            }
        }
    };

    @Override
    public void onStop() {
        super.onStop();
        _handler.removeCallbacks(_updater);
        _handler.removeCallbacks(_oneShotUpdate);

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(onStateChange);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateOneShot();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (lastRouterState != null)
            outState.putParcelable("lastState", lastRouterState);
        if (_savedStatus != null)
            outState.putString("status", _savedStatus);
        super.onSaveInstanceState(outState);
    }

    private void updateOneShot() {
        _handler.postDelayed(_oneShotUpdate, 10);
    }

    private class OneShotUpdate implements Runnable {

        public void run() {
            updateVisibility();
            try {
                updateStatus();
            } catch (NullPointerException npe) {
                // RouterContext wasn't quite ready
                Util.w("Status was updated before RouterContext was ready", npe);
            }
        }
    }

    private class Updater implements Runnable {

        private int counter;
        private final int delay = 1000;
        private final int toloop = delay / 500;

        public void run() {
            updateVisibility();
            if (counter++ % toloop == 0) {
                try {
                    updateStatus();
                } catch (NullPointerException npe) {
                    // RouterContext wasn't quite ready
                    Util.w("Status was updated before RouterContext was ready", npe);
                }
            }
            //_handler.postDelayed(this, 2500);
            _handler.postDelayed(this, delay);
        }
    }

    private void updateVisibility() {
        boolean showOnOff = mCallback.shouldShowOnOff();
        mOnOffButton.setVisibility(showOnOff ? View.VISIBLE : View.GONE);

        boolean isOn = mCallback.shouldBeOn();
        mOnOffButton.setChecked(isOn);

        boolean isGraceful = mCallback.isGracefulShutdownInProgress();
        vGracefulButtons.setVisibility(isGraceful ? View.VISIBLE : View.GONE);
        if (isOn && isGraceful) {
            RouterContext ctx = getRouterContext();
            if (ctx != null) {
                TextView tv = (TextView) vGracefulButtons.findViewById(R.id.router_graceful_status);
                long ms = ctx.router().getShutdownTimeRemaining();
                if (ms > 1000) {
                    tv.setText(getActivity().getResources().getString(R.string.button_router_graceful,
                            DataHelper.formatDuration(ms)));
                } else {
                    tv.setText(getActivity().getString(R.string.notification_status_stopping));
                }
            }
        }
    }

    public boolean onBackPressed() {
        RouterContext ctx = getRouterContext();
        // RouterService svc = _routerService; Which is better to use?!
        _keep = Connectivity.isConnected(getActivity()) && (ctx != null || _startPressed);
        Util.d("*********************************************************");
        Util.d("Back pressed, Keep? " + _keep);
        Util.d("*********************************************************");
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!_keep) {
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
            } catch (InterruptedException ex) {
            }
            System.exit(0);
        }
    }

    public void updateState(State newState) {
        if (newState == State.INIT ||
                newState == State.STOPPED ||
                newState == State.MANUAL_STOPPED ||
                newState == State.MANUAL_QUITTED ||
                newState == State.NETWORK_STOPPED) {
            mConsoleLights.setImageResource(R.drawable.routerlogo_0);
        } else if (newState == State.STARTING ||
                newState == State.GRACEFUL_SHUTDOWN ||
                newState == State.STOPPING ||
                newState == State.MANUAL_STOPPING ||
                newState == State.MANUAL_QUITTING ||
                newState == State.NETWORK_STOPPING) {
            mConsoleLights.setImageResource(R.drawable.routerlogo_1);
        } else if (newState == State.RUNNING) {
            mConsoleLights.setImageResource(R.drawable.routerlogo_2);
        } else if (newState == State.ACTIVE) {
            mConsoleLights.setImageResource(R.drawable.routerlogo_3);
        } else if (newState == State.WAITING) {
            mConsoleLights.setImageResource(R.drawable.routerlogo_4);
        } // Ignore unknown states.
    }

    private void updateStatus() {
        RouterContext ctx = getRouterContext();

        if (!Connectivity.isConnected(getActivity())) {
            // Manually set state, RouterService won't be running
            updateState(State.WAITING);
            vNetStatusText.setText(R.string.no_internet);
            vStatusContainer.setVisibility(View.VISIBLE);
            vNonNetStatus.setVisibility(View.GONE);
        } else if (ctx != null) {
            if (_startPressed) {
                _startPressed = false;
            }

            Util.NetStatus netStatus = Util.getNetStatus(getActivity(), ctx);
            switch (netStatus.level) {
                case ERROR:
                    vNetStatusLevel.setImageDrawable(getResources().getDrawable(R.drawable.ic_error_red_24dp));
                    vNetStatusLevel.setVisibility(View.VISIBLE);
                    break;
                case WARN:
                    vNetStatusLevel.setImageDrawable(getResources().getDrawable(R.drawable.ic_warning_amber_24dp));
                    vNetStatusLevel.setVisibility(View.VISIBLE);
                    break;
                case INFO:
                default:
                    vNetStatusLevel.setVisibility(View.GONE);
            }
            vNetStatusText.setText(getString(R.string.settings_label_network) + ": " + netStatus.status);

            String uptime = DataHelper.formatDuration(ctx.router().getUptime());
            int active = ctx.commSystem().countActivePeers();
            int known = Math.max(ctx.netDb().getKnownRouters() - 1, 0);
            vUptime.setText("" + uptime);
            vActive.setText("" + active);
            vKnown.setText("" + known);

            // Load running tunnels
            loadDestinations(ctx);

            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(PREF_SHOW_STATS, false)) {
                int inEx = ctx.tunnelManager().getFreeTunnelCount();
                int outEx = ctx.tunnelManager().getOutboundTunnelCount();
                int inCl = ctx.tunnelManager().getInboundClientTunnelCount();
                int outCl = ctx.tunnelManager().getOutboundClientTunnelCount();
                int part = ctx.tunnelManager().getParticipatingCount();
                double dLag = ctx.statManager().getRate("jobQueue.jobLag").getRate(60000).getAverageValue();
                String jobLag = DataHelper.formatDuration((long) dLag);
                String msgDelay = DataHelper.formatDuration(ctx.throttle().getMessageDelay());

                String tunnelStatus = ctx.throttle().getTunnelStatus();
                //ctx.commSystem().getReachabilityStatus();

                String status =
                        "Exploratory Tunnels in/out: " + inEx + " / " + outEx
                                + "\nClient Tunnels in/out: " + inCl + " / " + outCl;


                // Need to see if we have the participation option set to on.
                // I thought there was a router method for that? I guess not! WHY NOT?
                // It would be easier if we had a number to test status.
                String participate = "\nParticipation: " + tunnelStatus + " (" + part + ")";

                String details =
                        "\nMemory: " + DataHelper.formatSize(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                                + "B / " + DataHelper.formatSize(Runtime.getRuntime().maxMemory()) + 'B'
                                + "\nJob Lag: " + jobLag
                                + "\nMsg Delay: " + msgDelay;

                _savedStatus = status + participate + details;
                vAdvStatusText.setText(_savedStatus);
                vAdvStatus.setVisibility(View.VISIBLE);
            } else {
                vAdvStatus.setVisibility(View.GONE);
            }
            vStatusContainer.setVisibility(View.VISIBLE);
            vNonNetStatus.setVisibility(View.VISIBLE);

            // Usage stats in bottom toolbar

            double inBW = ctx.bandwidthLimiter().getReceiveBps() / 1024;
            double outBW = ctx.bandwidthLimiter().getSendBps() / 1024;

            // control total width
            DecimalFormat fmt;
            if (inBW >= 1000 || outBW >= 1000) {
                fmt = new DecimalFormat("#0");
            } else if (inBW >= 100 || outBW >= 100) {
                fmt = new DecimalFormat("#0.0");
            } else {
                fmt = new DecimalFormat("#0.00");
            }

            double kBytesIn = ctx.bandwidthLimiter().getTotalAllocatedInboundBytes() / 1024;
            double kBytesOut = ctx.bandwidthLimiter().getTotalAllocatedOutboundBytes() / 1024;

            // control total width
            DecimalFormat kBfmt;
            if (kBytesIn >= 1000 || kBytesOut >= 1000) {
                kBfmt = new DecimalFormat("#0");
            } else if (kBytesIn >= 100 || kBytesOut >= 100) {
                kBfmt = new DecimalFormat("#0.0");
            } else {
                kBfmt = new DecimalFormat("#0.00");
            }

            ((TextView) getActivity().findViewById(R.id.console_download_stats)).setText(
                    fmt.format(inBW) + "KBps / " + kBfmt.format(kBytesIn) + "KB");
            ((TextView) getActivity().findViewById(R.id.console_upload_stats)).setText(
                    fmt.format(outBW) + "KBps / " + kBfmt.format(kBytesOut) + "KB");

            getActivity().findViewById(R.id.console_usage_stats).setVisibility(View.VISIBLE);
        } else {
            // network but no router context
            vStatusContainer.setVisibility(View.GONE);
            getActivity().findViewById(R.id.console_usage_stats).setVisibility(View.INVISIBLE);
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
     *
     * @param ctx The RouterContext
     */
    private void loadDestinations(RouterContext ctx) {
        vTunnels.removeAllViews();

        List<Destination> clients = null;
        if (ctx.clientManager() != null)
            clients = new ArrayList<Destination>(ctx.clientManager().listClients());

        if (clients != null && !clients.isEmpty()) {
            Collections.sort(clients, new AlphaComparator(ctx));
            for (Destination client : clients) {
                String name = getName(ctx, client);
                Hash h = client.calculateHash();
                TableRow dest = new TableRow(getActivity());
                dest.setPadding(16, 4, 0, 4);

                // Client or server
                TextView type = new TextView(getActivity());
                type.setTextColor(getResources().getColor(android.R.color.primary_text_light));
                type.setTypeface(Typeface.DEFAULT_BOLD);
                type.setGravity(Gravity.CENTER);
                if (ctx.clientManager().shouldPublishLeaseSet(h))
                    type.setText(R.string.char_server_tunnel);
                else
                    type.setText(R.string.char_client_tunnel);
                dest.addView(type);

                // Name
                TextView destName = new TextView(getActivity());
                destName.setPadding(16, 0, 0, 0);
                destName.setGravity(Gravity.CENTER_VERTICAL);
                destName.setText(name);
                dest.addView(destName);

                // Status
                LeaseSet ls = ctx.netDb().lookupLeaseSetLocally(h);
                if (ls != null && ctx.tunnelManager().getOutboundClientTunnelCount(h) > 0) {
                    long timeToExpire = ls.getEarliestLeaseDate() - ctx.clock().now();
                    if (timeToExpire < 0) {
                        // red or yellow light
                        type.setBackgroundResource(R.drawable.tunnel_yellow);
                    } else {
                        // green light
                        type.setBackgroundResource(R.drawable.tunnel_green);
                    }
                } else {
                    // yellow light
                    type.setBackgroundResource(R.drawable.tunnel_yellow);
                }

                vTunnels.addView(dest);
            }
        } else {
            TableRow empty = new TableRow(getActivity());
            TextView emptyText = new TextView(getActivity());
            emptyText.setText(R.string.no_tunnels_running);
            empty.addView(emptyText);
            vTunnels.addView(empty);
        }
    }

    private static final String SHARED_CLIENTS = "shared clients";

    /**
     * compare translated nicknames - put "shared clients" first in the sort
     */
    private class AlphaComparator implements Comparator<Destination> {
        private String xsc;
        private RouterContext _ctx;

        public AlphaComparator(RouterContext ctx) {
            _ctx = ctx;
            xsc = _(ctx, SHARED_CLIENTS);
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

    /**
     * translate here so collation works above
     */
    private String getName(RouterContext ctx, Destination d) {
        TunnelPoolSettings in = ctx.tunnelManager().getInboundSettings(d.calculateHash());
        String name = (in != null ? in.getDestinationNickname() : null);
        if (name == null) {
            TunnelPoolSettings out = ctx.tunnelManager().getOutboundSettings(d.calculateHash());
            name = (out != null ? out.getDestinationNickname() : null);
        }

        if (name == null)
            name = d.calculateHash().toBase64().substring(0, 6);
        else
            name = _(ctx, name);

        return name;
    }

    private String _(RouterContext ctx, String s) {
        if (SHARED_CLIENTS.equals(s))
            return getString(R.string.shared_clients);
        else
            return s;
    }

    private void checkDialog() {
        final I2PActivityBase ab = (I2PActivityBase) getActivity();
        String language = PreferenceManager.getDefaultSharedPreferences(ab).getString(
                getString(R.string.PREF_LANGUAGE), null
        );
        if (language == null) {
            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            b.setTitle(R.string.choose_language)
                    .setItems(R.array.language_names, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Save the language choice
                            String language = getResources().getStringArray(R.array.languages)[which];
                            PreferenceManager.getDefaultSharedPreferences(getActivity())
                                    .edit()
                                    .putString(getString(R.string.PREF_LANGUAGE), language)
                                    .commit();
                            // Close the dialog
                            dialog.dismiss();
                            // Broadcast the change to RouterService just in case the router is running
                            Intent intent = new Intent(RouterService.LOCAL_BROADCAST_LOCALE_CHANGED);
                            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                            // Update the parent
                            ab.notifyLocaleChanged();
                            // Run checkDialog() again to show the next dialog
                            // (if the change doesn't restart the Activity)
                            checkDialog();
                        }
                    })
                    .setCancelable(false)
                    .show();
        } else if (ab.getPref(PREF_CONFIGURE_BROWSER, true)) {
            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            b.setTitle(R.string.configure_browser_title)
                    .setMessage(R.string.configure_browser_for_i2p)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.dismiss();
                            ab.setPref(PREF_CONFIGURE_BROWSER, false);
                            Intent hi = new Intent(getActivity(), BrowserConfigActivity.class);
                            startActivity(hi);
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.cancel();
                            ab.setPref(PREF_CONFIGURE_BROWSER, false);
                        }
                    })
                    .show();
        }
        /*VersionDialog dialog = new VersionDialog();
        String oldVersion = ((I2PActivityBase) getActivity()).getPref(PREF_INSTALLED_VERSION, "??");
        if(oldVersion.equals("??")) {
            // TODO Don't show this dialog until it is reworked
            Bundle args = new Bundle();
            args.putInt(VersionDialog.DIALOG_TYPE, VersionDialog.DIALOG_NEW_INSTALL);
            dialog.setArguments(args);
            dialog.show(getActivity().getSupportFragmentManager(), "newinstall");
        } else {
            // TODO Don't show dialog on new version until we have something new to tell them
            String currentVersion = Util.getOurVersion(getActivity());
            if(!oldVersion.equals(currentVersion)) {
                Bundle args = new Bundle();
                args.putInt(VersionDialog.DIALOG_TYPE, VersionDialog.DIALOG_NEW_VERSION);
                dialog.setArguments(args);
                dialog.show(getActivity().getSupportFragmentManager(), "newversion");
            }
        }*/
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
