package net.i2p.android.router.util;

import net.i2p.android.router.R;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
        NotificationCompat.Builder b =
                new NotificationCompat.Builder(mCtx)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(ICON)
                .setAutoCancel(true);

        if (c != null) {
            Intent intent = new Intent(mCtx, c);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(mCtx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            b.setContentIntent(pi);
        }

        mNotificationManager.notify(7175, b.build());
    }
}
