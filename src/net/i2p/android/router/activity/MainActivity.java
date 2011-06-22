package net.i2p.android.router.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;

import net.i2p.android.router.R;
import net.i2p.android.router.service.RouterService;
import net.i2p.data.DataHelper;
import net.i2p.router.RouterContext;

public class MainActivity extends I2PActivityBase {

    private Handler _handler;
    private Runnable _updater;
    private String _savedStatus;

    protected static final String PROP_NEW_INSTALL = "i2p.newInstall";
    protected static final String PROP_NEW_VERSION = "i2p.newVersion";
    protected static final int DIALOG_NEW_INSTALL = 0;
    protected static final int DIALOG_NEW_VERSION = 1;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button news = (Button) findViewById(R.id.news_button);
        news.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), NewsActivity.class);
                startActivity(intent);
            }
        });

        Button notes = (Button) findViewById(R.id.releasenotes_button);
        notes.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), TextResourceActivity.class);
                intent.putExtra(TextResourceActivity.TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
                startActivity(intent);
            }
        });

        Button licenses = (Button) findViewById(R.id.licenses_button);
        licenses.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), LicenseActivity.class);
                //Intent intent = new Intent(view.getContext(), TextResourceActivity.class);
                //intent.putExtra(TextResourceActivity.TEXT_RESOURCE_ID, R.raw.licenses_txt);
                startActivity(intent);
            }
        });

        Button website = (Button) findViewById(R.id.website_button);
        website.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), WebActivity.class);
                //intent.setData((new Uri.Builder()).scheme("http").authority("www.i2p2.de").path("/").build());
                intent.setData(Uri.parse("http://www.i2p2.de/"));
                startActivity(intent);
            }
        });

        Button faq = (Button) findViewById(R.id.faq_button);
        faq.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), WebActivity.class);
                //intent.setData((new Uri.Builder()).scheme("http").authority("www.i2p2.de").path("/faq").build());
                intent.setData(Uri.parse("http://www.i2p2.de/faq"));
                startActivity(intent);
            }
        });

        Button welcome = (Button) findViewById(R.id.welcome_button);
        welcome.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), WebActivity.class);
                // default is to display the welcome_html resource
                startActivity(intent);
            }
        });

        Button addressbook = (Button) findViewById(R.id.addressbook_button);
        addressbook.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), AddressbookActivity.class);
                startActivity(intent);
            }
        });

        Button start = (Button) findViewById(R.id.router_start_button);
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RouterService svc = _routerService;
                if (svc != null && _isBound) {
                    setAutoStart(true);
                    svc.manualStart();
                } else {
                    startRouter();
                }
                updateVisibility();
            }
        });

        Button stop = (Button) findViewById(R.id.router_stop_button);
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RouterService svc = _routerService;
                if (svc != null && _isBound) {
                    setAutoStart(false);
                    svc.manualStop();
                    updateVisibility();
                }
            }
        });

        Button quit = (Button) findViewById(R.id.router_quit_button);
        quit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RouterService svc = _routerService;
                if (svc != null && _isBound) {
                    setAutoStart(false);
                    svc.manualQuit();
                    updateVisibility();
                }
            }
        });

        if (savedInstanceState != null) {
            String saved = savedInstanceState.getString("status");
            if (saved != null) {
                _savedStatus = saved;
            }
        }

        _handler = new Handler();
        _updater = new Updater();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        _handler.removeCallbacks(_updater);
        if (_savedStatus != null) {
            TextView tv = (TextView) findViewById(R.id.main_status_text);
            tv.setText(_savedStatus);
        }
        _handler.postDelayed(_updater, 100);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        _handler.removeCallbacks(_updater);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        checkDialog();
        updateVisibility();
        updateStatus();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        if (_savedStatus != null)
            outState.putString("status", _savedStatus);
        super.onSaveInstanceState(outState);
    }

    private class Updater implements Runnable {
        private boolean needsCheck = true;
        private int counter;

        public void run() {
            if (getRouterContext() != null && needsCheck) {
                checkDialog();
                needsCheck = false;
            }
            updateVisibility();
            if (counter++ % 3 == 0)
                updateStatus();
            _handler.postDelayed(this, 2500);
        }
    }

    private void updateVisibility() {
        RouterService svc = _routerService;
        boolean showStart = (svc == null) || (!_isBound) || svc.canManualStart();
        Button start = (Button) findViewById(R.id.router_start_button);
        start.setVisibility(showStart ? View.VISIBLE : View.INVISIBLE);

        boolean showStop = svc != null && _isBound && svc.canManualStop();
        Button stop = (Button) findViewById(R.id.router_stop_button);
        stop.setVisibility(showStop ? View.VISIBLE : View.INVISIBLE);

        Button quit = (Button) findViewById(R.id.router_quit_button);
        quit.setVisibility(showStop ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateStatus() {
        RouterContext ctx = getRouterContext();
        TextView tv = (TextView) findViewById(R.id.main_status_text);
        if (ctx != null) {
            int active = ctx.commSystem().countActivePeers();
            int known = Math.max(ctx.netDb().getKnownRouters() - 1, 0);
            int inEx = ctx.tunnelManager().getFreeTunnelCount();
            int outEx = ctx.tunnelManager().getOutboundTunnelCount();
            int inCl = ctx.tunnelManager().getInboundClientTunnelCount();
            int outCl = ctx.tunnelManager().getOutboundClientTunnelCount();
            //int part = _context.tunnelManager().getParticipatingCount();
            double dLag = ctx.statManager().getRate("jobQueue.jobLag").getRate(60000).getAverageValue();
            String jobLag = DataHelper.formatDuration((long) dLag);
            String msgDelay = DataHelper.formatDuration(ctx.throttle().getMessageDelay());
            String uptime = DataHelper.formatDuration(ctx.router().getUptime());
            //String tunnelStatus = _context.throttle().getTunnelStatus();
            double inBW = ctx.bandwidthLimiter().getReceiveBps() / 1024;
            double outBW = ctx.bandwidthLimiter().getSendBps() / 1024;
            // control total width
            DecimalFormat fmt;
            if (inBW >= 1000 || outBW >= 1000)
                fmt = new DecimalFormat("#0");
            else if (inBW >= 100 || outBW >= 100)
                fmt = new DecimalFormat("#0.0");
            else
                fmt = new DecimalFormat("#0.00");

            String status =
                   "ROUTER STATUS" +
                   "\nPeers active/known: " + active + " / " + known +
                   "\nExploratory Tunnels in/out: " + inEx + " / " + outEx +
                   "\nClient Tunnels in/out: " + inCl + " / " + outCl;
                   //" Pt " + part +

            String details =
                   "\nBandwidth in/out: " + fmt.format(inBW) + " / " + fmt.format(outBW) + " KBps" +
                   "\nJob Lag: " + jobLag +
                   "\nMsg Delay: " + msgDelay +
                   "\nUptime: " + uptime;

            _savedStatus = status + details;
            tv.setText(_savedStatus);
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.INVISIBLE);
        }
    }

    private void checkDialog() {
        if (Boolean.valueOf(System.getProperty(PROP_NEW_INSTALL)).booleanValue()) {
            showDialog(DIALOG_NEW_INSTALL);
        } else if (Boolean.valueOf(System.getProperty(PROP_NEW_VERSION)).booleanValue()) {
            showDialog(DIALOG_NEW_VERSION);
        }
    }

    protected Dialog onCreateDialog(int id) {
        Dialog rv = null;
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        switch (id) {
          case DIALOG_NEW_INSTALL:
            b.setMessage(getResources().getText(R.string.welcome_new_install))
              .setCancelable(false)
              .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           System.setProperty(PROP_NEW_INSTALL, "false");
                           System.setProperty(PROP_NEW_VERSION, "false");
                           dialog.cancel();
                           MainActivity.this.removeDialog(id);
                       }
               })
              .setNeutralButton("Release Notes", new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           System.setProperty(PROP_NEW_INSTALL, "false");
                           System.setProperty(PROP_NEW_VERSION, "false");
                           dialog.cancel();
                           MainActivity.this.removeDialog(id);
                           Intent intent = new Intent(MainActivity.this, TextResourceActivity.class);
                           intent.putExtra(TextResourceActivity.TEXT_RESOURCE_ID, R.raw.releasenotes_txt);
                           startActivity(intent);
                       }
               })
              .setNegativeButton("Licenses", new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           System.setProperty(PROP_NEW_INSTALL, "false");
                           System.setProperty(PROP_NEW_VERSION, "false");
                           dialog.cancel();
                           MainActivity.this.removeDialog(id);
                           Intent intent = new Intent(MainActivity.this, LicenseActivity.class);
                           startActivity(intent);
                       }
               });
            rv = b.create();
            break;

          case DIALOG_NEW_VERSION:
            b.setMessage(getResources().getText(R.string.welcome_new_version) + " " + System.getProperty("i2p.version"))
              .setCancelable(true)
              .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           System.setProperty(PROP_NEW_VERSION, "false");
                           try {
                               dialog.dismiss();
                           } catch (Exception e) {}
                           MainActivity.this.removeDialog(id);
                       }
               })
              .setNegativeButton("Release Notes", new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           System.setProperty(PROP_NEW_VERSION, "false");
                           try {
                               dialog.dismiss();
                           } catch (Exception e) {}
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
    }
}
