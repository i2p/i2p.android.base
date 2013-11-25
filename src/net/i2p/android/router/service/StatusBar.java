package net.i2p.android.router.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import net.i2p.android.router.R;
import net.i2p.android.router.activity.MainActivity;

class StatusBar {

    private final Context ctx;
    private final NotificationManager mNotificationManager;
    private final NotificationCompat.Builder mNotifyBuilder;
    private Notification mNotif;

    private static final int ID = 1337;

    public static final int ICON_STARTING = R.drawable.ic_stat_router_starting;
    public static final int ICON_RUNNING = R.drawable.ic_stat_router_running;
    public static final int ICON_ACTIVE = R.drawable.ic_stat_router_active;
    public static final int ICON_STOPPING = R.drawable.ic_stat_router_stopping;
    public static final int ICON_SHUTTING_DOWN = R.drawable.ic_stat_router_shutting_down;
    public static final int ICON_WAITING_NETWORK = R.drawable.ic_stat_router_waiting_network;

    StatusBar(Context cx) {
        ctx = cx;
        mNotificationManager = (NotificationManager) ctx.getSystemService(
                Context.NOTIFICATION_SERVICE);
        Thread.currentThread().setUncaughtExceptionHandler(
                new CrashHandler(mNotificationManager));

        int icon = ICON_STARTING;
        // won't be shown if replace() is called
        String text = "Starting I2P";

        mNotifyBuilder = new NotificationCompat.Builder(ctx)
            .setContentText(text)
            .setSmallIcon(icon)
            .setOngoing(true)
            .setOnlyAlertOnce(true);

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mNotifyBuilder.setContentIntent(pi);
    }

    public void replace(int icon, String text) {
        mNotifyBuilder.setSmallIcon(icon)
            .setStyle(null)
            .setTicker(text);
        update(text);
    }

    public void update(String text) {
        String title = "I2P Status";
        update(title, text);
    }

    public void update(String title, String text, String bigText) {
        mNotifyBuilder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(bigText));
        update(title, text);
    }

    public void update(String title, String text) {
        mNotifyBuilder.setContentTitle(title)
            .setContentText(text);
        mNotif = mNotifyBuilder.build();
        mNotificationManager.notify(ID, mNotif);
    }

    public void remove() {
        mNotificationManager.cancel(ID);
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
        return mNotif;
    }
}
