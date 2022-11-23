package net.i2p.android.router.util;

import net.i2p.android.router.R;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

public class Notifications {
    private final Context mCtx;
    private final NotificationManager mNotificationManager;



    public static final int ICON = R.drawable.ic_stat_router_active;

    public Notifications(Context ctx) {
        mCtx = ctx;
        mNotificationManager = (NotificationManager) ctx.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    public void notify(String title, String text) {
        notify(title, text, null);
    }

    public void notify(String title, String text, Class<?> c) {
        notify(title, text, "", c);
    }

    public void notify(String title, String text, String channel, Class<?> c) {
        NotificationCompat.Builder b;
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            b = new NotificationCompat.Builder(mCtx);
        } else {
            if (channel.equals("")){
                b = new NotificationCompat.Builder(mCtx);
            } else {
                b = new NotificationCompat.Builder(mCtx, channel);
            }
        }

        b.setContentTitle(title);
        b.setContentText(text);
        b.setSmallIcon(ICON);
        b.setColor(mCtx.getResources().getColor(R.color.primary_light));
        b.setAutoCancel(true);

        if (c != null) {
            Intent intent = new Intent(mCtx, c);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                pi = PendingIntent.getActivity(mCtx, 0, intent, PendingIntent.FLAG_MUTABLE);
            } else {
                pi = PendingIntent.getActivity(mCtx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            b.setContentIntent(pi);
        }

        mNotificationManager.notify(7175, b.build());
    }
}
