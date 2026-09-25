package gr.vassili.work;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.PowerManager;

public class AlarmReceiver extends BroadcastReceiver {

    static final String CHANNEL = "work_shift";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        int id      = intent.getIntExtra("id", 1);
        String title = intent.getStringExtra("title");
        String body  = intent.getStringExtra("body");

        /* αν η εφαρμογή είναι ανοιχτή μπροστά, το μήνυμα το δείχνει η ίδια */
        if (MainActivity.foreground) {
            ctx.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
               .edit().remove("alarm_" + id).apply();
            return;
        }

        /* άναψε την οθόνη, όπως κάνουν οι κλήσεις */
        try {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                  | PowerManager.ACQUIRE_CAUSES_WAKEUP
                  | PowerManager.ON_AFTER_RELEASE, "worktimesheet:alarm");
            wl.acquire(10000);
        } catch (Exception ignored) { }

        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel ch = new NotificationChannel(
                CHANNEL, "Βάρδια", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Λήξη διαλείμματος και σχόλασμα");
        ch.enableVibration(true);
        ch.setLightColor(Color.GREEN);
        nm.createNotificationChannel(ch);

        Intent openIntent = new Intent(ctx, MainActivity.class);
        openIntent.putExtra("fromAlarm", true);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent open = PendingIntent.getActivity(ctx, id,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder nb = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentIntent(open)
                .setFullScreenIntent(open, true);

        if (body != null && !body.isEmpty()) {
            nb.setContentText(body)
              .setStyle(new Notification.BigTextStyle().bigText(body));
        }

        nm.notify(id, nb.build());

        /* το είδε ως notification - η εφαρμογή δεν θα ξαναπεί το ίδιο με popup */
        ctx.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
           .edit().remove("alarm_" + id)
                  .putString("flag_notified_" + id, "1").commit();
    }
}
