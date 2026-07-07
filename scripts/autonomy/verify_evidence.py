#!/usr/bin/env python3
"""Verify autonomy gate evidence artifacts against the current commit.

Per ADR-011 D3 (AIEAR adaptation of ORIION ADR-037) + amendment 2026-07-07
(grill, decision A5: single evidence path). A phase that runs local-only gates
(live-gold ru-STT/LLM, RuStore Pay sandbox, on-device OEM survival, adversarial
audit) which GitHub CI cannot run MUST commit an evidence artifact per gate
under ``specs/<wave>/evidence/<PHASE>/<gate>.json`` and declare the required
gates in ``specs/<wave>/evidence/<PHASE>/manifest.json`` -- INSIDE the human
evidence bundle (ADR-010), not in a repo-root ``evidence/`` dir. This script
(invoked by the ``ci-evidence`` workflow AND runnable locally) asserts, for
every declared gate: the artifact exists, is fresh (``head_sha`` == the commit
under test), and ``verdict == "PASS"``. Any miss -> non-zero exit -> merge
blocked.

Default mode discovers every ``specs/*/evidence/*/manifest.json`` and verifies
them all; ``--phase <PHASE>`` narrows discovery to one phase. No manifest found
-> exit 0 (phase without local-only gates), UNLESS ``--require`` is passed:
native/AI phases (tripwire ``requires_device_evidence`` / live-gold per
ADR-011 D3-native) MUST have a NON-EMPTY manifest, so ``--require`` turns
"no manifest / empty required_gates" into a hard FAIL. The runner passes
``--require`` for such phases; missing-manifest-is-OK is only for phases with
no local-only gates.

Backward compatible: ``--manifest PATH`` pins a single explicit manifest
(legacy call shape); ``--evidence-dir`` overrides the artifact dir (defaults
to the manifest's own directory).

Stdlib-only so CI can run it as bare ``python scripts/autonomy/verify_evidence.py``
without a virtualenv.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any

SCHEMA_VERSION = 1
_SHA_RE = re.compile(r"^[0-9a-f]{40}$")
_REQUIRED_FIELDS = ("schema_version", "gate", "head_sha", "timestamp", "verdict")


def _git_head_sha() -> str | None:
    try:
        out = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            capture_output=True,
            text=True,
            check=True,
        )
    except (subprocess.CalledProcessError, FileNotFoundError):
        return None
    return out.stdout.strip() or None


def _commit_files(sha: str) -> list[str] | None:
    """Paths touched by ``sha`` (vs its first parent). None on git failure."""
    try:
        out = subprocess.run(
            ["git", "show", "--name-only", "--format=", "--first-parent", sha],
            capture_output=True,
            text=True,
            check=True,
        )
    except (subprocess.CalledProcessError, FileNotFoundError):
        return None
    return [line.strip() for line in out.stdout.splitlines() if line.strip()]


def _last_non_evidence_commit(start_sha: str, evidence_dir: str) -> str:
    """Walk first-parent past commits that touch ONLY ``evidence_dir``.

    Resolves the head_sha chicken-and-egg: the evidence artifact records the
    commit the gate ran against, but COMMITTING the artifact advances the tip,
    so a literal ``head_sha == tip`` can never hold (the commit hash cannot
    appear inside its own tree). Freshness therefore means: no commit AFTER the
    gate ran touches anything outside ``evidence_dir``. The teeth are preserved
    -- one code/docs path in a later commit stops the walk and the evidence is
    stale again.
    """
    prefix = evidence_dir.rstrip("/\\") + "/"
    sha = start_sha
    # Bound the walk: a legitimate tail is 1-2 evidence-only commits.
    for _ in range(5):
        files = _commit_files(sha)
        if not files or not all(f.startswith(prefix) for f in files):
            return sha
        try:
            out = subprocess.run(
                ["git", "rev-parse", f"{sha}^"],
                capture_output=True,
                text=True,
                check=True,
            )
        except (subprocess.CalledProcessError, FileNotFoundError):
            return sha
        parent = out.stdout.strip()
        if not _SHA_RE.match(parent):
            return sha
        sha = parent
    return sha


def _load_json(path: Path) -> Any:
    with path.open(encoding="utf-8") as fh:
        return json.load(fh)


def _validate_evidence(payload: Any, gate: str, expected_sha: str) -> list[str]:
    """Return a list of human-readable problems (empty == the gate passes)."""
    problems: list[str] = []
    if not isinstance(payload, dict):
        return [f"evidence for '{gate}' is not a JSON object"]

    for field in _REQUIRED_FIELDS:
        if field not in payload:
            problems.append(f"missing required field '{field}'")
    if problems:
        return problems

    if payload["schema_version"] != SCHEMA_VERSION:
        problems.append(
            f"schema_version {payload['schema_version']!r} != {SCHEMA_VERSION}"
        )
    if payload["gate"] != gate:
        problems.append(
            f"gate field {payload['gate']!r} != declared gate {gate!r}"
        )
    head_sha = str(payload["head_sha"])
    if not _SHA_RE.match(head_sha):
        problems.append(f"head_sha {head_sha!r} is not a 40-char sha")
    elif head_sha != expected_sha:
        problems.append(
            f"STALE: evidence head_sha {head_sha[:12]} != commit under test "
            f"{expected_sha[:12]} -gate did not run against this code"
        )
    if payload["verdict"] != "PASS":
        problems.append(f"verdict is {payload['verdict']!r}, not PASS")
    return problems


def _discover_manifests(phase: str | None) -> list[Path]:
    """All ``specs/*/evidence/<PHASE>/manifest.json`` (single evidence path, A5)."""
    pattern = f"specs/*/evidence/{phase}/manifest.json" if phase else "specs/*/evidence/*/manifest.json"
    return sorted(Path(".").glob(pattern))


