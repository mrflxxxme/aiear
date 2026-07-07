# Autonomy decisions-log

> Append-only. Every agent-owned fork the autonomous runner resolved without asking the founder (ADR-011 D4). The founder's post-hoc audit trail. Architectural entries also have an ADR (see `ADR-refs`). Written by `scripts/autonomy/log_decision.py`.

### 2026-07-07T12:03:11Z | phase WAVE0-S2 | impl
- Fork: CDM background trigger: CompanionDeviceService.onDeviceAppeared vs raw ACTION_ACL_CONNECTED BroadcastReceiver
- Decision: CompanionDeviceService presence-observation (startObservingDevicePresence) as the primary path; raw ACL_CONNECTED receiver rejected
- Rationale: On API 34 implicit ACL_CONNECTED broadcasts do not reliably wake a background app, and REQUEST_COMPANION_START_FOREGROUND_SERVICES_FROM_BACKGROUND is designed to legalize FGS-start from the CDM presence callback — the sanctioned trigger the spec's AC2 targets. Fallback (non-mic armed FGS + tile, S3) documented if OEM blocks it.
- Reversibility: reversible

### 2026-07-07T12:03:11Z | phase WAVE0-S2 | impl
- Fork: onDeviceAppeared start target: direct mic-FGS vs non-mic 'armed' FGS then chain
- Decision: Primary path starts MicForegroundService directly (type=microphone) from onDeviceAppeared; non-mic armed FGS kept as documented OEM-fallback, not in the primary spike path
- Rationale: S2-AC2 tests raising mic-FGS in response to connect; the CDM bg-start permission covers a microphone FGS on 34. Keeping the primary minimal makes the device result unambiguous; fallback is S3's guaranteed lower tier.
- Reversibility: reversible

### 2026-07-07T12:03:11Z | phase WAVE0-S2 | impl
- Fork: S2-AC3 stop semantics on disconnect
- Decision: onDeviceDisappeared -> MicForegroundService STOP (stopForeground+stopSelf); never keep the mic open without headphones
- Rationale: Privacy + battery + AC3; reuses the existing STOP action, no new stop path
- Reversibility: reversible

### 2026-07-07T12:27:05Z | phase WAVE0-S2 | impl
- Fork: Review-driven: BLUETOOTH_CONNECT necessity + MAC-in-logs (reviewer-security/reviewer gate)
- Decision: Dropped BLUETOOTH_CONNECT (CDM path needs none — least-privilege); scrubbed BT MAC from logs (ФЗ-152 PII); guarded RECORD_AUDIO in onDeviceAppeared; assertTrue->assumeTrue on CDM-feature check; restored paired from myAssociations
- Rationale: Both review gates PASS with no compile-blockers; applied 1 security-major (MAC), 1 test-major (assume), and least-privilege minors before the founder-ack merge
- Reversibility: reversible
