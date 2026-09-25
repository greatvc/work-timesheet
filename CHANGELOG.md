# Changelog

All notable changes to this project are documented here. Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## 🏷️ [v1.3.0] - 25/9/26

### ✨ Added
- 🎈 Holiday takeover - every compulsory private-sector public holiday for the rest of 2026 and all of 2027 is baked into the app, minus the ones that fall on a weekend. On such a day the app opens straight into a full-screen celebration: the card dims behind a blur, a festive graphic fades in with a gold rim and shakes itself awake, and balloons drift down across the whole screen for two seconds. It stays put - nothing underneath can be tapped - and replays every time the app is reopened. Wording elsewhere is untouched; the day keeps its usual mood colour.
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
- 🎨 Flexopack banner across the top of the card and the worker icon on the left of the day name. The corporate logo ships in a dark-mode variant - its white background was stripped and the grey lettering lightened, so it sits on the dark card instead of showing up as a white box.
- 🌙 Deliberately low-key on a desk: the web card idles at 78% opacity and wakes to full on touch or hover for 12 seconds, buttons carry emoji and images instead of readable words, and nothing about the card announces what it's tracking from across the room.
- 🤖 CI build with no local toolchain: a GitHub Actions workflow produces a signed APK as an artifact on every push, using a keystore committed to the (private) repo so successive builds install over each other without an uninstall.
- 📱 Tuned for iPhone (safe-area insets, `viewport-fit=cover`, standalone home-screen mode) and for the Samsung A34 on the APK side.