def _verify_manifest(
    manifest_path: Path,
    evidence_dir_override: str | None,
    head_sha_arg: str | None,
    require: bool,
) -> int:
    """Verify one manifest's declared gates. Returns the failure count."""
    try:
        manifest = _load_json(manifest_path)
    except (json.JSONDecodeError, OSError) as exc:
        print(f"[ci-evidence] FAIL: cannot read manifest {manifest_path}: {exc}")
        return 1

    required = manifest.get("required_gates", []) if isinstance(manifest, dict) else None
    if not isinstance(required, list):
        print(f"[ci-evidence] FAIL: {manifest_path}: required_gates must be a list")
        return 1

    declared_phase = manifest.get("phase") if isinstance(manifest, dict) else None
    dir_phase = manifest_path.parent.name
    if declared_phase and declared_phase != dir_phase:
        print(
            f"[ci-evidence] WARN: {manifest_path}: manifest.phase {declared_phase!r} "
            f"!= evidence dir {dir_phase!r}"
        )

    if not required:
        if require:
            print(
                f"[ci-evidence] FAIL: {manifest_path} declares NO required gates, but this "
                "phase is native/AI (--require): an EMPTY manifest cannot close it "
                "(ADR-011 D3-native / decision 4.4)."
            )
            return 1
        print(f"[ci-evidence] {manifest_path}: no required gates declared. OK.")
        return 0

    expected_sha = head_sha_arg or _git_head_sha()
    if not expected_sha or not _SHA_RE.match(expected_sha):
        print(
            "[ci-evidence] FAIL: could not resolve the commit-under-test sha "
            "(pass --head-sha or run inside a git repo)"
        )
        return 1

    evidence_dir = Path(evidence_dir_override) if evidence_dir_override else manifest_path.parent

    resolved_sha = _last_non_evidence_commit(expected_sha, str(evidence_dir))
    if resolved_sha != expected_sha:
        print(
            f"[ci-evidence] tip {expected_sha[:12]} is an evidence-only tail; "
            f"gates must have run against {resolved_sha[:12]} (last non-evidence commit)"
        )
        expected_sha = resolved_sha

    failures = 0
    print(
        f"[ci-evidence] {manifest_path.parent}: verifying {len(required)} gate(s) "
        f"against {expected_sha[:12]}"
    )
    for gate in required:
        ev_path = evidence_dir / f"{gate}.json"
        if not ev_path.exists():
            print(f"  [MISS] {gate}: no evidence artifact at {ev_path}")
            failures += 1
            continue
        try:
            payload = _load_json(ev_path)
        except (json.JSONDecodeError, OSError) as exc:
            print(f"  [FAIL] {gate}: cannot read {ev_path}: {exc}")
            failures += 1
            continue
        problems = _validate_evidence(payload, gate, expected_sha)
        if problems:
            failures += 1
            for problem in problems:
                print(f"  [FAIL] {gate}: {problem}")
        else:
            cost = payload.get("cost_usd")
            cost_str = f" (${cost})" if cost is not None else ""
            print(f"  [OK]   {gate}: PASS{cost_str}")
    return failures


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--manifest",
        default=None,
        help="Explicit manifest path (legacy single-manifest mode). Default: discover "
        "specs/*/evidence/*/manifest.json.",
    )
    parser.add_argument(
        "--phase",
        default=None,
        help="Narrow discovery to specs/*/evidence/<PHASE>/manifest.json.",
    )
    parser.add_argument(
        "--evidence-dir",
        default=None,
        help="Override the gate-artifact dir. Default: the manifest's own directory.",
    )
    parser.add_argument(
        "--require",
        action="store_true",
        help="Native/AI phase: a missing or EMPTY manifest is a FAIL, not OK "
        "(ADR-011 D3-native; auto-merge condition 3, grill 2026-07-07).",
    )
    parser.add_argument(
        "--head-sha",
        default=None,
        help="Commit the gates must have run against. Defaults to `git rev-parse HEAD`.",
    )
    args = parser.parse_args(argv)

    if args.manifest:
        manifests = [Path(args.manifest)]
        missing = [p for p in manifests if not p.exists()]
        if missing:
            if args.require:
                print(
                    f"[ci-evidence] FAIL: no manifest at {missing[0]} but the phase is "
                    "native/AI (--require): machine manifest is MANDATORY (ADR-011 D3-native)."
                )
                return 1
            print(f"[ci-evidence] no manifest at {missing[0]} -no local-only gates to verify. OK.")
            return 0
    else:
        manifests = _discover_manifests(args.phase)
        if not manifests:
            where = f"specs/*/evidence/{args.phase or '*'}/manifest.json"
            if args.require:
                print(
                    f"[ci-evidence] FAIL: no manifest found at {where} but the phase is "
                    "native/AI (--require): machine manifest is MANDATORY (ADR-011 D3-native)."
                )
                return 1
            print(f"[ci-evidence] no manifest found at {where} -no local-only gates to verify. OK.")
            return 0

    failures = 0
    for manifest_path in manifests:
        failures += _verify_manifest(manifest_path, args.evidence_dir, args.head_sha, args.require)

    if failures:
        print(f"[ci-evidence] FAIL: {failures} gate(s) missing/stale/failed -merge blocked.")
        return 1
    print("[ci-evidence] all declared gates verified fresh + PASS. OK.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
