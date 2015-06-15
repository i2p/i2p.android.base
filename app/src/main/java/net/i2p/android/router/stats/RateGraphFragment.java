package net.i2p.android.router.stats;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidplot.Plot;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.XYStepMode;

import net.i2p.android.router.I2PFragmentBase;
import net.i2p.android.router.R;
import net.i2p.android.router.service.StatSummarizer;
import net.i2p.android.router.service.SummaryListener;
import net.i2p.android.router.util.Util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

public class RateGraphFragment extends I2PFragmentBase {
    // redraws a plot whenever an update is received:
    private class MyPlotUpdater implements Observer {
        Plot plot;

        public MyPlotUpdater(Plot plot) {
            this.plot = plot;
        }

        public void update(Observable o, Object arg) {
            Util.d("Redrawing plot");
            plot.redraw();
        }
    }

    public static final String RATE_NAME = "rate_name";
    public static final String RATE_PERIOD = "rate_period";

    private Handler _handler;
    private SetupTask _setupTask;
    private SummaryListener _listener;
    private XYPlot _ratePlot;
    private MyPlotUpdater _plotUpdater;

    public static RateGraphFragment newInstance(String name, long period) {
        RateGraphFragment f = new RateGraphFragment();
        Bundle args = new Bundle();
        args.putString(RATE_NAME, name);
        args.putLong(RATE_PERIOD, period);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _handler = new Handler();
        _setupTask = new SetupTask();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_graph, container, false);

        _ratePlot = (XYPlot) v.findViewById(R.id.rate_stat_plot);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        _handler.removeCallbacks(_setupTask);
        _handler.postDelayed(_setupTask, 100);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (_listener != null && _plotUpdater != null) {
            Util.d("Removing plot updater from listener");
            _listener.removeObserver(_plotUpdater);
        }
        _handler.removeCallbacks(_setupTask);
    }

    private class SetupTask implements Runnable {
        public void run() {
            String rateName = getArguments().getString(RATE_NAME);
            long period = getArguments().getLong(RATE_PERIOD);
            int k = 1000;
            if (rateName.startsWith("bw.") || rateName.contains("Size") || rateName.contains("Bps") || rateName.contains("memory"))
                k = 1024;

            Util.d("Setting up " + rateName + "." + period);
            if (StatSummarizer.instance() == null) {
                Util.d("StatSummarizer is null, delaying setup");
                _handler.postDelayed(this, 1000);
                return;
            }
            _listener = StatSummarizer.instance().getListener(rateName, period);
            if (_listener == null) {
                Util.d("Listener is null, delaying setup");
                _handler.postDelayed(this, 1000);
                return;
            }

            XYSeries rateSeries = _listener.getSeries();

            _plotUpdater = new MyPlotUpdater(_ratePlot);

            _ratePlot.addSeries(rateSeries, new LineAndPointFormatter(Color.rgb(0, 0, 0), null, Color.rgb(0, 80, 0), null));
            _ratePlot.calculateMinMaxVals();
            long maxX = _ratePlot.getCalculatedMaxX().longValue();
            final double maxY = _ratePlot.getCalculatedMaxY().doubleValue();

            Util.d("Adding plot updater to listener");
            _listener.addObserver(_plotUpdater);

            // Only one line, so hide the legend
            _ratePlot.getLegendWidget().setVisible(false);

            _ratePlot.setDomainUpperBoundary(maxX, BoundaryMode.GROW);
            _ratePlot.setDomainStep(XYStepMode.INCREMENT_BY_VAL, 15 * 60 * 1000);
            _ratePlot.setTicksPerDomainLabel(4);

            _ratePlot.setRangeLowerBoundary(0, BoundaryMode.FIXED);
            _ratePlot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, getRangeStep(maxY, k));
            _ratePlot.setTicksPerRangeLabel(5);

            _ratePlot.setDomainValueFormat(new Format() {
                private DateFormat dateFormat = SimpleDateFormat.getTimeInstance(DateFormat.SHORT);

                @Override
                public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo,
                                           @NonNull FieldPosition pos) {
                    long when = ((Number) obj).longValue();
                    Date date = new Date(when);
                    return dateFormat.format(date, toAppendTo, pos);
                }

                @Override
                public Object parseObject(String s, @NonNull ParsePosition parsePosition) {
                    return null;
                }
            });

            final int finalK = k;
            _ratePlot.setRangeValueFormat(new Format() {

                @Override
                public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo,
                                           @NonNull FieldPosition pos) {
                    double val = ((Number) obj).doubleValue();

                    if (val == 0 || maxY < finalK) {
                        return new DecimalFormat("0").format(val, toAppendTo, pos);
                    } else if (maxY < finalK * finalK) {
                        if (val < 10 * finalK)
                            return new DecimalFormat("0.0 k").format(val / (1000), toAppendTo, pos);
                        else
                            return new DecimalFormat("0 k").format(val / (1000), toAppendTo, pos);
                    } else {
                        if (val < 10 * finalK * finalK)
                            return new DecimalFormat("0.0 M").format(val / (finalK * finalK), toAppendTo, pos);
                        else
                            return new DecimalFormat("0 M").format(val / (finalK * finalK), toAppendTo, pos);
                    }
                }

                @Override
                public Object parseObject(String source, @NonNull ParsePosition pos) {
                    return null;
                }

            });

            Util.d("Redrawing plot");
            _ratePlot.redraw();
        }
    }

    private double getRangeStep(double maxY, int k) {
        if (maxY >= k * k)
            return getRangeStepForScale(maxY, k * k);
        else if (maxY >= k)
            return getRangeStepForScale(maxY, k);
        else
            return getRangeStepForScale(maxY, 1);
    }

    private double getRangeStepForScale(double maxY, int scale) {
        if (maxY >= 400 * scale)
            return 40 * scale;
        else if (maxY >= 200 * scale)
            return 20 * scale;
        else if (maxY >= 100 * scale)
            return 10 * scale;
        else if (maxY >= 40 * scale)
            return 4 * scale;
        else if (maxY >= 20 * scale)
            return 2 * scale;
        else if (maxY >= 10 * scale)
            return scale;
        else if (maxY >= 4 * scale)
            return 0.4 * scale;
        else if (maxY >= 2 * scale)
            return 0.2 * scale;
        else
            return 0.1 * scale;
    }
}
