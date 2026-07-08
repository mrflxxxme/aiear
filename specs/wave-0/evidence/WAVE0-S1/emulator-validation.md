# Emulator validation — run-kit fixes (WAVE0-S1)

**Date:** 2026-07-08 · **Env:** headless AVD `android-34;aosp_atd;x86_64` in Docker (KVM passthrough),
image `aiear-android-emu` (cirruslabs/android-sdk:35 + JDK17 + emulator + system-image). **Not** a
physical OEM — this validates the *instrument*, not S1-GO. `needs-device` (≥2 OEM, Xiaomi mandatory)
stays open; the emulator cannot reproduce MIUI/HyperOS OEM-killer behaviour (no such system image exists).

## Why: three defects found during the first emulator sweep

The run-kit and one code path would have produced a **false FAIL or no evidence** on the founder's real
device run. Reproduced on the emulator, fixed, re-verified on the emulator.

| # | Where | Defect (observed) | Fix |
|---|---|---|---|
| 1 | `run-on-device.sh` mode A | After `:app:connectedDebugAndroidTest`, AGP 8.7.3 **uninstalls both APKs**, wiping `filesDir` incl. `heartbeat_*.log`. The `run-as` pull found 0 files → script printed `FAIL (no heartbeat log pulled)` **even though the instrumented test passed (rc=0)**. No supported AGP opt-out exists. | Score mode A from the **logcat capture** the script already streams from before the run (tag `AIEAR_S1`, survives the uninstall). `parse_heartbeat`'s `t=`-reset check still catches a kill+restart inside the synthesized file. Also: a non-zero gradle rc now forces the verdict to FAIL so a red assertion isn't masked. |
| 2 | `run-on-device.sh` mode B | `am start-foreground-service …/MicForegroundService` → **`Error: Requires permission not exported from uid`** — the service is `exported=false` (ADR-002), so shell cannot start it; capture never began. Also `dumpsys deviceidle force-idle` was a silent no-op (`Unable to go deep idle; not enabled`). | Start capture the **legal way** — tap the app's own Start button via a `uiautomator dump` + `input tap` helper (the only ADR-002 start surface), then verify a heartbeat session file actually appeared before proceeding. Add `dumpsys deviceidle enable deep` before `force-idle`. Stop via `force-stop` **after** logs are pulled. |
| 3 | `MicForegroundService.runCaptureLoop` | On a graceful stop (`stopCapture` → `job.cancel()`), the broad `catch (Throwable)` swallowed the `CancellationException` and logged a spurious **`EXC`** beat before `STOPPED` — a normal stop looked like a crash in the heartbeat log. | `coroutineContext.ensureActive()` at the top of the catch **rethrows** cancellation (normal stop → straight to `STOPPED`); genuine failures still log `EXC` (S1-AC3 preserved). |

## Re-verification on the emulator (after fixes)

| Check | Command | Result |
|---|---|---|
| Mode A (logcat scoring) | `run-on-device.sh --minutes 3` | **PASS**, exit 0 — `beats=19 max_gap=10051ms last_t=181673ms`; heartbeat synthesized from logcat after the AGP uninstall |
| Mode B (UI-tap start) | `run-on-device.sh --manual --minutes 2` | **PASS**, exit 0 — capture started via the Start button; `beats=12 max_gap=10051ms last_t=120566ms` |
| EXC fix (graceful stop) | tap Start → 25 s → tap Stop; read heartbeat file | **PASS** — tail `ALIVE, ALIVE, STOPPED`; **0** `EXC` beats |
| Fail-closed still honest (regression) | kill service mid-run (`am force-stop` @60 s of a 3-min run) | **FAIL** as required — `last_t=60259ms` early-end caught, exit 1 |
| Full instrumented survival | `./gradlew pixel8Api34DebugAndroidTest` (project GMD) | **PASS** — `MicFgsSurvivalTest` 1/1, 122 s |
| Extended endurance | 30 min screen-off + deep Doze | **PASS** — 181 beats, gap ≤10055 ms, 57.7 MB PCM (≈ nominal 32 kB/s) |

## Scope / honesty (ADR-010)

- These runs prove the **run-kit is now trustworthy and evidence-producing**, and that the FGS survives
  screen-off + forced Doze on **stock Android 14**. They do **not** close `needs-device`: OEM battery
  killers (MIUI/HyperOS Autostart, Honor) have no emulator image and remain the founder's physical run.
- Emulator artefacts are **not** committed under `device-logs/` (that dir is reserved for founder OEM
  evidence per `device-matrix-checklist.md`).
- Full local CI gate re-run green after the Kotlin change: `ktlintCheck detekt testDebugUnitTest
  assembleDebug assembleDebugAndroidTest` → 0 lint, unit 7/7, both APKs.
