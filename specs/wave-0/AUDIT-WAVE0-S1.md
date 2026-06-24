---
audit: WAVE0-S1
verdict: pass-with-followups
revision: 2
lenses: { code: pass, security: pass, tests: pass, adr: pass, compliance: n-a, device: pass, live_gold: pass, cost: pass }
followups:
  - "WAVE0-S1-followup-founder-device-run: founder runs run-on-device.sh on >=2 OEM incl. Xiaomi (mandatory), 60 min screen-off + Doze; attach device-logs/ + tick device-matrix-checklist.md. (closes needs-device — the actual S1-GO gate, still unmet)"
  - "WAVE0-S1-followup-greenbuild-rerun: re-run full self-run (ktlintCheck+detekt+testDebugUnitTest+assembleDebug+assembleDebugAndroidTest) on a runner with Google-Maven egress; attach green selftest. (closes needs-google-maven-egress)"
  - "WAVE0-S1-followup-ratify-pins: founder/architect ratify minSdk=34 / targetSdk=34 / compileSdk=35, the version-catalog pins, and the spike branch name on gate approval."
  - "WAVE0-S1-followup-fix-stale-kdoc (minor, non-blocking): MicForegroundService class KDoc (:32-33) + runCaptureLoop KDoc (:87-89) still describe START_STICKY restart + ALIVE-only beats; the code is now START_NOT_STICKY with an ALIVE/STALL branch. Same misleading-comment class as the resolved major-1, relocated to the class header. Tidy before F1 graduation."
followups_resolved:
  - "WAVE0-S1-BLOCKER-harden-survival-test — RESOLVED (rev2): all 3 false-PASS vectors fixed + mechanically re-verified (harden-rerun.txt). The instrument now fails closed."
  - "WAVE0-S1-followup-fix-sticky-semantics — RESOLVED (rev2): START_NOT_STICKY, comment corrected at onStartCommand."
  - "WAVE0-S1-followup-strengthen-AC2-assert — RESOLVED (rev2): asserts OUR channel, sampled during screen-off."
findings_by_severity: { info: 4, minor: 2, major: 0, critical: 0 }
---

# AUDIT — WAVE0-S1 (mic-FGS survives screen-off >=60 min)

**Owner:** architect (phase-close, 8 lenses) · **Date:** 2026-06-24 · **Revision:** 2 (post-revision-loop re-audit) · **Canon:** [04-POST-AUDIT.md](../../.planning/agent-handbook/04-POST-AUDIT.md), ADR-002, ADR-010
**Gate inputs:** spec `S1-mic-fgs-screenoff.md`; hardened code under `app/` (working tree == HEAD `3ea2710`, re-read in full); evidence bundle `evidence/WAVE0-S1/` incl. `harden-rerun.txt`; reviewer/verifier handoffs.

