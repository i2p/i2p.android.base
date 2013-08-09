package net.i2p.android.router.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.io.File;
import java.text.DecimalFormat;
import net.i2p.android.router.R;
import net.i2p.android.router.activity.AddressbookActivity;
import net.i2p.android.router.activity.LicenseActivity;
import net.i2p.android.router.activity.LogActivity;
import net.i2p.android.router.service.RouterService;
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
    protected static final int DIALOG_NEW_INSTALL = 0;
    protected static final int DIALOG_NEW_VERSION = 1;

    /**
     * Called when the fragment is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Init stuff here so settings work.
        _myDir = getActivity().getFilesDir().getAbsolutePath();
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
                Intent intent = new Intent(view.getContext(), NewsFragment.class);
                startActivity(intent);
            }
        });

        b = (Button) v.findViewById(R.id.releasenotes_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), TextResourceFragment.class);
                intent.putExtra(TextResourceFragment.TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
                startActivity(intent);
            }
        });

        b = (Button) v.findViewById(R.id.licenses_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), LicenseActivity.class);
                //Intent intent = new Intent(view.getContext(), TextResourceActivity.class);
                //intent.putExtra(TextResourceActivity.TEXT_RESOURCE_ID, R.raw.licenses_txt);
                startActivity(intent);
            }
        });

        b = (Button) v.findViewById(R.id.website_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), WebFragment.class);
                //intent.setData((new Uri.Builder()).scheme("http").authority("www.i2p2.de").path("/").build());
                intent.setData(Uri.parse("http://www.i2p2.de/"));
                startActivity(intent);
            }
        });

        b = (Button) v.findViewById(R.id.faq_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), WebFragment.class);
                //intent.setData((new Uri.Builder()).scheme("http").authority("www.i2p2.de").path("/faq").build());
                intent.setData(Uri.parse("http://www.i2p2.de/faq"));
                startActivity(intent);
            }
        });

        b = (Button) v.findViewById(R.id.welcome_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), WebFragment.class);
                intent.putExtra(WebFragment.HTML_RESOURCE_ID, R.raw.welcome_html);
                startActivity(intent);
            }
        });

        b = (Button) v.findViewById(R.id.addressbook_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), AddressbookActivity.class);
                startActivity(intent);
            }
        });

        b = (Button) v.findViewById(R.id.logs_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), LogActivity.class);
                startActivity(intent);
            }
        });

        b = (Button) v.findViewById(R.id.error_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), LogActivity.class);
                intent.putExtra(LogActivity.ERRORS_ONLY, true);
                startActivity(intent);
            }
        });

        b = (Button) v.findViewById(R.id.peers_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), PeersFragment.class);
                startActivity(intent);
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
                RouterService svc = _routerService;
                if(svc != null && _isBound) {
                    setPref(PREF_AUTO_START, true);
                    svc.manualStart();
                } else {
                    (new File(_myDir, "wrapper.log")).delete();
                    startRouter();
                }
                updateOneShot();
            }
        });

        b = (Button) v.findViewById(R.id.router_quit_button);
        b.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {
                RouterService svc = _routerService;
                if(svc != null && _isBound) {
                    setPref(PREF_AUTO_START, false);
                    svc.manualQuit();
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
        RouterService svc = _routerService;
        boolean showStart = ((svc == null) || (!_isBound) || svc.canManualStart())
                && Util.isConnected(getActivity());
        Button start = (Button) getActivity().findViewById(R.id.router_start_button);
        start.setVisibility(showStart ? View.VISIBLE : View.INVISIBLE);

        boolean showStop = svc != null && _isBound && svc.canManualStop();
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
        String oldVersion = getPref(PREF_INSTALLED_VERSION, "??");
        /*if(oldVersion.equals("??")) {
            getActivity().showDialog(DIALOG_NEW_INSTALL);
        } else {
            String currentVersion = Util.getOurVersion(getActivity());
            if(!oldVersion.equals(currentVersion)) {
                getActivity().showDialog(DIALOG_NEW_VERSION);
            }
        }*/
    }

    /*@Override
    protected Dialog onCreateDialog(int id) {
        final String currentVersion = Util.getOurVersion(this);
        Dialog rv = null;
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        switch(id) {
            case DIALOG_NEW_INSTALL:
                b.setMessage(getResources().getText(R.string.welcome_new_install)).setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        setPref(PREF_INSTALLED_VERSION, currentVersion);
                        dialog.cancel();
                        MainActivity.this.removeDialog(id);
                    }
                }).setNeutralButton("Release Notes", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        setPref(PREF_INSTALLED_VERSION, currentVersion);
                        dialog.cancel();
                        MainActivity.this.removeDialog(id);
                        Intent intent = new Intent(MainActivity.this, TextResourceActivity.class);
                        intent.putExtra(TextResourceActivity.TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
                        startActivity(intent);
                    }
                }).setNegativeButton("Licenses", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        setPref(PREF_INSTALLED_VERSION, currentVersion);
                        dialog.cancel();
                        MainActivity.this.removeDialog(id);
                        Intent intent = new Intent(MainActivity.this, LicenseActivity.class);
                        startActivity(intent);
                    }
                });
                rv = b.create();
                break;

            case DIALOG_NEW_VERSION:
                b.setMessage(getResources().getText(R.string.welcome_new_version) + " " + currentVersion).setCancelable(true).setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        setPref(PREF_INSTALLED_VERSION, currentVersion);
                        try {
                            dialog.dismiss();
                        } catch(Exception e) {
                        }
                        MainActivity.this.removeDialog(id);
                    }
                }).setNegativeButton("Release Notes", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        setPref(PREF_INSTALLED_VERSION, currentVersion);
                        try {
                            dialog.dismiss();
                        } catch(Exception e) {
                        }
                        MainActivity.this.removeDialog(id);
                        Intent intent = new Intent(MainActivity.this, TextResourceActivity.class);
                        intent.putExtra(TextResourceActivity.TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
                        startActivity(intent);
                    }
                });

                rv = b.create();
                break;
        }
        return rv;
    }*/
}
