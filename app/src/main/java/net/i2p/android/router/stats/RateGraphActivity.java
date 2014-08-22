package net.i2p.android.router.stats;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import net.i2p.android.router.I2PActivityBase;
import net.i2p.android.router.R;
import net.i2p.android.router.SettingsActivity;
import net.i2p.android.router.service.StatSummarizer;
import net.i2p.android.router.service.SummaryListener;
import net.i2p.stat.Rate;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

public class RateGraphActivity extends I2PActivityBase {
    private static final String SELECTED_RATE = "selected_rate";

    private boolean mFinishOnResume;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDrawerToggle.setDrawerIndicatorEnabled(false);

        if (StatSummarizer.instance() != null) {
            // Get the rates currently being graphed
            List<SummaryListener> listeners = StatSummarizer.instance().getListeners();
            TreeSet<SummaryListener> ordered = new TreeSet<SummaryListener>(new AlphaComparator());
            ordered.addAll(listeners);

            if (ordered.size() > 0) {
                // Extract the rates and periods
                final String[] mRates = new String[ordered.size()];
                final long[] mPeriods = new long[ordered.size()];
                int i = 0;
                for (SummaryListener listener : ordered) {
                    Rate r = listener.getRate();
                    mRates[i] = r.getRateStat().getName();
                    mPeriods[i] = r.getPeriod();
                    i++;
                }

                // Set up action bar for drop-down list
                ActionBar actionBar = getSupportActionBar();
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

                SpinnerAdapter mSpinnerAdapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_dropdown_item, mRates);

                ActionBar.OnNavigationListener mNavigationListener = new ActionBar.OnNavigationListener() {
                    String[] rates = mRates;
                    long[] periods = mPeriods;

                    public boolean onNavigationItemSelected(int position, long itemId) {
                        String rateName = rates[position];
                        long period = periods[position];
                        RateGraphFragment f = RateGraphFragment.newInstance(rateName, period);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.main_fragment, f, rates[position]).commit();
                        return true;
                    }
                };

                actionBar.setListNavigationCallbacks(mSpinnerAdapter, mNavigationListener);

                if (savedInstanceState != null) {
                    int selected = savedInstanceState.getInt(SELECTED_RATE);
                    actionBar.setSelectedNavigationItem(selected);
                }
            } else {
                DialogFragment df = new DialogFragment() {
                    @Override
                    public Dialog onCreateDialog(Bundle savedInstanceState) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(R.string.no_graphs_configured)
                                .setPositiveButton(R.string.configure_graphs, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        mFinishOnResume = true;
                                        Intent i = new Intent(RateGraphActivity.this, SettingsActivity.class);
                                        // Navigation to a sub-category doesn't seem to work yet
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                                            i.setAction(SettingsActivity.ACTION_PREFS_GRAPHS);
                                        } else {
                                            i.putExtra("settings", "graphs");
                                        }
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
                        return builder.create();
                    }
                };
                df.show(getSupportFragmentManager(), "nographs");
            }
        } else {
            DialogFragment df = new DialogFragment() {
                @Override
                public Dialog onCreateDialog(Bundle savedInstanceState) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(R.string.graphs_not_ready)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    finish();
                                }
                            })
                            .setCancelable(false);
                    return builder.create();
                }
            };
            df.show(getSupportFragmentManager(), "graphsnotready");
        }
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
        outState.putInt(SELECTED_RATE,
                getSupportActionBar().getSelectedNavigationIndex());
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
