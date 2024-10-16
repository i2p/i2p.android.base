package net.i2p.android.router.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.widget.Toast;

import net.i2p.android.I2PActivity;
import net.i2p.android.router.R;
import net.i2p.android.router.util.Notifications;

class StatusBar {

    private Context mCtx;
    private final NotificationManager mNotificationManager;
    private final NotificationCompat.Builder mNotifyBuilder;
    private Notification mNotif;
    private final String NOTIFICATION_CHANNEL_ID = "net.i2p.android.STARTUP_STATE_CHANNEL";
    private final String channelName = "I2P Router Service";
    NotificationChannel mNotificationChannel;

    private static final int ID = 1337;

    public static final int ICON_STARTING = R.drawable.ic_stat_router_starting;
    public static final int ICON_RUNNING = R.drawable.ic_stat_router_running;
    public static final int ICON_ACTIVE = R.drawable.ic_stat_router_active;
    public static final int ICON_STOPPING = R.drawable.ic_stat_router_stopping;
    public static final int ICON_SHUTTING_DOWN = R.drawable.ic_stat_router_shutting_down;
    public static final int ICON_WAITING_NETWORK = R.drawable.ic_stat_router_waiting_network;

    StatusBar(Context ctx) {
        mCtx = ctx;
        mNotificationManager = (NotificationManager) mCtx.getSystemService(
                Context.NOTIFICATION_SERVICE);
        assert mNotificationManager != null;

        Thread.currentThread().setUncaughtExceptionHandler(
                new CrashHandler(mNotificationManager));

        int icon = ICON_STARTING;
        // won't be shown if replace() is called
        String text = mCtx.getString(R.string.notification_status_starting);
        mNotifyBuilder = notifyBuilder(icon, text);
    }

    private NotificationCompat.Builder notifyBuilder(int icon, String text) {
        NotificationCompat.Builder tNotifyBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tNotifyBuilder = new NotificationCompat.Builder(mCtx);
        } else {
            tNotifyBuilder = new NotificationCompat.Builder(mCtx, NOTIFICATION_CHANNEL_ID);
        }
        tNotifyBuilder.setContentText(text);
        tNotifyBuilder.setSmallIcon(icon);
        tNotifyBuilder.setColor(mCtx.getResources().getColor(R.color.primary_light));
        tNotifyBuilder.setOngoing(true);
        tNotifyBuilder.setPriority(NotificationManager.IMPORTANCE_LOW);
        tNotifyBuilder.setCategory(Notification.CATEGORY_SERVICE);

        PendingIntent pi = this.pendingIntent();
        tNotifyBuilder.setContentIntent(pi);
        return tNotifyBuilder;
    }

    private PendingIntent pendingIntent() {
        Intent intent = new Intent(mCtx, I2PActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pi = PendingIntent.getActivity(mCtx,
                    0,
                    intent,
                    PendingIntent.FLAG_MUTABLE);
        } else {
            pi = PendingIntent.getActivity(mCtx,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return pi;
    }

    public void replace(int icon, int textResource) {
        PendingIntent pi = pendingIntent();
        mNotifyBuilder.setContentIntent(pi);
        replace(icon, mCtx.getString(textResource));
    }

    public void replace(int icon, String title) {
        PendingIntent pi = pendingIntent();
        mNotifyBuilder.setContentIntent(pi);
        mNotifyBuilder.setSmallIcon(icon)
            .setStyle(null)
            .setTicker(title);
        update(title);
    }

    public void replaceIntent(int icon, String text, PendingIntent pi) {
        mNotifyBuilder.setContentIntent(pi);
        mNotifyBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
        mNotifyBuilder.setAutoCancel(true);
        mNotifyBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        mNotifyBuilder.setSmallIcon(icon).setContentText(text);
        update(null, text);
    }

    public void replaceIntent(int icon, int textResource, PendingIntent pi) {
        String text = mCtx.getString(textResource);
        mNotifyBuilder.setContentIntent(pi);
        mNotifyBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
        mNotifyBuilder.setAutoCancel(true);
        mNotifyBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        mNotifyBuilder.setSmallIcon(icon).setContentText(text);
        update(null, text);
    }

    public void update(String title) {
        update(title, null);
    }

    public void update(String title, String text, String bigText) {
        mNotifyBuilder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(bigText));
        update(title, text);
    }

    public void update(String title, String text) {
        if (title != null)
            mNotifyBuilder.setContentTitle(title);
        if (text != null)
            mNotifyBuilder.setContentText(text);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(mNotificationChannel);
            mNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            mNotifyBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            //
        }
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
