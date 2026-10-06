# Changelog

All notable changes to this project are documented here. Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## 🏷️ [v3.0.2] - 6/10/26

### 🛠️ Changed
- 🔓 Settings now lock only while the shift is actually running. Once you have clocked out they open again - there is no reason to refuse a change at six in the evening when it only takes effect tomorrow morning.
- 🧷 Each shift remembers the length it started with. Without that, changing the duration after clocking out would recalculate the day that just finished and bring it back to life with time left on it. Today keeps the settings it began with; new ones apply from the next day. The same goes for the break.
- 🎚️ The break switch moved up beside its section heading, so the row that said "Έχω διάλειμμα" is gone and only the duration remains underneath.

## 🏷️ [v3.0.1] - 5/10/26

### 🛡️ Fixed
- 💥 Clocking out wrecked the layout. The card borrowed a class name, `pop`, that already belonged to the in-app message overlay - so at the moment the shift ended the card itself took on the overlay's styling: pinned to the left edge, stretched to full height, its contents strung out in one row that ran off the screen. The glow animation now has a name of its own. The break was never affected, since that class is only added when the shift ends.

### 🛠️ Changed
- ⏱️ Shift length now starts at 7:00 instead of 8:00, still up to 9:00 in five-minute steps, default unchanged at 8:30. The countdown's colour thresholds follow automatically, since they are percentages of the shift rather than fixed times.

## 🏷️ [v3.0.0] - 30/9/26 &nbsp;&nbsp;&nbsp;![Major Release](https://img.shields.io/badge/Major-Release-22c55e?labelColor=166534)

### ✨ Added
- ⚙️ Settings. A gear sits top-right of the screen, outside the card, and opens everything that used to be hard-coded. Shift length runs from 8:00 to 9:00 in five-minute steps, the break from 10 to 60 minutes one minute at a time, and every arrow repeats while held. Nothing is applied as you tap: changes live in a draft until you press Save, and the ✕ discards them.
- 🔕 The break can be switched off entirely. When it is, its button leaves the card altogether rather than sitting there greyed out, its notification is never scheduled and its legend never appears.
- 🎛️ Holidays that differ from one workplace to another became switches - Μεγάλη Παρασκευή and Αγίου Πνεύματος on by default, 2 Ιανουαρίου off, since that one is a retail-only closure for stocktaking.
- 🖼️ The banner is yours to choose. The logo comes from the phone's own file chooser - no permission is asked, because the chooser runs outside the app - and is scaled down before being stored on the device. A reset button returns to the built-in wordmark, which is now the default: the app no longer ships wearing one company's branding.

### 🛠️ Changed
- 🎨 The countdown's colour thresholds became proportional instead of fixed. Hard red above half the shift, soft red below it, amber for the final eighth - for an 8:30 day that lands on 4:15 and 41 minutes, almost exactly the old numbers, but now correct for any shift length.
- ⏱️ The dead zone around the break is derived rather than configured: always the break plus two minutes, so the button unlocks and locks itself whatever duration is set.
- 🔒 Settings lock while a shift is running. Changing the length mid-shift would leave an alarm scheduled for a time that no longer exists.

### 🛡️ Fixed
- 🎄 A holiday now outranks the weekend. The list had been built with weekend holidays stripped out, because at the time all we wanted to know was which days were off - so Christmas 2027, falling on a Saturday, showed the couch and popcorn. Every one of them is back with its proper theme: 26/12/2026, Πρωτομαγιά and Κυριακή του Πάσχα 2027, Δεκαπενταύγουστος, and both days of Christmas 2027.

## 🏷️ [v2.1.0] - 29/9/26 &nbsp;&nbsp;&nbsp;![Release](https://img.shields.io/badge/Release-22c55e)

### ✨ Added
- 📛 Days that have a name of their own now carry it in the header instead of the plain weekday - Πρωτοχρονιά, Φώτα, Τσικνοπέμπτη, Καθαρά Δευτέρα, the whole of Holy Week from Μ. Δευτέρα to Μ. Σάββατο, Κυριακή and Δευτέρα του Πάσχα, Αγίου Πνεύματος, Πρωτομαγιά, Δεκαπενταύγουστος and Χριστούγεννα, for 2026 and 2027. Long names shrink themselves to fit: the app measures the free space next to the header icon and steps the type down until it clears, so "Δεκαπενταύγουστος" sits on one line as comfortably as "Τρίτη".

### 🛡️ Fixed
- 🎨 A day off no longer wears a working day's mood. On any holiday or declared leave the day name now glows a bright, thick green, and the weekday effects are suppressed - no blood dripping on a Monday you are not working, no tears on a Tuesday, no Friday fireworks competing with the celebration already on screen.

## 🏷️ [v2.0.0] - 27/9/26 &nbsp;&nbsp;&nbsp;![Major Release](https://img.shields.io/badge/Major-Release-22c55e?labelColor=166534)

