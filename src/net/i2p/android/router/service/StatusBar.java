package net.i2p.android.router.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import net.i2p.android.router.R;
import net.i2p.android.router.activity.MainActivity;

class StatusBar {

    private final Context ctx;
    private final Intent intent;
    private final Notification notif;
    private final NotificationManager mgr;

    private static final int ID = 1337;

    public static final int ICON1 = R.drawable.ic_launcher_itoopie_300;
    public static final int ICON2 = R.drawable.ic_launcher_itoopie_330;
    public static final int ICON3 = R.drawable.ic_launcher_itoopie;
    public static final int ICON4 = R.drawable.ic_launcher_itoopie_120;
    public static final int ICON5 = R.drawable.ic_launcher_itoopie_150;
    public static final int ICON6 = R.drawable.ic_launcher_itoopie_180;

    StatusBar(Context cx) {
        ctx = cx;
        String ns = Context.NOTIFICATION_SERVICE;
        mgr = (NotificationManager)ctx.getSystemService(ns);
        Thread.currentThread().setUncaughtExceptionHandler(new CrashHandler(mgr));

        int icon = ICON1;
        // won't be shown if replace() is called
        String text = "Starting I2P";
        long now = System.currentTimeMillis();
        notif = new Notification(icon, text, now);
        notif.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
        // notif.flags |= Notification.FLAG_ONGOING_EVENT;
        notif.flags |= Notification.FLAG_NO_CLEAR;
        intent = new Intent(ctx, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /** remove and re-add */
    public void replace(int icon, String tickerText) {
        off();
        notif.icon = icon;
        notif.tickerText= tickerText;
        update(tickerText);
    }

    public void update(String details) {
        String title = "I2P Status";
        update(title, details);
    }

    public void update(String title, String details) {
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notif.setLatestEventInfo(ctx, title, details, pi);
        mgr.notify(ID, notif);
    }

    public void off() {
        //mgr.cancel(ID);
    }

    public void remove() {
        mgr.cancel(ID);
    }

    /**
     * http://stackoverflow.com/questions/4028742/how-to-clear-a-notification-if-activity-crashes
     */
    private static class CrashHandler implements Thread.UncaughtExceptionHandler {

        private final Thread.UncaughtExceptionHandler defaultUEH;
        private final NotificationManager mgr;

        public CrashHandler(NotificationManager nMgr) {
            defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
            mgr = nMgr;
        }

        public void uncaughtException(Thread t, Throwable e) {
            if (mgr != null) {
                try {
                    mgr.cancel(ID);
                } catch (Throwable ex) {}
            }
            System.err.println("In CrashHandler " + e);
            e.printStackTrace(System.err);
            defaultUEH.uncaughtException(t, e);
        }
    }

    public Notification getNote() {
        return notif;
    }
}
