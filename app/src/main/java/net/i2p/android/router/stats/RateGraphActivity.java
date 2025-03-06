package net.i2p.android.router.stats;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
//import android.support.v7.app.AlertDialog;
import androidx.appcompat.app.AlertDialog;
//import android.support.v7.widget.Toolbar;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import net.i2p.android.I2PActivityBase;
import net.i2p.android.router.R;
import net.i2p.android.router.SettingsActivity;
import net.i2p.android.router.service.StatSummarizer;
import net.i2p.android.router.service.SummaryListener;
import net.i2p.android.router.util.Util;
import net.i2p.stat.Rate;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class RateGraphActivity extends I2PActivityBase {
    private static final String SELECTED_RATE = "selected_rate";

    private String[] mRates;
    private long[] mPeriods;
    private Spinner mSpinner;
    private boolean mFinishOnResume;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_onepane);

        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (Util.getRouterContext() == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.router_not_running)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setCancelable(false);
            builder.show();
        } else if (StatSummarizer.instance() == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.graphs_not_ready)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setCancelable(false);
            builder.show();
        } else {
            // Get the rates currently being graphed
            List<SummaryListener> listeners = StatSummarizer.instance().getListeners();
            TreeSet<SummaryListener> ordered = new TreeSet<>(new AlphaComparator());
            ordered.addAll(listeners);

            if (ordered.size() > 0) {
                // Extract the rates and periods
                mRates = new String[ordered.size()];
                mPeriods = new long[ordered.size()];
                int i = 0;
                for (SummaryListener listener : ordered) {
                    Rate r = listener.getRate();
                    mRates[i] = r.getRateStat().getName();
                    mPeriods[i] = r.getPeriod();
                    i++;
                }

                mSpinner = (Spinner) findViewById(R.id.main_spinner);
                mSpinner.setVisibility(View.VISIBLE);

                mSpinner.setAdapter(new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_dropdown_item, mRates));

                mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        selectRate(i);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });

                if (savedInstanceState != null) {
                    int selected = savedInstanceState.getInt(SELECTED_RATE);
                    mSpinner.setSelection(selected);
                } else
                    selectRate(0);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.no_graphs_configured)
                        .setPositiveButton(R.string.configure_graphs, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                mFinishOnResume = true;
                                Intent i = new Intent(RateGraphActivity.this, SettingsActivity.class);
                                i.putExtra(SettingsActivity.PREFERENCE_CATEGORY, SettingsActivity.PREFERENCE_CATEGORY_GRAPHS);
                                startActivity(i);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.cancel();
                                finish();
                            }
                        })
                        .setCancelable(false);
                builder.show();
            }
        }
    }

    private void selectRate(int position) {
        String rateName = mRates[position];
        long period = mPeriods[position];
        RateGraphFragment f = RateGraphFragment.newInstance(rateName, period);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment, f, rateName).commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mFinishOnResume) {
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSpinner != null)
            outState.putInt(SELECTED_RATE, mSpinner.getSelectedItemPosition());
    }

    private static class AlphaComparator implements Comparator<SummaryListener> {
        public int compare(SummaryListener l, SummaryListener r) {
            String lName = l.getRate().getRateStat().getName();
            String rName = r.getRate().getRateStat().getName();
            int rv = lName.compareTo(rName);
            if (rv != 0)
                return rv;
            return (int) (l.getRate().getPeriod() - r.getRate().getPeriod());
        }
    }
}