### ✨ Added
- 🌴 Personal leave. A round button on the card opens a screen where leave is added and deleted - no editing, by design. Each entry is a from/to range plus a type (plain, summer, Christmas, Easter), and a single day is just the same date twice. Weekends can't be picked as endpoints, overlapping ranges are refused, and deletion takes two taps like the reset button.
- 🧮 Days are counted as actual working days: weekends and public holidays inside a range don't consume leave. Two figures sit under the list - the total available for the year and what's left after everything declared - and the yearly allowance is entered with up/down arrows from 1 to 30, so no keyboard and nothing to validate. Leave taken in January is charged against last year's balance while any of it remains, which is how the carry-over actually works.
- 🌙 A leave day takes over the screen like a holiday does, with its own graphic per type; the plain one drops moons and stars over a bedroom scene. Leave outranks everything - holidays and weekends included.
- 🎈 Holiday takeover - every compulsory private-sector public holiday for the rest of 2026 and all of 2027 is baked into the app, minus the ones that fall on a weekend. On such a day the app opens straight into a full-screen celebration: the card dims behind a blur, a festive graphic fades in with a gold rim and shakes itself awake, and balloons drift down across the whole screen for two seconds. It stays put - nothing underneath can be tapped - and replays every time the app is reopened. Wording elsewhere is untouched; the day keeps its usual mood colour.
- 💾 Shift state is now written through the Android bridge into SharedPreferences with a synchronous commit, with localStorage kept as a fallback. The WebView flushes localStorage to disk lazily, so pressing Έναρξη and leaving the app a second later could lose the start time - that window is closed.
- 🍿 Weekend takeover - Saturday and Sunday now get the same treatment as a holiday, with a couch-and-popcorn graphic and popcorn tumbling down the screen instead of balloons. The card sits dimmed behind it, so there is nothing to press on a day off. Both share one routine, ready to take Christmas, Easter and personal leave later.
- 💬 In-app messages when the app is in the foreground. If the break or the shift ends while you are actually looking at the screen, no system notification fires at all - instead the card shows a proper dialog that waits for an OK. The pending state is stored, so leaving the app and coming back brings the same dialog straight back rather than losing it. Notifications still behave exactly as before whenever the app is closed, backgrounded, or the screen is locked.

## 🏷️ [v1.1.0] - 25/9/26 &nbsp;&nbsp;&nbsp;![Release](https://img.shields.io/badge/Release-22c55e)

### 🛠️ Changed
- 🎭 Innovative day display according to mood - the day name is now the centrepiece of the card, bigger and bolder, and it carries the temperament of the day: Monday bleeds, Tuesday weeps, Wednesday sulks in amber, Thursday brightens, and Friday goes full green with fireworks. The clock was dropped entirely; the phone already shows it in the status bar.
- 🎨 The remaining-shift counter now changes colour as the day burns down: hard red with a red glow while there are more than 4 hours left, a softer red below that, and amber in the final 40 minutes. Hours are zero-padded, so it reads 08:30 rather than 8:30.
- ☕ Break shortened from 30 to 25 minutes, and the dead zone at each end of the shift adjusted from 31 to 27 minutes accordingly - the extra 5 minutes cover walking back and badging in, so a break taken at the limit no longer runs past the clock-out.

## 🏷️ [v1.0.0] - 23/9/26 &nbsp;&nbsp;&nbsp;![Initial Release](https://img.shields.io/badge/Initial-Release-22c55e?labelColor=124fde)

### ✨ Added
- 💼 First working version of **Work TimeSheet** - a discreet shift tracker for an 8h30 workday, shipping as two builds from one codebase: a PHP page (`index.php`, state in `data/shift.json`) and a native Android APK (WebView shell, state in `localStorage`, no server and no network permission at all).
- ⏱️ Live clock in hours and minutes with a blinking colon, countdown of the remaining shift in hours and minutes, and the calculated clock-out time - press at 09:00 and it reads 17:30.
- 🍴 30-minute break with its own countdown in minutes and seconds inside a tinted panel, plus a confirmation overlay ("Να ξεκινήσω διάλειμμα;") rendered in-card with a blurred backdrop instead of the browser's native `confirm()`. Once taken, it leaves a bulleted legend showing the exact window, e.g. "Διάλειμμα 13:50 - 14:20".
- 🚫 Break button stays disabled during the first 31 minutes of the shift and the last 31 minutes before clocking out - a 30-minute break doesn't fit in either window. Enforced client-side and again server-side in the PHP build, and it unlocks live without a refresh.
- 🎉 Celebration on clock-out: confetti clipped inside the card (not the full screen), a pulsing green border glow, and a glowing "Σχόλασες 🎉" that replays on every refresh for as long as it's the same day.
- ♻️ Two-step reset: the first press arms the button - it turns deep red and a thin bar drains across the bottom over 10 seconds. A second press within that window resets; otherwise it disarms itself. Prevents the mis-tap that would wipe a running shift.
- 🔔 **Android notifications** scheduled through `AlarmManager` at the moment a button is pressed, firing with sound and heads-up banner even when the app is fully closed - one for the end of the break, one for clocking out. No background service, no battery drain. Alarms are re-registered after a device reboot via `BootReceiver`, and Reset cancels both.
- 🧪 Hidden test: press and hold the card header for 1.5 seconds to schedule a real notification 15 seconds out, so the whole chain can be verified without waiting a full shift.
- 🎨 A company banner across the top of the card and the worker icon on the left of the day name. The logo ships in a dark-mode variant - its white background stripped and the grey lettering lightened, so it sits on the dark card instead of showing up as a white box.
- 🌙 Deliberately low-key on a desk: the web card idles at 78% opacity and wakes to full on touch or hover for 12 seconds, buttons carry emoji and images instead of readable words, and nothing about the card announces what it's tracking from across the room.
- 🤖 CI build with no local toolchain: a GitHub Actions workflow produces a signed APK as an artifact on every push, using a committed debug keystore so successive builds install over each other without an uninstall.
- 📱 Tuned for iPhone (safe-area insets, `viewport-fit=cover`, standalone home-screen mode) and for the Samsung A34 on the APK side.
