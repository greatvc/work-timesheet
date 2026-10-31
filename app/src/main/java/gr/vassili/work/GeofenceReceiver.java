package gr.vassili.work;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.Calendar;

/**
 * Υπενθύμιση άφιξης.
 *
 * Το λειτουργικό μάς ξυπνάει κάθε φορά που περνάμε τον κύκλο γύρω από το
 * γραφείο. Όλη η κρίση γίνεται εδώ, και μόνο από σημαίες που γράφει η
 * JavaScript: ο native κώδικας ΔΕΝ ξέρει αργίες, άδειες και πρόγραμμα, ούτε
 * πρέπει να τα ξαναϋπολογίσει. Η σελίδα του αφήνει έτοιμη τη λίστα με τις
 * επόμενες εργάσιμες, κι εδώ γίνεται απλή αναζήτηση μέσα της. Έτσι υπάρχει
 * μία μόνο αλήθεια για το τι είναι εργάσιμη.
 */
public class GeofenceReceiver extends BroadcastReceiver {

    static final String CHANNEL = "work_arrive";
    static final int    NOTIF_ID = 11;

    /** ό,τι δεν έχει οριστεί από τη σελίδα */
    static final String FALLBACK_TEXT = "Μην ξεχάσεις την έναρξη βάρδιας";
    static final int    DEF_FROM = 5 * 60;      // 05:00
    static final int    DEF_TO   = 12 * 60;     // 12:00

    @Override
    public void onReceive(Context ctx, Intent intent) {
        GeofencingEvent ev = GeofencingEvent.fromIntent(intent);
        if (ev == null || ev.hasError()) return;
        if (ev.getGeofenceTransition() != Geofence.GEOFENCE_TRANSITION_ENTER) return;

        SharedPreferences p =
                ctx.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);

        if (!"1".equals(p.getString("flag_geoOn", ""))) return;

        String today = todayIso();

        /* χτύπησε ήδη σήμερα - βγήκες για καφέ και γύρισες */
        if (today.equals(p.getString("flag_geoFired", ""))) return;

        /* έχεις ήδη πατήσει έναρξη σήμερα */
        if (today.equals(p.getString("flag_geoStarted", ""))) return;

        /* εργάσιμη για σένα; Αργία, άδεια και ρεπό έχουν ήδη αφαιρεθεί
           από τη λίστα που έγραψε η σελίδα. */
        if (!inList(p.getString("flag_geoDays", ""), today)) return;

        /* μέσα στο παράθυρο ώρας */
        Calendar c = Calendar.getInstance();
        int minutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        if (minutes < num(p, "flag_geoFrom", DEF_FROM)) return;
        if (minutes >= num(p, "flag_geoTo", DEF_TO))    return;

        String text = p.getString("flag_geoText", "");
        if (text == null || text.isEmpty()) text = FALLBACK_TEXT;

        notify(ctx, text);
        p.edit().putString("flag_geoFired", today).commit();
    }

    /* ---------------- η ειδοποίηση ---------------- */

    /**
     * Ξεχωριστό κανάλι από τα alarms της βάρδιας. Το «Σχόλασες» ανάβει οθόνη
     * και παίρνει τον έλεγχο, γιατί πρέπει να το δεις τη στιγμή που γίνεται.
     * Αυτή εδώ είναι το αντίθετο: κάθεται ήσυχα στη μπάρα μέχρι να βγάλεις το
     * κινητό απ' την τσάντα. Χωρίς full screen, χωρίς wake lock.
     */
    private void notify(Context ctx, String text) {
        NotificationManager nm =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel ch = new NotificationChannel(
                CHANNEL, "Άφιξη στο γραφείο", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("Υπενθύμιση να πατήσεις έναρξη βάρδιας όταν φτάνεις");
        ch.enableVibration(true);
        nm.createNotificationChannel(ch);

        Intent open = new Intent(ctx, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, NOTIF_ID, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new Notification.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(text)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setAutoCancel(true)       // φεύγει όταν την πατήσεις
                .setOnlyAlertOnce(true)
                .setContentIntent(pi)
                .build();

        nm.notify(NOTIF_ID, n);
    }

    /* ---------------- βοηθητικά ---------------- */

    static String todayIso() {
        Calendar c = Calendar.getInstance();
        return String.format(java.util.Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    /** η λίστα έρχεται ως "2026-11-02,2026-11-03,..." */
    static boolean inList(String list, String day) {
        if (list == null || list.isEmpty()) return false;
        int i = list.indexOf(day);
        while (i >= 0) {
            boolean left  = (i == 0) || list.charAt(i - 1) == ',';
            int end = i + day.length();
            boolean right = (end == list.length()) || list.charAt(end) == ',';
            if (left && right) return true;
            i = list.indexOf(day, i + 1);
        }
        return false;
    }

    static int num(SharedPreferences p, String key, int fallback) {
        try {
            String v = p.getString(key, "");
            if (v == null || v.isEmpty()) return fallback;
            return Integer.parseInt(v.trim());
        } catch (Exception e) { return fallback; }
    }
}