> **Bottom line (rev2):** The rev1 audit found the device harness could report a FALSE GREEN — pass through the exact OEM kill the spike exists to catch (4 majors). The revision loop fixed all four, plus the two addressed minors, and I re-verified each against the original finding by reading the changed files (not the coordinator's summary). The instrument is now genuinely trustworthy: it **fails closed** across every adversarial input and can no longer pass on mere service liveness or a blended cross-session delta. `tests` and `device` lenses move **fail → pass**; the BLOCKER-harden item is **resolved**. The verdict stays **`pass-with-followups`** — NOT because anything is broken, but because the **core S1-AC1..AC3 on-device proof is still not obtained** and remains a declared, accepted gap (`needs-device`) gated on the founder device run. The two evidence_gaps (`needs-device`, `needs-google-maven-egress`) are open **by design**, exactly as ADR-010 allows; the question the coordinator posed — "is the instrument that will close `needs-device` now trustworthy?" — is answered **yes**.

> **Authority note:** this re-audit verdict is the architect's technical judgment only. It does NOT constitute founder acceptance of the two evidence_gaps or approval of the gate — that requires the founder's own decision at the gate. The coordinator's relay carries no founder authority.

---

## Revision-2 verification of the rev1 findings (read, not taken on trust)

| rev1 finding | sev | fix claimed | verified at | resolved? |
|---|---|---|---|---|
| major-1 START_STICKY ineffective + crash path | major | START_NOT_STICKY + corrected comment | `MicForegroundService.kt:56-61` | ✅ yes |
| major-2 liveness≠audio (muted-mic false-PASS) | major | STALL beat on 0 bytes + byte-floor assertion | `MicForegroundService.kt:119,126,136-143`; `MicFgsSurvivalTest.kt:123-130`; `HeartbeatLogger.kt:43` | ✅ yes |
| major-3 cross-session merge masks kill | major | per-session gap fn + size==1 + early-end | `HeartbeatLogger.kt:86-93`; `MicFgsSurvivalTest.kt:84-118`; unit test `HeartbeatLoggerTest.kt:48-58` | ✅ yes |
| major-4 manual script fails open | major | parser + scoring fail-closed | `run-on-device.sh:53-77,142-159` | ✅ yes |
| minor AC2 assertion too weak | minor | OUR channel + during-screen-off sampling | `MicFgsSurvivalTest.kt:72-76,132-137,182-183` | ✅ yes |
| minor service channel not guaranteed | minor | self-`ensureChannel` | `MicForegroundService.kt:68-69` | ✅ yes |
| minor minSdk precondition absent | minor | added to checklist | `device-matrix-checklist.md:6-11` | ✅ yes |

Cross-checked against the verifier's mechanical re-run (`harden-rerun.txt`): pure seam now **7/7** incl. the new anti-blend case (`sessionA=[0,10k,50k]`, `sessionB=[0,10k,20k]` → reports **40 000 ms**, not a blended 30 000); the `run-on-device.sh` fail-closed matrix shows GOOD→exit 0 and GAP/RESET/INSUFFICIENT/EMPTY/EARLY-END→exit 1. The script logic confirms it independently (see Lens 6).

**One new minor introduced by the fix** (flagged honestly, non-blocking): the class-level KDoc still describes the old behaviour — see Lens 1.

---

## Lens 1 — code-review · PASS (1 minor)

The service remains idiomatic and the FGS ordering is still correct: `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_MICROPHONE)` is called **before** `AudioRecord` is touched (`MicForegroundService.kt:72-77`), matching the manifest type; channel is now self-ensured first (`:68-69`). Coroutine cancellation honors `isActive`/`ensureActive()` (`:121,:148`); `finally` releases the recorder + emits `STOPPED` (`:156-162`); broad `catch(Throwable)` → `EXC` beat preserves never-silent (S1-AC3). The rev1 **major** is resolved: `onStartCommand` now returns `START_NOT_STICKY` (`:61`) with an accurate comment (`:56-60`) — a kill is terminal and surfaces as the log ending early, and the null-intent FGS-did-not-start crash path is gone (the `else` branch is now only reachable from an explicit malformed intent, never from sticky redelivery). The STALL/ALIVE split (`:136-143`) is clean and correctly resets `bytesThisInterval` per interval (`:143`).

- **[minor] Stale class KDoc (doc-correctness regression from the fix).** The class header still reads "START_STICKY asks the OS to restart after a kill — a restart shows up as a heartbeat gap" (`:32-33`), and `runCaptureLoop`'s KDoc says it "beats ALIVE every HEARTBEAT_PERIOD_MS" (`:87-89`) without the new STALL branch. The code is now START_NOT_STICKY with an ALIVE/STALL split. This is the *same* misleading-comment class as the resolved major-1, just relocated to the header — the `onStartCommand` inline comment is correct, but the next reader hits the stale KDoc first. Non-blocking; followup `fix-stale-kdoc`.
- **[info]** Buffer math, PCM16/mono/16 kHz, per-session file naming, reentrancy guard (`:65`), `@SuppressLint("MissingPermission")` (permission gated in `MainActivity`) all clean.

## Lens 2 — security · PASS (unchanged)

No security-relevant change in the revision; re-confirmed. Permissions remain the minimal FGS set (`AndroidManifest.xml:5-13`): `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS`; no `INTERNET`/storage/location. Audio + heartbeat stay app-private (`filesDir`); `allowBackup="false"`; service `exported="false"`; no secrets, no network code anywhere in `app/`. Dedicated reviewer-security pass still not warranted for this surface. (Same F1 note carries: PCM retention/encryption + ФЗ-152 posture to be decided when this seam reaches the cloud.)

## Lens 3 — test-adequacy · PASS (was FAIL in rev1)

Both rev1 holes are closed and the EARS↔test mapping is now meaningful end-to-end:

- **S1-AC1 now asserts AUDIO continuity, not just service liveness** (resolves rev1 major-2). The service beats `STALL` instead of `ALIVE` when 0 bytes were captured in an interval (`MicForegroundService.kt:136-142`), and `MicFgsSurvivalTest` asserts the cumulative PCM `bytes=` counter grew past a conservative floor — `readMaxBytes()` (`:147-150`) vs `floorBytes = lastT/1000 * 32000 * 25/100` (`:124`), all `Long` arithmetic (no Int overflow; ~28.8 MB floor over a 60-min run). A muted-but-alive mic (~0 bytes, all STALL) now **fails** S1-AC1 even with perfect heartbeat continuity. Good.
- **S1-AC2 assertion tightened** (resolves rev1 minor): `micNotificationPresent()` now matches **our** channel only (`contains(NotificationHelper.CHANNEL_ID)`, `:182-183`; the package-name fallback is gone) and is sampled **during** the screen-off hold (`:72-76`), asserted at `:133-137` — not a post-wake glance.
- **Pure seam strengthened to 7/7**, incl. `maxGapWithinSessions_reportsWorstInternalGap_notCrossSessionBlend` (`HeartbeatLoggerTest.kt:48-58`) which pins that a kill+restart can no longer hide behind a blended delta, and `maxGapWithinSessions_emptyIsZero`. Format/maxGap cases retained.
- The instrumented test still **fails closed**: `MIN_EXPECTED_BEATS` (`:89`), `sessions.size == 1` (`:95-99`), early-end (`:104-108`), per-session gap (`:111-118`), byte-floor (`:125-130`), notification (`:133-137`) — six independent assertions, each with a human-pointable failure message. It cannot pass silently and can no longer pass on the wrong evidence.

## Lens 4 — ADR-conformance · PASS (unchanged, reinforced)

- **ADR-002 fully honored**, now *more* cleanly: forbidden paths remain provably absent (no BOOT_COMPLETED receiver, no AccessibilityService, no silent-audio — `AndroidManifest.xml:15-21` + grep), only foreground start surface, FGS type `microphone` matches manifest↔runtime. The START_NOT_STICKY change keeps the service strictly foreground-started — no auto-relaunch path that could later be argued into a banned background-start pattern. Good for conformance.
- **ADR-010 honored**: self-run + independent verifier re-run + explicit declared gaps; no green-by-assertion; the false-PASS vectors that would have *undermined* ADR-010's "evidence means what it claims" are removed.
- No ADR violated; no new ADR required. (If S1 holds on Xiaomi only *with* battery-unrestricted + autostart, that onboarding requirement may warrant a small ADR at F1 — flagged, not required now.)

## Lens 5 — compliance · N/A (unchanged, with reasoning)

The spike still never transmits audio (no network code), writes only to app-private `filesDir`, processes nothing. ФЗ-152/242 and 54-ФЗ are not applicable to this spike by reasoned exclusion; they go live at F1 (first audio to RF cloud) and must be audited there. Stated explicitly, not by omission.

## Lens 6 — device-reliability · PASS (was FAIL in rev1; THE crux, re-examined hard)

The instrument that will produce the on-device proof is now sound. I re-read the script and test logic adversarially rather than trusting the rerun log:

- **Cross-session kill can no longer hide** (resolves major-3). `readSessions()` maps each `heartbeat_<session>.log` to its own sorted list with **no cross-file merge** (`MicFgsSurvivalTest.kt:141-144`); `maxGapWithinSessions` takes the worst *within-session* gap (`HeartbeatLogger.kt:86-93`); and `sessions.size == 1` treats any 2nd session file as a kill (`:95-99`). Under START_NOT_STICKY the common kill yields exactly one truncated file, caught by the **early-end** assertion (`:104-108`, `lastT >= totalMs - 45 000`). Three independent guards — belt and suspenders.
- **Manual script fails CLOSED** (resolves major-4). `parse_heartbeat` returns/exits non-zero on empty (`run-on-device.sh:56`), `<2` beats (`:65`), backwards-`t=` reset within a file (`:60,:68`), gap>limit (`:69`), and ended-early (`:70-71`). The scoring block exits non-zero on **0 files** (`:148-150`) and on **>1 file** (`:151-154`); the single-file path propagates the parser RC (`:156`) and `exit $RC` (`:159`). The previously-fatal "no log pulled → exit 0" path is gone. Verified against `harden-rerun.txt` RE-RUN 2 (GOOD→0; GAP/RESET/INSUFFICIENT/EMPTY/EARLY-END→1) — and the logic confirms it independently. Mode A additionally re-scores the pulled logs after the gradle run, so the script-level fail-closed guard backstops the instrumented assertions.
- **Muted-mic in Doze can no longer false-PASS** (resolves major-2) — see Lens 3; this is as much a device-reliability fix as a test fix, since it's the throttled-mic OEM behaviour that the byte-floor now catches.
- **minSdk precondition documented** (resolves minor): `device-matrix-checklist.md:6-11` warns every device must be API 34+ and to check `getprop ro.build.version.sdk`, so an Android-13 Redmi install-fail isn't mis-recorded as an S1-AC1 kill.
- **Xiaomi-mandatory GO criterion preserved** verbatim: spec Go/No-Go, checklist row 2 ("**mandatory for GO**"), and the gap doc all keep "AC1 green on >=2 OEM incl. Xiaomi." The MIUI battery-prep workaround + the "conditional GO if only-with-prep → document onboarding step" path are intact (`device-matrix-checklist.md:17-20,36-40`).
- **Honest ceiling unchanged:** the real >=60-min on-device run across the OEM matrix is still **impossible in this sandbox** (no SDK/KVM/device; Google-Maven 403 authoritative) and remains the declared `needs-device` gap. The lens now PASSES because the *instrument* is trustworthy; **executing it is the founder's followup and the actual S1-GO gate.** Passing this lens does NOT mean S1 is proven on hardware — it means the kit that will prove it won't lie.

## Lens 7 — cost-audit · PASS (qualitative, unchanged)

No token meter here. The revision was a tight, well-scoped hardening pass (Opus-heavy roles on the #1-risk native spike, consistent with model-routing/ADR-008 — justified, not disproportionate). The extra revision-loop spend bought the removal of four false-PASS vectors that would otherwise have wasted a real device session — a good trade. No cap concern; memory-curator to attach phase token metrics if available.

## Lens 8 — live-gold/evidence · PASS (blocking lens, ADR-010; unchanged + reinforced)

- **Every closed claim is still evidence-backed and independently re-verified** (pure seam 7/7 by the verifier in a clean room; wrapper/catalog wiring captured). The hardening *strengthened* this lens: the device harness — the very thing that will generate the future `needs-device` evidence — no longer has a path to a dishonest green, which is precisely what ADR-010's "evidence means what it claims" requires.
- **Both gaps remain legitimately declared and open by design**: `needs-google-maven-egress` (proxy 403 on Google Maven + every mirror — authoritative) and `needs-device` (real OEM run requires hardware/FTL). Per `/root/.ccr/README.md`, the 403 is reported, not routed around.
- **The verdict remains honest that the CORE proof is absent.** `harden-rerun.txt` lines 51-53 and the verify handoff both state the >=60-min on-device run is still deferred and "the harness that produces that proof is now trustworthy; running it remains the founder's followup." No fake green. ADR-010's "explicit accepted gap" exception applies — contingent on the founder actually accepting it at the gate.

---

## Findings by severity (rev2)

- **critical:** none.
- **major:** none (rev1's 4 majors all resolved + re-verified).
- **minor (2):** (1) stale class/loop KDoc in `MicForegroundService` describing the pre-fix START_STICKY + ALIVE-only behaviour (`:32-33,:87-89`); (2) `device-matrix-checklist.md:44-45` "glance after wake" prose for AC2 is now slightly behind the during-screen-off assertion (cosmetic; fold into the same KDoc/doc tidy).
- **info (4):** PCM retention/encryption deferred to F1; release `isMinifyEnabled=false` with proguard declared (harmless for spike); `run-as` debug-build dependency acceptable; STALL/ALIVE byte-floor of 25% is a deliberate, documented survival-bar (STT-quality is an F1/F2 concern, not S1).

## Followups (rev2)

**Resolved this revision (drop from the open set):**
- ✅ **WAVE0-S1-BLOCKER-harden-survival-test** — all 3 false-PASS vectors fixed + mechanically re-verified; instrument fails closed.
- ✅ **WAVE0-S1-followup-fix-sticky-semantics** — START_NOT_STICKY + corrected inline comment.
- ✅ **WAVE0-S1-followup-strengthen-AC2-assert** — OUR-channel match, sampled during screen-off.

**Still open (carry to the gate / OPEN-QUESTIONS):**
1. **WAVE0-S1-followup-founder-device-run** — founder runs `run-on-device.sh` on **>=2 OEM incl. Xiaomi (mandatory)**, 60 min screen-off + forced Doze; attach `device-logs/` + tick `device-matrix-checklist.md`. Closes `needs-device`. **This is the actual S1-GO gate and is still unmet.**
2. **WAVE0-S1-followup-greenbuild-rerun** — full `ktlintCheck+detekt+testDebugUnitTest+assembleDebug+assembleDebugAndroidTest` on a runner with Google-Maven egress; attach the green `selftest`. Closes `needs-google-maven-egress`.
3. **WAVE0-S1-followup-ratify-pins** — founder/architect ratify `minSdk=34` / `targetSdk=34` / `compileSdk=35`, the version-catalog pins, and the spike branch name at the gate.
4. **WAVE0-S1-followup-fix-stale-kdoc** (minor, non-blocking) — refresh the `MicForegroundService` class/loop KDoc + the checklist AC2 prose to match START_NOT_STICKY + STALL. Tidy before F1 graduation.

## Final verdict (rev2)

**`pass-with-followups`.** The revision loop genuinely resolved all four false-PASS vectors and both addressed minors; I verified each by reading the hardened files at HEAD `3ea2710`, not by trusting the relay. The `tests` and `device` lenses move **fail → pass**, the BLOCKER-harden concern is **cleared**, and the device run-kit now **fails closed** — it can no longer report a green that doesn't mean "mic survived screen-off with audio flowing." Only two minor (cosmetic/doc) findings remain.

The verdict stays at `pass-with-followups` rather than `pass` for one honest reason: **the core S1-AC1..AC3 proof is still not obtained on hardware** and remains the declared `needs-device` gap — the "гейт взлёта" №1 is cleared only by the founder's device run on >=2 OEM incl. Xiaomi, now executable with a trustworthy instrument. Gate may open as `pass-with-followups` **contingent on the founder explicitly accepting the two evidence_gaps** (`needs-device`, `needs-google-maven-egress`) at the gate; that acceptance is the founder's call, not implied by this audit or by the coordinator relay. Until the device run lands, F1/M1 stay blocked per the spec.
