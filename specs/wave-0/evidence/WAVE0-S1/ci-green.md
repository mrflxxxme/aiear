# CI green — WAVE0-S1 (closes `needs-google-maven-egress`)

**Date:** 2026-06-24 · **PR:** #2 · **Head:** `2afb36c` · **All 3 checks green.**

GitHub Actions runs on a runner with Google-Maven egress (unlike the agent sandbox, where
`maven.google.com`/`dl.google.com` are 403-blocked), so the full Android self-run executes
there for real.

| Check | Result | Notes |
|---|---|---|
| `ci-android` | ✅ success ([run 28090768093](https://github.com/mrflxxxme/aiear/actions/runs/28090768093)) | `setup-android` → `ktlintCheck` → `detekt` → `testDebugUnitTest` → `assembleDebug` → `assembleDebugAndroidTest` |
| `ci-security` | ✅ success | gitleaks (with `GITHUB_TOKEN`) |
| `ci-backend` | ✅ success | no `backend/` yet → steps skip |

## What this proves (live, on a real Android toolchain)
- **Compiles:** `assembleDebug` builds the app (manifest, FGS service, notification, Compose UI).
- **Unit green:** `testDebugUnitTest` runs `HeartbeatLoggerTest` (the continuity-detector seam).
- **Instrumented test compiles:** `assembleDebugAndroidTest` (UiAutomator + Compose-test deps resolve).
- **Lint clean:** `ktlintCheck` + `detekt` pass (Compose-aware config).

## What this does NOT prove (still `needs-device` — Gap 2)
CI compiles + assembles + unit-tests; it does **not** run the instrumented survival test on a
device. The real S1-AC1..AC3 (≥60 min screen-off + Doze + heartbeat continuity on a physical
OEM, Xiaomi mandatory) still needs the founder device run — see `device-matrix-checklist.md`.

## Getting here (CI was red on inherited templates, not on the code)
The CI templates were broken independently of the app: invalid YAML (`run: echo "...: ..."`),
`secrets` in `if:`, `hashFiles()` in a job-level `if:` (startup_failures), gitleaks missing
`GITHUB_TOKEN`, then Compose lint config (detekt + ktlint function-naming / nesting). All fixed;
lessons codified in `agent-handbook/06-PR-WORKFLOW.md` + `07-VERIFICATION-EVIDENCE.md`.
