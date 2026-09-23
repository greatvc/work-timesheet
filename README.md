# Work TimeSheet - Android APK

Tracker βάρδιας 8:30 με διάλειμμα 30', σαν native εφαρμογή.
Όλα τοπικά στη συσκευή: κανένας server, καμία σύνδεση, καμία άδεια δικτύου.

## Τι κάνει

- Ίδια οθόνη με τη web έκδοση (card, ρολόι, αντίστροφη μέτρηση, confetti στο σχόλασμα).
- Τα δεδομένα ζουν στο `localStorage` της εφαρμογής και μηδενίζουν μόνα τους με την αλλαγή ημέρας.
- **Notifications**: μόλις πατήσεις Έναρξη προγραμματίζεται ειδοποίηση για το σχόλασμα, και με το
  διάλειμμα μία ακόμα για τη λήξη του. Παίζουν κι αν η εφαρμογή είναι τελείως κλειστή, μέσω
  `AlarmManager` - χωρίς υπηρεσία στο παρασκήνιο, χωρίς κατανάλωση μπαταρίας.
- Το Reset ακυρώνει και τις δύο προγραμματισμένες ειδοποιήσεις.
- Μετά από restart του κινητού, τα alarms ξαναστήνονται (`BootReceiver`).

## Build χωρίς να εγκαταστήσεις τίποτα

1. Φτιάξε private repo στο GitHub και ανέβασε όλο τον φάκελο (το `settings.gradle` πρέπει να
   είναι στη ρίζα του repo).
2. Το GitHub Actions ξεκινάει μόνο του σε κάθε push σε `main`/`master`.
   Μπορείς και χειροκίνητα: **Actions → Build APK → Run workflow**.
3. Όταν τελειώσει (~3-5 λεπτά), κατέβασε το artifact **work-apk**. Μέσα είναι το
   `app-release.apk`.
4. Στο κινητό: άνοιξε το αρχείο, δώσε "Install unknown apps" στον browser ή στα Files.

## Υπογραφή

Το `app/debug.keystore` είναι μέσα στο repo επίτηδες, ώστε κάθε build να υπογράφεται με το ίδιο
κλειδί - έτσι οι επόμενες εκδόσεις εγκαθίστανται πάνω από την προηγούμενη χωρίς uninstall.
Επειδή το repo είναι private, δεν υπάρχει θέμα. (Για Play Store θα χρειαζόταν άλλο κλειδί,
φυλαγμένο σαν secret.)

## Άδειες που ζητάει

- `POST_NOTIFICATIONS` - ζητιέται στο πρώτο άνοιγμα (Android 13+).
- `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM` - για ειδοποίηση στο ακριβές λεπτό.
- `RECEIVE_BOOT_COMPLETED` - για να επιβιώνουν τα alarms σε restart.

Σε Xiaomi/Samsung/Huawei, αν οι ειδοποιήσεις αργούν, βγάλε την εφαρμογή από το battery
optimization (Settings → Apps → Work TimeSheet → Battery → Unrestricted).

## Αλλαγές

- Χρόνοι βάρδιας/διαλείμματος: `app/src/main/assets/index.html`, μεταβλητές `SHIFT`, `BREAK`, `CUTOFF`.
- Κείμενα ειδοποιήσεων: ίδιο αρχείο, συναρτήσεις `doStart()` και `doBreak()`.
- Όνομα εφαρμογής: `app/src/main/res/values/strings.xml`.
- Εικονίδιο: `app/src/main/res/mipmap-*/ic_launcher.png`.
