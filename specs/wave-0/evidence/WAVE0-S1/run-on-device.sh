#!/usr/bin/env bash
# =============================================================================
# WAVE0-S1 founder device run-kit — mic-FGS survives screen-off >=60 min
# -----------------------------------------------------------------------------
# Closes the `needs-device` evidence_gap. Runs the S1 survival test on a REAL
# phone (the org sandbox has no device/KVM), pulls the heartbeat log, and scores
# the max heartbeat gap. GO = S1-AC1 green on >=2 OEM, Xiaomi MANDATORY.
#
# Two modes:
#   (A) gradle   — full instrumented test (default). Needs Android SDK + Google
#                  Maven egress on THIS machine (the test self-manages screen-off
#                  + Doze + assertion). Recommended.
#   (B) manual   — pure-adb: start service, screen off, force Doze, wait, pull.
#                  Use when you have only the installed debug APK, no SDK build.
#
# Usage:
#   ./run-on-device.sh                       # mode A, 60 min, Doze on
#   ./run-on-device.sh --minutes 60          # explicit duration
#   ./run-on-device.sh --manual --minutes 60 # mode B
#   ./run-on-device.sh --parse-only FILE     # just score an existing heartbeat log
#   ANDROID_SERIAL=<serial> ./run-on-device.sh   # pick a device when several attached
#
# Beat period = 10 s; a gap > 15 000 ms = a stall/kill = S1-AC1 FAIL on this device.
# =============================================================================
set -euo pipefail

PKG="com.aiear"
TAG="AIEAR_S1"
MAX_ALLOWED_GAP_MS=15000
EARLY_END_TOL_MS=45000
MINUTES=60
FORCE_DOZE=true
MODE="gradle"
PARSE_ONLY=""
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$HERE/device-logs"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --minutes) MINUTES="$2"; shift 2 ;;
    --manual) MODE="manual"; shift ;;
    --no-doze) FORCE_DOZE=false; shift ;;
    --parse-only) PARSE_ONLY="$2"; shift 2 ;;
    -h|--help) grep '^#' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "unknown arg: $1" >&2; exit 2 ;;
  esac
done

# --- parser: score ONE session log (one heartbeat_<session>.log = one session) -
# FAILS CLOSED: empty/insufficient -> non-zero; within-file t= reset -> FAIL;
# gap over tolerance -> FAIL; ended early (last_t < expect-tol) -> FAIL (killed
# mid-run). $2 = expected total ms (0 to skip the early-end check). Portable awk.
parse_heartbeat() {
  local f="$1"
  local expect_ms="${2:-0}"
  if [[ ! -s "$f" ]]; then echo "PARSE: empty/missing log: $f -> FAIL"; return 1; fi
  grep -oE 't=[0-9]+' "$f" | cut -d= -f2 | awk \
      -v lim="$MAX_ALLOWED_GAP_MS" -v expect="$expect_ms" -v tol="$EARLY_END_TOL_MS" '
    { t = $1 + 0; n++
      if (have && t < prev) reset = 1            # t went backwards in one file = corruption/merge
      if (have) { d = t - prev; if (d > maxd) { maxd = d; at = prev } }
      if (t > last) last = t
      prev = t; have = 1 }
    END {
      if (n < 2) { print "PARSE: beats=" n " (need >=2) -> INSUFFICIENT/FAIL"; exit 1 }
      printf "PARSE: beats=%d  max_gap=%d ms  last_t=%d ms  break_near_t=%d ms\n", n, maxd, last, at
      fail = 0
      if (reset)    { print  "RESULT: FAIL (t= reset within file -> session corruption/restart)"; fail = 1 }
      if (maxd > lim) { printf "RESULT: FAIL (gap %d > %d ms) -> S1-AC1 NOT met\n", maxd, lim; fail = 1 }
      if (expect > 0 && last < expect - tol) {
        printf "RESULT: FAIL (ended early: last_t %d < %d-%d ms) -> killed mid-run\n", last, expect, tol; fail = 1 }
      if (fail) exit 1
      print "RESULT: PASS (continuous + full duration within tolerance) -> S1-AC1 met on this device"
    }'
}

if [[ -n "$PARSE_ONLY" ]]; then parse_heartbeat "$PARSE_ONLY" 0; exit $?; fi

