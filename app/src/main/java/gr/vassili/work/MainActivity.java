package gr.vassili.work;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * Work - WebView shell γύρω από τη σελίδα της βάρδιας.
 * Τα δεδομένα ζουν στο localStorage της συσκευής, τα notifications
 * προγραμματίζονται με AlarmManager (παίζουν και με κλειστή εφαρμογή).
 */
public class MainActivity extends Activity {

    public static final String PREFS = "work_alarms";

    /** true όσο η εφαρμογή είναι μπροστά - τότε το alarm δεν βγάζει notification */
    public static volatile boolean foreground = false;
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null && getIntent().getBooleanExtra("fromAlarm", false)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        }

        getWindow().setStatusBarColor(Color.parseColor("#0b0d12"));
        getWindow().setNavigationBarColor(Color.parseColor("#0b0d12"));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0b0d12"));

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setTextZoom(100);

        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");

        setContentView(web);
        askNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        foreground = true;
        if (web != null) web.evaluateJavascript("window.__wake && window.__wake();", null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        foreground = false;
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                   != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{ Manifest.permission.POST_NOTIFICATIONS }, 1);
        }
    }

    /* ---------------- γέφυρα JavaScript -> Android ---------------- */

    class Bridge {
        @JavascriptInterface
        public void schedule(int id, String whenMillis, String title, String body) {
            long when;
            try { when = Long.parseLong(whenMillis); } catch (Exception e) { return; }
            if (when <= System.currentTimeMillis()) return;

            setAlarm(MainActivity.this, id, when, title, body);

            SharedPreferences.Editor e = getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            e.putString("alarm_" + id, when + "|" + title + "|" + body).apply();
        }

        @JavascriptInterface
        public void cancel(int id) {
            clearAlarm(MainActivity.this, id);
            getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove("alarm_" + id).apply();
        }

        @JavascriptInterface
        public boolean available() { return true; }
    }

    /* ---------------- AlarmManager ---------------- */

    static PendingIntent intentFor(Context ctx, int id, String title, String body) {
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.putExtra("id", id);
        i.putExtra("title", title);
        i.putExtra("body", body);
        return PendingIntent.getBroadcast(ctx, id, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void setAlarm(Context ctx, int id, long when, String title, String body) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = intentFor(ctx, id, title, body);

        boolean exact = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            exact = am.canScheduleExactAlarms();
        }
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, when, 60000, pi);
        }
    }

    static void clearAlarm(Context ctx, int id) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        am.cancel(intentFor(ctx, id, "", ""));
    }
}
