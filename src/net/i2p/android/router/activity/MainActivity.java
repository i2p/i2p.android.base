package net.i2p.android.router.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;

import net.i2p.android.router.R;
import net.i2p.data.DataHelper;
import net.i2p.router.RouterContext;

public class MainActivity extends I2PActivityBase {

    private Handler _handler;
    private Runnable _updater;
    private int _counter;

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
                startActivityForResult(intent, 0);
            }
        });

        Button start = (Button) findViewById(R.id.router_start_button);
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (_routerService != null && _isBound) {
                    _routerService.manualStart();
                     updateVisibility();
                }
            }
        });

        Button stop = (Button) findViewById(R.id.router_stop_button);
        stop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (_routerService != null && _isBound) {
                    _routerService.manualStop();
                     updateVisibility();
                }
            }
        });

        _handler = new Handler();
        _updater = new Updater();
    }


    @Override
    public void onStart()
    {
        super.onStart();
        _handler.removeCallbacks(_updater);
        _handler.postDelayed(_updater, 50);
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
        updateVisibility();
        updateStatus();
    }

    private class Updater implements Runnable {
        public void run() {
            updateVisibility();
            if (++_counter % 3 == 0)
                updateStatus();
            _handler.postDelayed(this, 2500);
        }
    }

    private void updateVisibility() {
        boolean showStart = _routerService != null && _isBound && _routerService.canManualStart();
        Button start = (Button) findViewById(R.id.router_start_button);
        start.setVisibility(showStart ? View.VISIBLE : View.INVISIBLE);

        boolean showStop = _routerService != null && _isBound && _routerService.canManualStop();
        Button stop = (Button) findViewById(R.id.router_stop_button);
        stop.setVisibility(showStop ? View.VISIBLE : View.INVISIBLE);
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
                   "Router status: " +
                   " Peers " + active + '/' + known +
                   "; Expl. Tunnels " + inEx + '/' + outEx +
                   "; Client Tunnels " + inCl + '/' + outCl;
                   //" Pt " + part +

            String details =
                   "; Bandwidth " + fmt.format(inBW) + '/' + fmt.format(outBW) + " KBps" +
                   "; Job Lag " + jobLag +
                   "; Msg Delay " + msgDelay +
                   "; Up " + uptime;

            tv.setText(status + details);
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.INVISIBLE);
        }
    }
}
