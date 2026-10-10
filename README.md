<p align="center">
  <img src="docs/banner.png" alt="Work TimeSheet" width="820">
</p>

<h1 align="center">Work TimeSheet</h1>

<p align="center">
  <b>Track a single working day — when it started, what is left of it, and when you can go home.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/version-v3.0.0-f0b429?labelColor=1b2130" alt="version">
  <img src="https://img.shields.io/badge/Android-8%2B-4ade80?labelColor=1b2130" alt="android">
  <img src="https://img.shields.io/badge/dependencies-none-58b0c8?labelColor=1b2130" alt="dependencies">
  <img src="https://img.shields.io/badge/network-offline-7fb2ff?labelColor=1b2130" alt="offline">
  <img src="https://img.shields.io/badge/licence-MIT-22c55e?labelColor=1b2130" alt="licence">
</p>

---

It is a WebView shell around one HTML file, with a native layer for alarms. No account, no server, no network permission at all — everything lives on the phone.

## What it does

**Counts down the shift.** Press the start button when you clock in. The card shows the time remaining and the exact clock-out time. The countdown changes colour as the day burns down: hard red above half the shift, softer red below it, amber for the final stretch. When the time runs out the card turns green, confetti falls inside it, and everything locks until the next day.

**Handles the break.** One tap starts it, with a confirmation first so it cannot be triggered by accident. The button stays locked during the opening and closing minutes of the shift, since a break started there would run past the end of the day. When it finishes you get a notification — or, if you happen to be looking at the screen, a dialog inside the app instead. Never both.

**Notifies you when it matters.** Two alarms: end of break, end of shift. They are scheduled with `AlarmManager` the moment you press a button, so they fire with sound and wake the screen even when the app is closed. No background service, no battery drain, and they survive a reboot.

**Knows what day it is.** The day name sits at the top of the card and carries the mood of the day — Monday bleeds, Tuesday weeps, Wednesday sulks in amber, Friday goes green with fireworks. Days with a name of their own show it instead of the weekday.

**Stops you on days off.** Weekends, public holidays and your own declared leave take over the whole screen with their own artwork — popcorn on a weekend, snow at Christmas, red eggs at Easter, a beach in summer — and the shift controls lock, because there is nothing to time.

**Tracks your leave.** Add a range and a type, delete it with two taps, no editing by design. Days are counted as actual working days: weekends and public holidays inside a range do not consume leave. The screen shows how much you have taken and what is left of the yearly allowance you entered.

**Adjusts to your workplace.** Shift length from 7:00 to 9:00 in five-minute steps, break from 10 to 60 minutes or switched off entirely, toggles for the holidays that differ from one employer to another — Καθαρά Δευτέρα, Μεγάλη Παρασκευή, Αγίου Πνεύματος, 2 Ιανουαρίου — and your own logo in place of the default wordmark. A holiday you switch off becomes an ordinary working day everywhere at once, including the day counting that decides how much of your leave a range consumes.

**Looks like you.** The figure beside the day name comes in two versions, picked in settings. Everything else on the card is the same either way.

## Holiday coverage

⚠️ **The holiday list is hard-coded and currently covers late 2026 and all of 2027.**

Greek Orthodox Easter moves every year and drags half the calendar with it, so those dates cannot come from a fixed table. A new major release ships each year carrying the next year's dates. Running past the covered period breaks nothing — those days simply count as ordinary working days.

## Building

No local toolchain needed. Push to the repository and GitHub Actions produces a signed APK as a build artifact. Push a `v*` tag and it also publishes a release, with the APK attached and the matching changelog section as release notes.

To build locally it is a plain Gradle project: `gradle assembleRelease`, minSdk 26, and no dependencies whatsoever — not even AndroidX.

## Installing

Download the APK from the latest release, open it on the phone, allow installation from unknown sources. On first launch it asks for notification permission; without it the alarms are silent.

On Android 14 and later, if the screen does not wake for notifications, enable **Full screen notifications** for the app in system settings. On Samsung, Xiaomi and Huawei devices it is also worth removing the app from battery optimisation, otherwise alarms can arrive late.

## Privacy

There is no network code in the app and no internet permission in the manifest. Shift state, leave entries, settings and the chosen logo live in the app's own private storage. Nothing is collected, nothing leaves the device.

## Licence

MIT.
