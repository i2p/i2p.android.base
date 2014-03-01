package net.i2p.android.router.service;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CopyOnWriteArrayList;

import net.i2p.router.RouterContext;
import net.i2p.stat.Rate;
import net.i2p.stat.RateStat;

public class StatSummarizer implements Runnable {
    private final RouterContext _context;
    private final List<SummaryListener> _listeners;
    // TODO remove static instance
    private static StatSummarizer _instance;
    private volatile boolean _isRunning = true;
    private Thread _thread;

    public StatSummarizer() {
        _context = RouterContext.listContexts().get(0);
        _listeners = new CopyOnWriteArrayList<SummaryListener>();
        _instance = this;
        _context.addShutdownTask(new Shutdown());
    }

    public static StatSummarizer instance() { return _instance; }

    public void run() {
        _thread = Thread.currentThread();
        String specs = "";
        while (_isRunning && _context.router().isAlive()) {
            specs = adjustDatabases(specs);
            try { Thread.sleep(60 * 1000);} catch (InterruptedException ie) {}
        }
    }

    /** list of SummaryListener instances */
    public List<SummaryListener> getListeners() { return _listeners; }

    public SummaryListener getListener(String rateName, long period) {
        for (SummaryListener lsnr : _listeners) {
            if (lsnr.getName().equals(rateName + "." + period))
                return lsnr;
        }
        return null;
    }

    private static final String DEFAULT_DATABASES =
               "bw.sendRate.60000"
            + ",bw.recvRate.60000"
            + ",router.memoryUsed.60000"
            + ",router.activePeers.60000";

    private String adjustDatabases(String oldSpecs) {
        String spec = _context.getProperty("stat.summaries", DEFAULT_DATABASES);
        if ( ( (spec == null) && (oldSpecs == null) ) ||
                ( (spec != null) && (oldSpecs != null) && (oldSpecs.equals(spec))) )
               return oldSpecs;

        List<Rate> old = parseSpecs(oldSpecs);
        List<Rate> newSpecs = parseSpecs(spec);

        // remove old ones
        for (Rate r : old) {
            if (!newSpecs.contains(r))
                removeDb(r);
        }
        // add new ones
        StringBuilder buf = new StringBuilder();
        boolean comma = false;
        for (Rate r : newSpecs) {
            if (!old.contains(r))
                addDb(r);
            if (comma)
                buf.append(',');
            else
                comma = true;
            buf.append(r.getRateStat().getName()).append(".").append(r.getPeriod());
        }
        return buf.toString();
    }

    private void removeDb(Rate r) {
        for (SummaryListener lsnr : _listeners) {
            if (lsnr.getRate().equals(r)) {
                // no iter.remove() in COWAL
                _listeners.remove(lsnr);
                lsnr.stopListening();
                return;
            }
        }
    }
    private void addDb(Rate r) {
        SummaryListener lsnr = new SummaryListener(r);
        lsnr.startListening();
        _listeners.add(lsnr);
    }

    /**
     * @param specs statName.period,statName.period,statName.period
     * @return list of Rate objects
     */
    List<Rate> parseSpecs(String specs) {
        StringTokenizer tok = new StringTokenizer(specs, ",");
        List<Rate> rv = new ArrayList<Rate>();
        while (tok.hasMoreTokens()) {
            String spec = tok.nextToken();
            int split = spec.lastIndexOf('.');
            if ( (split <= 0) || (split + 1 >= spec.length()) )
                continue;
            String name = spec.substring(0, split);
            String per = spec.substring(split+1);
            long period = -1;
            try {
                period = Long.parseLong(per);
                RateStat rs = _context.statManager().getRate(name);
                if (rs != null) {
                    Rate r = rs.getRate(period);
                    if (r != null)
                        rv.add(r);
                }
            } catch (NumberFormatException nfe) {}
        }
        return rv;
    }

    private class Shutdown implements Runnable {
        public void run() {
            _isRunning = false;
            if (_thread != null)
                _thread.interrupt();
            for (SummaryListener lsnr : _listeners) {
                lsnr.stopListening();
            }
            _listeners.clear();
        }
    }
}
