package net.i2p.android.router.service;

import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;

import net.i2p.I2PAppContext;
import net.i2p.stat.Rate;
import net.i2p.stat.RateStat;
import net.i2p.stat.RateSummaryListener;

import java.util.Observable;
import java.util.Observer;

public class SummaryListener implements RateSummaryListener {
    public static final int HISTORY_SIZE = 30;

    private final I2PAppContext _context;
    private final Rate _rate;
    private String _name;
    private SimpleXYSeries _series;
    private MyObservable _notifier;

    public SummaryListener(Rate r) {
        _context = I2PAppContext.getGlobalContext();
        _rate = r;
        _notifier = new MyObservable();
    }

    // encapsulates management of the observers watching this rate for update events:
    class MyObservable extends Observable {
        @Override
        public void notifyObservers() {
            setChanged();
            super.notifyObservers();
        }
    }

    public void addObserver(Observer observer) {
        _notifier.addObserver(observer);
    }

    public void removeObserver(Observer observer) {
        _notifier.deleteObserver(observer);
    }

    public void add(double totalValue, long eventCount, double totalEventTime,
            long period) {
        long when = now();
        double val = eventCount > 0 ? (totalValue / eventCount) : 0d;

        if (_series.size() > HISTORY_SIZE)
            _series.removeFirst();

        _series.addLast(when, val);

        _notifier.notifyObservers();
    }

    public Rate getRate() { return _rate; }

    public String getName() { return _name; }

    public XYSeries getSeries() { return _series; }

    long now() { return _context.clock().now(); }

    public void startListening() {
        RateStat rs = _rate.getRateStat();
        long period = _rate.getPeriod();
        _name = rs.getName() + "." + period;
        _series = new SimpleXYSeries(_name);
        _rate.setSummaryListener(this);
    }

    public void stopListening() {
        _rate.setSummaryListener(null);
    }
}
