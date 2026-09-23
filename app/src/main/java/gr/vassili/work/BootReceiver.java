package gr.vassili.work;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Map;

/** Μετά από restart τα alarms χάνονται - τα ξαναστήνουμε. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        SharedPreferences p = ctx.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);

        for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
            if (!e.getKey().startsWith("alarm_")) continue;

            String[] parts = String.valueOf(e.getValue()).split("\\|", 3);
            if (parts.length < 3) continue;

            try {
                int id = Integer.parseInt(e.getKey().substring(6));
                long when = Long.parseLong(parts[0]);
                if (when > System.currentTimeMillis()) {
                    MainActivity.setAlarm(ctx, id, when, parts[1], parts[2]);
                } else {
                    p.edit().remove(e.getKey()).apply();
                }
            } catch (Exception ignored) { }
        }
    }
}
