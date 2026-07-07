# RUN-QUEUE — autonomous runner interrupt queue

> Append-only log of runner interrupt events (ADR-011 D8): ack-needed / escalation / revert / stuck / complete. Pending entries are waiting for the founder; resolve with `/autonomy:ack <ID> <verdict>`. Written by `scripts/autonomy/run_queue.py`.

### RQ-20260707-001 | ack-needed | pr:4 | phase:WAVE0-S2 | 2026-07-07T12:28:26Z | status:pending
- Summary: PR #4 (WAVE0-S2 CDM-автозапуск) — native tripwire ack + device_survival gap
- Categories: native_background_permissions (manifest + CompanionCaptureService/Pairing + test). Reviews: reviewer-security PASS 0-blockers, reviewer PASS 0-compile-blockers (fixes applied).
- device_survival: evidence_gap (founder-only). ci-evidence RED BY DESIGN until evidence/device_survival.json (PASS) committed from run-on-device.sh on >=2 OEM (Xiaomi mandatory).
- Merge path: close device_survival -> ci-android+ci-evidence green -> /autonomy:ack  approved.
- Resolve: `/autonomy:ack RQ-20260707-001 approved|rejected`
