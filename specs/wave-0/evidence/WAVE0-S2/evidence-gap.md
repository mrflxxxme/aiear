# WAVE0-S2 — evidence_gap: device_survival (needs-device)

**Gap:** `device_survival` (D3-native, ADR-011). The CDM background trigger — the OS calling
`CompanionCaptureService.onDeviceAppeared` on a real Bluetooth headphone connect **while the app
is closed** — cannot be exercised in the org sandbox (no device, no KVM, no real Bluetooth) and
is not reproducible on Firebase Test Lab without a paired BT peripheral. It is founder-device-only.

**What IS proven autonomously (CI-green):**
- `ci-android`: ktlint + detekt + unit + `assembleDebug`/`assembleDebugAndroidTest` — the S2 code
  compiles and the wiring test builds.
- `CompanionAutostartTest`: the CDM feature + `CompanionCaptureService` are declared/bindable, and
  the mic-FGS **start→armed / stopService→STOPPED** contract the CDM callbacks route to holds
  (S2-AC3 wiring). Runs on any device/FTL.

**What the founder must run to close the gap (GO = S2-AC1+AC2 on ≥2 OEM, Xiaomi mandatory):**
1. Install the debug APK, pair headphones in-app (one-time CDM association).
2. `specs/wave-0/evidence/WAVE0-S2/run-on-device.sh record --oem xiaomi` (then pixel/samsung) —
   close the app, connect → expect **armed** (mic-FGS notification, no UI), disconnect → **stopped**.
3. `run-on-device.sh emit --oems "xiaomi,pixel"` → writes `evidence/device_survival.json` (PASS,
   `head_sha` = the code commit). Commit ONLY that file + push → `ci-evidence` goes green.
4. `/autonomy:ack <RQ-ID> approved` → runner merges (native tripwire).

**Until then `ci-evidence` is RED BY DESIGN** — a native-background spike must not merge without
on-device proof (this is the D3-native rail doing its job, not a code failure).

**Fallback if CDM fails on an OEM (esp. MIUI/Xiaomi autostart):** BT-connect → non-mic armed FGS
with a "tap to start" notification + Quick Settings tile (S3). Record the OEM + failure mode in
`memory/cdm-bt.md` (via memory-curator) and treat tile (S3) as the guaranteed lower tier.
