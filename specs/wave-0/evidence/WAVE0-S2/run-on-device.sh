#!/usr/bin/env bash
# =============================================================================
# WAVE0-S2 founder device run-kit — CDM background autostart on BT connect
# -----------------------------------------------------------------------------
# Closes the `device_survival` evidence_gap (ADR-011 D3-native). The CDM
# background trigger CANNOT be validated in the org sandbox (no device/KVM, no
# real Bluetooth), so this runs on a REAL phone with REAL headphones and records
# the connect->arm->disconnect->stop cycle from logcat + the FGS notification.
#
# GO (S2-AC1+AC2): on >=2 OEM (Xiaomi MANDATORY) — with the app CLOSED, connecting
# the paired headphones must arm the mic-FGS (our notification + AIEAR_S1
# heartbeat appear) with NO UI; disconnecting must stop it (S2-AC3).
#
# Per device:
#   1) install the debug APK, open the app, tap "Сопрячь наушники", pick your
#      headphones, grant RECORD_AUDIO/BLUETOOTH/notifications.
#   2) ./run-on-device.sh record --oem xiaomi   # then, when prompted:
#        - swipe the app away (close it),
#        - CONNECT the headphones -> watch for ARMED,
#        - DISCONNECT them        -> watch for STOPPED.
#   3) repeat on the next OEM.
#   4) ./run-on-device.sh emit --oems "xiaomi,pixel"   # writes the evidence JSON
#      once >=2 OEM (incl. xiaomi) passed. Commit ONLY evidence/device_survival.json.
#
# The emitted evidence/device_survival.json is stamped with `git rev-parse HEAD`
# (the code commit). ci-evidence then verifies it fresh + PASS and goes green.
# =============================================================================
set -euo pipefail

PKG="com.aiear"
TAG="AIEAR_S1"                 # heartbeat tag (shared with S1 HeartbeatLogger)
CDM_TAG="aiear.cdm"            # CompanionCaptureService logs
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$HERE/../../../.." && pwd)"
OUT_DIR="$HERE/device-logs"
EVIDENCE="$REPO_ROOT/evidence/device_survival.json"
MODE="${1:-help}"; shift || true

OEM=""; OEMS=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --oem) OEM="$2"; shift 2 ;;
    --oems) OEMS="$2"; shift 2 ;;
    -h|--help) MODE="help"; shift ;;
    *) echo "unknown arg: $1" >&2; exit 2 ;;
  esac
done

require_device() {
  command -v adb >/dev/null || { echo "adb not found" >&2; exit 3; }
  adb get-state >/dev/null 2>&1 || { echo "no device attached (adb get-state)" >&2; exit 3; }
}

# ---- record: capture ONE device's connect/disconnect cycle from logcat --------
record() {
  require_device
  [[ -n "$OEM" ]] || { echo "pass --oem <name> (e.g. xiaomi)" >&2; exit 2; }
  mkdir -p "$OUT_DIR"
  local log="$OUT_DIR/s2-$OEM.log"
  adb logcat -c
  echo ">>> [$OEM] CLOSE the app now (swipe it away)."
  read -r -p "    Pressed? Enter to continue..." _
  echo ">>> [$OEM] CONNECT the paired headphones. Watching for ARMED (up to 30s)..."
  adb logcat -v time "$CDM_TAG:I" "$TAG:I" '*:S' > "$log" &
  local pid=$!
  local armed="" t=0
  while (( t < 30 )); do
    sleep 2; t=$((t+2))
    if grep -q "onDeviceAppeared" "$log" 2>/dev/null || \
       adb shell dumpsys notification --noredact 2>/dev/null | grep -q "aiear_capture"; then
      armed=1; echo "    ARMED ✓ (onDeviceAppeared / mic-FGS notification seen)"; break
    fi
  done
  echo ">>> [$OEM] DISCONNECT the headphones. Watching for STOPPED (up to 20s)..."
  local stopped="" s=0
  while (( s < 20 )); do
    sleep 2; s=$((s+2))
    if grep -q "onDeviceDisappeared" "$log" 2>/dev/null || \
       grep -q "state=STOPPED" <(adb shell run-as $PKG cat files/heartbeat_*.log 2>/dev/null) 2>/dev/null; then
      stopped=1; echo "    STOPPED ✓"; break
    fi
  done
  kill "$pid" 2>/dev/null || true
  echo "----"
  if [[ -n "$armed" && -n "$stopped" ]]; then
    echo "[$OEM] RESULT: PASS  (armed on connect, stopped on disconnect)  log=$log"
  else
    echo "[$OEM] RESULT: FAIL  (armed=${armed:-0} stopped=${stopped:-0})  log=$log"
    echo "       If FAIL on Xiaomi/MIUI: enable Autostart for AIEAR, then re-run."
    echo "       Fallback path (S3) = non-mic armed FGS + Quick Settings tile."
  fi
}

# ---- emit: write the D3 evidence artifact after >=2 OEM (incl xiaomi) pass ----
emit() {
  [[ -n "$OEMS" ]] || { echo "pass --oems \"xiaomi,pixel\" (the OEMs that PASSED)" >&2; exit 2; }
  IFS=',' read -r -a arr <<< "$OEMS"
  local n=${#arr[@]}
  local has_xiaomi=0; for o in "${arr[@]}"; do [[ "$o" == *xiaomi* ]] && has_xiaomi=1; done
  if (( n < 2 )) || (( has_xiaomi == 0 )); then
    echo "GO not met: need >=2 OEM AND Xiaomi. got n=$n xiaomi=$has_xiaomi" >&2
    echo "Do NOT emit a PASS. Record an evidence_gap instead." >&2
    exit 4
  fi
  local head; head="$(git -C "$REPO_ROOT" rev-parse HEAD)"
  local ts; ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  local devices="[" first=1
  for o in "${arr[@]}"; do
    [[ $first -eq 1 ]] && first=0 || devices+=","
    devices+="\"$o\""
  done
  devices+="]"
  mkdir -p "$(dirname "$EVIDENCE")"
  cat > "$EVIDENCE" <<JSON
{
  "schema_version": 1,
  "gate": "device_survival",
  "head_sha": "$head",
  "timestamp": "$ts",
  "verdict": "PASS",
  "runner": "specs/wave-0/evidence/WAVE0-S2/run-on-device.sh",
  "details": {
    "ac": ["S2-AC1", "S2-AC2", "S2-AC3"],
    "oems_passed": $devices,
    "cycle": "app-closed -> BT connect -> mic-FGS armed (no UI) -> BT disconnect -> stopped"
  }
}
JSON
  echo "wrote $EVIDENCE (head=$head). Commit ONLY this file, push, then /autonomy:ack the PR."
  python3 "$REPO_ROOT/scripts/autonomy/verify_evidence.py" --head-sha "$head" || \
    echo "(verify_evidence will pass on CI once committed; local HEAD differs pre-commit is expected)"
}

case "$MODE" in
  record) record ;;
  emit)   emit ;;
  *) grep '^#' "$0" | sed 's/^# \{0,1\}//' ;;
esac
