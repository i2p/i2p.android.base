package net.i2p.android.router.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.i2p.android.router.R;

public class MainActivity extends I2PActivityBase {

    private Handler _handler;
    private Runnable _updater;

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
    }

    private class Updater implements Runnable {
        public void run() {
            updateVisibility();
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
}
