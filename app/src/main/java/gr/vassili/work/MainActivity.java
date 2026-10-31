package gr.vassili.work;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.provider.Settings;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Work - WebView shell γύρω από τη σελίδα της βάρδιας.
 * Τα δεδομένα ζουν στο localStorage της συσκευής, τα notifications
 * προγραμματίζονται με AlarmManager (παίζουν και με κλειστή εφαρμογή).
 */
public class MainActivity extends Activity {

    public static final String PREFS = "work_alarms";
    private static final int REQ_LOGO   = 7;
    private static final int REQ_GEO    = 8;   // τοποθεσία όσο η εφαρμογή είναι ανοιχτή
    private static final int REQ_GEO_BG = 9;   // τοποθεσία στο παρασκήνιο
    private static final int GEO_PI     = 1001;

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
        if (web != null) {
            web.evaluateJavascript("window.__wake && window.__wake();", null);
            /* μπορεί να γύρισε από τις ρυθμίσεις του Android αφού άλλαξε
               την άδεια τοποθεσίας - η σελίδα πρέπει να το δει */
            web.evaluateJavascript("window.__geoPerm && window.__geoPerm();", null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req != REQ_GEO && req != REQ_GEO_BG) return;
        applyGeofence(this);
        if (web != null) web.evaluateJavascript("window.__geoPerm && window.__geoPerm();", null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        foreground = false;
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_LOGO || res != RESULT_OK || data == null || data.getData() == null) return;

        try {
            InputStream in = getContentResolver().openInputStream(data.getData());
            Bitmap bm = BitmapFactory.decodeStream(in);
            if (in != null) in.close();
            if (bm == null) return;

            /* σμίκρυνση πριν την αποθήκευση - το base64 ζει στις SharedPreferences */
            int w = bm.getWidth(), h = bm.getHeight();
            float sc = Math.min(1f, 640f / (float) w);
            Bitmap small = Bitmap.createScaledBitmap(bm, Math.round(w * sc), Math.round(h * sc), true);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            small.compress(Bitmap.CompressFormat.PNG, 90, out);
            String b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);

            getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("flag_logo_new", "data:image/png;base64," + b64).commit();

            if (web != null) web.evaluateJavascript("window.__logoReady && window.__logoReady();", null);
        } catch (Exception ignored) { }
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

        /* σημαίες που επιβιώνουν ακόμα κι αν η εφαρμογή κλείσει αμέσως μετά */
        @JavascriptInterface
        public void setFlag(String key, String value) {
            getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString("flag_" + key, value == null ? "" : value).commit();
        }

        /* επιλογή λογότυπου - SAF, χωρίς καμία άδεια */
        @JavascriptInterface
        public void pickLogo() {
            runOnUiThread(new Runnable() {
                public void run() {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("image/*");
                    try { startActivityForResult(i, REQ_LOGO); } catch (Exception ignored) { }
                }
            });
        }

        @JavascriptInterface
        public String getFlag(String key) {
            return getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("flag_" + key, "");
        }

        /* ---------------- υπενθύμιση άφιξης ---------------- */