# --- preconditions -----------------------------------------------------------
command -v adb >/dev/null || { echo "adb not found (install platform-tools)"; exit 2; }
SERIAL_ARGS=()
[[ -n "${ANDROID_SERIAL:-}" ]] && SERIAL_ARGS=(-s "$ANDROID_SERIAL")
adb "${SERIAL_ARGS[@]}" get-state >/dev/null 2>&1 || { echo "no device. Connect a phone + enable USB debugging."; exit 2; }
SERIAL="$(adb "${SERIAL_ARGS[@]}" get-serialno)"
MODEL="$(adb "${SERIAL_ARGS[@]}" shell getprop ro.product.model | tr -d '\r')"
BRAND="$(adb "${SERIAL_ARGS[@]}" shell getprop ro.product.brand | tr -d '\r')"
REL="$(adb "${SERIAL_ARGS[@]}" shell getprop ro.build.version.release | tr -d '\r')"
RUN_DIR="$OUT_DIR/${BRAND}-${MODEL}-${SERIAL}"
mkdir -p "$RUN_DIR"
echo ">> device: $BRAND $MODEL (Android $REL, serial $SERIAL)  duration=${MINUTES}m doze=$FORCE_DOZE mode=$MODE"
echo ">> evidence dir: $RUN_DIR"
[[ "$BRAND" =~ [Xx]iaomi|[Rr]edmi|[Pp]oco ]] && echo ">> NOTE: Xiaomi/MIUI — set the app to 'No battery restrictions' + Autostart ON before running (see checklist)."

# stream logcat for the tag in the background
adb "${SERIAL_ARGS[@]}" logcat -c || true
adb "${SERIAL_ARGS[@]}" logcat "$TAG:I" '*:S' > "$RUN_DIR/logcat-$TAG.txt" &
LOGCAT_PID=$!
trap 'kill $LOGCAT_PID 2>/dev/null || true' EXIT

if [[ "$MODE" == "gradle" ]]; then
  command -v ./gradlew >/dev/null 2>&1 || cd "$HERE/../../../.."   # repo root (where gradlew lives)
  echo ">> mode A: ./gradlew :app:connectedDebugAndroidTest (needs SDK + Google Maven egress)"
  set +e
  ./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.durationMin="$MINUTES" \
    -Pandroid.testInstrumentationRunnerArguments.forceDoze="$FORCE_DOZE" \
    2>&1 | tee "$RUN_DIR/gradle-connected.txt"
  GRADLE_RC=${PIPESTATUS[0]}
  set -e
  echo ">> gradle connected test rc=$GRADLE_RC (the test itself asserts continuity)"
else
  echo ">> mode B: manual adb sequence"
  adb "${SERIAL_ARGS[@]}" shell pm grant "$PKG" android.permission.RECORD_AUDIO || true
  adb "${SERIAL_ARGS[@]}" shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS || true
  adb "${SERIAL_ARGS[@]}" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
  sleep 3
  adb "${SERIAL_ARGS[@]}" shell am start-foreground-service -n "$PKG/.capture.service.MicForegroundService" -a com.aiear.capture.action.START || true
  sleep 2
  adb "${SERIAL_ARGS[@]}" shell input keyevent KEYCODE_SLEEP
  if [[ "$FORCE_DOZE" == "true" ]]; then
    adb "${SERIAL_ARGS[@]}" shell dumpsys deviceidle force-idle || true
  fi
  echo ">> holding ${MINUTES} min with screen off..."
  sleep $(( MINUTES * 60 ))
  adb "${SERIAL_ARGS[@]}" shell dumpsys deviceidle unforce || true
  adb "${SERIAL_ARGS[@]}" shell input keyevent KEYCODE_WAKEUP
  adb "${SERIAL_ARGS[@]}" shell am start-foreground-service -n "$PKG/.capture.service.MicForegroundService" -a com.aiear.capture.action.STOP || true
fi

# --- pull the app-private heartbeat logs (debug build is run-as-able) --------
echo ">> pulling heartbeat logs via run-as"
mapfile -t LOGS < <(adb "${SERIAL_ARGS[@]}" shell run-as "$PKG" ls files 2>/dev/null | tr -d '\r' | grep '^heartbeat_' || true)
if [[ ${#LOGS[@]} -eq 0 ]]; then
  echo "!! no heartbeat_*.log found under files/ — capture may never have started (check perms/MIUI autostart)."
else
  for L in "${LOGS[@]}"; do
    adb "${SERIAL_ARGS[@]}" shell run-as "$PKG" cat "files/$L" > "$RUN_DIR/$L"
    echo "   -> $RUN_DIR/$L"
  done
fi

echo ">> scoring continuity (fail-closed)"
EXPECT_MS=$(( MINUTES * 60 * 1000 ))
RC=0
shopt -s nullglob
FILES=( "$RUN_DIR"/heartbeat_*.log )
shopt -u nullglob
if [[ ${#FILES[@]} -eq 0 ]]; then
  echo "RESULT: FAIL (no heartbeat log pulled -> capture never started / run-as blocked / killed instantly)"
  RC=1
elif [[ ${#FILES[@]} -gt 1 ]]; then
  echo "RESULT: FAIL (${#FILES[@]} heartbeat sessions -> the original mic-FGS was KILLED and restarted)"
  for L in "${FILES[@]}"; do parse_heartbeat "$L" "$EXPECT_MS" || true; done
  RC=1
else
  parse_heartbeat "${FILES[0]}" "$EXPECT_MS" || RC=$?
fi
echo ">> DONE (exit $RC). Paste $RUN_DIR/ into the PR (device-logs/) and tick the row in device-matrix-checklist.md."
exit $RC
