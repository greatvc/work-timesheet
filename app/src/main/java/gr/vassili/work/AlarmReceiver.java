package gr.vassili.work;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

public class AlarmReceiver extends BroadcastReceiver {

    static final String CHANNEL = "work_shift";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        int id      = intent.getIntExtra("id", 1);
        String title = intent.getStringExtra("title");
        String body  = intent.getStringExtra("body");

        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel ch = new NotificationChannel(
                CHANNEL, "Βάρδια", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Λήξη διαλείμματος και σχόλασμα");
        ch.enableVibration(true);
        ch.setLightColor(Color.GREEN);
        nm.createNotificationChannel(ch);

        PendingIntent open = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(open)
                .build();

        nm.notify(id, n);

        ctx.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
           .edit().remove("alarm_" + id).apply();
    }
}
