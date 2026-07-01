# Device-matrix checklist — WAVE0-S1 (founder live-gold)

Closes `needs-device`. Run on each phone, fill the result row, drop `device-logs/<dev>/` into the PR.
**GO = S1-AC1 green (max heartbeat gap ≤ 15 000 ms over ≥60 min screen-off) on ≥2 OEM — Xiaomi MANDATORY.**

## Precondition (read first)
**Every device MUST run Android 14+ (API 34).** The build is `minSdk = 34`, so an Android 13
(or older) phone — including an older Redmi/Samsung — will **fail to install**. That is a
precondition failure, NOT a service kill; don't record it as an S1-AC1 fail. Check
`adb shell getprop ro.build.version.sdk` ≥ 34 before running.

## Per-device procedure
1. Enable Developer options + USB debugging; connect; `adb devices` shows it.
2. Install the debug build: `./gradlew :app:installDebug` (or `adb install app-debug.apk`).
   APKs need a machine with **Google-Maven egress** to build (see `evidence-gap.md` Gap 1).
3. Launch the app once, tap **Start capture**, grant mic + notification permissions.
4. **OEM battery prep (decisive for the killer test) — do NOT skip, but record what you changed:**
   - **Xiaomi/Redmi/POCO (MIUI/HyperOS):** Settings → Apps → AIEAR → **Battery saver = No restrictions**;
     **Autostart = ON**; lock the app in Recents. (This is the onboarding workaround we must validate —
     if S1 only holds *with* it, that's a real product onboarding requirement, note it.)
   - **Samsung (One UI):** Settings → Battery → Background usage limits → **Never sleeping apps += AIEAR**;
     turn **off** "Put unused apps to sleep" for it.
   - **Pixel (stock):** App info → Battery → **Unrestricted**. (Baseline — expect it to hold unmodified.)
5. Run: `./run-on-device.sh --minutes 60` (mode A, recommended) or `--manual --minutes 60`.
   The script forces Doze, holds 60 min screen-off, pulls `heartbeat_*.log`, scores max gap.
6. Record the printed `RESULT: PASS/FAIL`, max_gap, and any logcat kill lines.

## Matrix
| # | Device | OEM | Focus | Battery prep needed? | Run | max_gap (ms) | S1-AC1 | Notes |
|---|---|---|---|---|---|---|---|---|
| 1 | Pixel 8 | Google | baseline + Doze | unrestricted | ☐ | — | ☐ pass / ☐ fail | reference device |
| 2 | Redmi/POCO | **Xiaomi (MIUI)** | **OEM-killer (R-OEM)** | **autostart+unrestricted** | ☐ | — | ☐ pass / ☐ fail | **mandatory for GO** |
| 3 | Galaxy A/S | Samsung | battery optimization | never-sleeping | ☐ | — | ☐ pass / ☐ fail | |

## Interpreting results
- **GO:** ≥2 rows pass incl. Xiaomi → S1 green; F1/M1 unblocked.
- **Conditional:** Xiaomi passes only *with* battery prep → document the onboarding step (battery-unrestricted
  + autostart prompt) as a product requirement, then it counts. Update `.planning/memory/android-oem.md`.
- **NO-GO (escalate):** Xiaomi fails even with every Don't-Kill-My-App workaround → fundamental block →
  `native-spike-debugger` (agent-device + per-brand matrix); if still dead, founder call on MVP viability.

## What each AC needs from the run
- **S1-AC1** — `RESULT: PASS` (gap ≤ 15 s) over the full 60 min.
- **S1-AC2** — FGS mic-notification visible the whole time (glance at the phone after wake; the
  instrumented test also asserts `micNotificationPresent()`).
- **S1-AC3** — if Doze breaks capture, the heartbeat log still shows the `READ_ERR`/`EXC` beat and the
  parser points at `break_near_t` — interruption captured for diagnosis, never silent.