        /** "none" καμία άδεια, "fore" μόνο με ανοιχτή εφαρμογή, "back" πάντα */
        @JavascriptInterface
        public String geoPerm() {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) return "none";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                       != PackageManager.PERMISSION_GRANTED) return "fore";
            return "back";
        }

        /**
         * Ζητάει το επόμενο βήμα της άδειας. Από Android 10 και πάνω το
         * σύστημα ΔΕΝ δέχεται να ζητηθούν μαζί η τοποθεσία και η τοποθεσία
         * στο παρασκήνιο: πρώτα η μία, μετά η άλλη.
         */
        @JavascriptInterface
        public void geoAsk() {
            runOnUiThread(new Runnable() { public void run() {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION }, REQ_GEO);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                           != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION }, REQ_GEO_BG);
                }
            }});
        }

        /** ανοίγει τις ρυθμίσεις της εφαρμογής, εκεί που δίνεται το «Να επιτρέπεται πάντα» */
        @JavascriptInterface
        public void geoSettings() {
            runOnUiThread(new Runnable() { public void run() {
                try {
                    Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception ignored) { }
            }});
        }

        /** παίρνει στίγμα εδώ και τώρα και το κρατάει ως γραφείο */
        @JavascriptInterface
        public void geoHere() {
            runOnUiThread(new Runnable() { public void run() {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) { toJs("perm"); return; }
                try {
                    LocationServices.getFusedLocationProviderClient(MainActivity.this)
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY,
                                            new CancellationTokenSource().getToken())
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            public void onSuccess(Location loc) {
                                if (loc == null) { toJs("fail"); return; }
                                getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                                    .putString("flag_geoLat", String.valueOf(loc.getLatitude()))
                                    .putString("flag_geoLng", String.valueOf(loc.getLongitude()))
                                    .putString("flag_geoAcc", String.valueOf(Math.round(loc.getAccuracy())))
                                    .putString("flag_geoAt",  String.valueOf(System.currentTimeMillis()))
                                    .commit();
                                applyGeofence(MainActivity.this);
                                toJs("ok");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            public void onFailure(Exception e) { toJs("fail"); }
                        });
                } catch (SecurityException e) { toJs("perm"); }
            }});
        }

        /** σβήνει το γραφείο και τον κύκλο γύρω του */
        @JavascriptInterface
        public void geoClear() {
            getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove("flag_geoLat").remove("flag_geoLng")
                .remove("flag_geoAcc").remove("flag_geoAt")
                .remove("flag_geoFired").commit();
            try {
                LocationServices.getGeofencingClient(MainActivity.this)
                    .removeGeofences(geoIntent(MainActivity.this));
            } catch (Exception ignored) { }
        }

        /** ξαναστήνει τον κύκλο όταν αλλάξει διακόπτης ή απόσταση */
        @JavascriptInterface
        public void geoApply() { applyGeofence(MainActivity.this); }
    }

    private void toJs(final String what) {
        runOnUiThread(new Runnable() { public void run() {
            if (web != null) web.evaluateJavascript(
                    "window.__geoReady && window.__geoReady('" + what + "');", null);
        }});
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

    /* ---------------- geofence ---------------- */

    /**
     * Το PendingIntent του geofence πρέπει να είναι MUTABLE από Android 12 και
     * πάνω: το σύστημα του προσθέτει το ίδιο τα στοιχεία του περάσματος. Με
     * IMMUTABLE ο receiver θα ξυπνούσε με άδεια χέρια.
     */
    static PendingIntent geoIntent(Context ctx) {
        Intent i = new Intent(ctx, GeofenceReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_MUTABLE;
        return PendingIntent.getBroadcast(ctx, GEO_PI, i, flags);
    }

    /**
     * Ξαναστήνει τον κύκλο από το μηδέν. Καλείται όποτε αλλάξει κάτι: νέο
     * γραφείο, άλλη απόσταση, διακόπτης, άδεια, ή restart της συσκευής.
     */
    static void applyGeofence(Context ctx) {
        GeofencingClient gc = LocationServices.getGeofencingClient(ctx);
        try { gc.removeGeofences(geoIntent(ctx)); } catch (Exception ignored) { }

        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!"1".equals(p.getString("flag_geoOn", ""))) return;

        double lat, lng;
        try {
            lat = Double.parseDouble(p.getString("flag_geoLat", ""));
            lng = Double.parseDouble(p.getString("flag_geoLng", ""));
        } catch (Exception e) { return; }

        float radius = 500f;
        try {
            String r = p.getString("flag_geoRad", "");
            if (r != null && !r.isEmpty()) radius = Float.parseFloat(r);
        } catch (Exception ignored) { }

        if (ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        Geofence g = new Geofence.Builder()
                .setRequestId("office")
                .setCircularRegion(lat, lng, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        /* setInitialTrigger(0): αν τη στιγμή που στήνεται ο κύκλος είσαι ήδη
           μέσα του - π.χ. πατάς «Είμαι εδώ τώρα» καθισμένος στο γραφείο - δεν
           θέλουμε να χτυπήσει αμέσως. Μόνο σε πραγματικό πέρασμα. */
        GeofencingRequest req = new GeofencingRequest.Builder()
                .setInitialTrigger(0)
                .addGeofence(g)
                .build();

        try { gc.addGeofences(req, geoIntent(ctx)); } catch (SecurityException ignored) { }
    }
}
