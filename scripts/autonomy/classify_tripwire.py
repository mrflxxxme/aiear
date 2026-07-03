#!/usr/bin/env python3
"""Classify a PR diff against the autonomy tripwire (ADR-011 D2, AIEAR).

The autonomous runner calls this BEFORE (would-be) auto-merging a green phase.
If any changed file matches a tripwire category — DB migrations, auth/account/
sessions, billing/money, secrets/keys, PII/ФЗ-242 audio-residency, native
background/permissions, public API contracts — the phase must NOT auto-merge:
it goes to RUN-QUEUE + notify + wait for founder ``/ack``. No match -> auto-merge
(once the founder has flipped auto-merge ON; until then the runner pauses at
every PR regardless — see ADR-011 D1 rails-first sequencing).

AIEAR v1 policy: ANY matched file = ack (conservative). Unlike ORIION there is
NO migration content-downgrade yet (backend/migrations don't exist) — add it
only after trust accrues.

Exit codes:
  0  clean — no tripwire category matched
  10 tripwire matched — pause-and-ack required (NOT an error; a decision signal)
  1  usage / IO error (fail-closed: the pre-merge hook treats this as "block")

Reads ``.claude/autonomy/tripwire.yaml`` with a stdlib-only minimal parser (no
PyYAML dependency — AIEAR has no guaranteed python venv; the hook + CI run this
as bare ``python``). Changed files come from (priority order): ``--files a b
c``, or ``--diff-base <ref>`` (``git diff --name-only <ref>...HEAD``), default
base ``origin/main``. Emits a JSON verdict to stdout for the runner to consume.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any

_DEFAULT_CONFIG = Path(".claude/autonomy/tripwire.yaml")

_TRUE = {"true", "yes", "on", "1"}


def parse_tripwire(text: str) -> dict[str, Any]:
    """Minimal YAML reader for the fixed tripwire.yaml shape (2-space indent).

    Understands: top-level ``key: value`` scalars, the ``categories:`` mapping,
    per-category ``reason:`` / ``requires_device_evidence:`` scalars, and a
    ``globs:`` block of ``- "pattern"`` list items. Block scalars (``|``) and
    flow collections are intentionally NOT supported — the config must stay
    within this subset (keeps the classifier dependency-free and auditable).
    """
    categories: dict[str, dict[str, Any]] = {}
    top: dict[str, Any] = {"categories": categories}
    cur_name: str | None = None
    cur_cat: dict[str, Any] | None = None
    in_categories = False
    in_globs = False

    for raw in text.splitlines():
        # strip a trailing inline comment only when clearly outside a quote
        line = raw.rstrip("\n")
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        indent = len(line) - len(line.lstrip(" "))
        stripped = line.strip()

        if indent == 0:
            in_categories = stripped.rstrip() == "categories:"
            in_globs = False
            cur_name = cur_cat = None
            if not in_categories and ":" in stripped:
                key, _, val = stripped.partition(":")
                top[key.strip()] = _scalar(val.strip())
            continue

        if not in_categories:
            continue

        if indent == 2 and stripped.endswith(":"):
            cur_name = stripped[:-1].strip()
            cur_cat = {"reason": "", "globs": [], "requires_device_evidence": False}
            categories[cur_name] = cur_cat
            in_globs = False
            continue

        if cur_cat is None:
            continue

        if indent == 4:
            in_globs = False
            if stripped == "globs:":
                in_globs = True
            elif ":" in stripped:
                key, _, val = stripped.partition(":")
                key = key.strip()
                val = _scalar(val.strip())
                if key == "requires_device_evidence":
                    cur_cat[key] = str(val).lower() in _TRUE
                else:
                    cur_cat[key] = val
            continue

        if indent >= 6 and in_globs and stripped.startswith("- "):
            cur_cat["globs"].append(_scalar(stripped[2:].strip()))

    return top


def _scalar(val: str) -> str:
    """Unquote a scalar and drop a trailing inline comment on unquoted values."""
    val = val.strip()
    if len(val) >= 2 and val[0] in "\"'" and val[-1] == val[0]:
        return val[1:-1]
    # unquoted: cut an inline comment
    if "#" in val:
        val = val.split("#", 1)[0].strip()
    return val


def _glob_to_regex(glob: str) -> re.Pattern[str]:
    """Translate a path glob (with ``**``) to an anchored regex on '/'-paths.

    ``**`` matches across directory separators; ``*`` matches within a segment;
    ``?`` matches a single non-separator char.
    """
    i = 0
    out: list[str] = ["^"]
    n = len(glob)
    while i < n:
        c = glob[i]
        if c == "*":
            if i + 1 < n and glob[i + 1] == "*":
                i += 2
                if i < n and glob[i] == "/":
                    out.append("(?:.*/)?")
                    i += 1
                else:
                    out.append(".*")
            else:
                out.append("[^/]*")
                i += 1
        elif c == "?":
            out.append("[^/]")
            i += 1
        else:
            out.append(re.escape(c))
            i += 1
    out.append("$")
    return re.compile("".join(out))


def _changed_files(args: argparse.Namespace) -> list[str]:
    if args.files:
        return [f.strip().replace("\\", "/") for f in args.files if f.strip()]
    base = args.diff_base
    try:
        out = subprocess.run(
            ["git", "diff", "--name-only", f"{base}...HEAD"],
            capture_output=True,
            text=True,
            check=True,
        )
    except (subprocess.CalledProcessError, FileNotFoundError) as exc:
        print(f"[tripwire] cannot compute git diff against {base}: {exc}", file=sys.stderr)
        raise SystemExit(1) from exc
    return [ln.strip().replace("\\", "/") for ln in out.stdout.splitlines() if ln.strip()]


def classify(files: list[str], config: dict[str, Any]) -> list[dict[str, Any]]:
    """Return matched categories, each with matched_files + device-evidence flag."""
    matches: list[dict[str, Any]] = []
    for name, spec in (config.get("categories") or {}).items():
        patterns = [_glob_to_regex(g) for g in (spec.get("globs") or [])]
        hit = sorted({f for f in files if any(p.match(f) for p in patterns)})
        if hit:
            matches.append(
                {
                    "category": name,
                    "reason": spec.get("reason", ""),
                    "requires_device_evidence": bool(spec.get("requires_device_evidence")),
                    "matched_files": hit,
                }
            )
    return matches


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", default=str(_DEFAULT_CONFIG))
    parser.add_argument("--diff-base", default="origin/main")
    parser.add_argument("--files", nargs="*", default=None, help="Explicit file list (skips git).")
    args = parser.parse_args(argv)

    config_path = Path(args.config)
    if not config_path.exists():
        print(f"[tripwire] config not found: {config_path}", file=sys.stderr)
        return 1
    config = parse_tripwire(config_path.read_text(encoding="utf-8"))
    if not config.get("categories"):
        # Fail-closed: an empty/unparseable config must NOT read as "all clean".
        print("[tripwire] no categories parsed from config — refusing to classify (fail-closed)", file=sys.stderr)
        return 1

    files = _changed_files(args)
    matches = classify(files, config)
    device_required = sorted(
        m["category"] for m in matches if m.get("requires_device_evidence")
    )
    verdict = {
        "changed_files": len(files),
        "tripwire_matched": bool(matches),
        "decision": "pause-and-ack" if matches else "auto-merge",
        "categories": matches,
        "device_evidence_required_by": device_required,
    }
    print(json.dumps(verdict, ensure_ascii=False, indent=2))
    return 10 if matches else 0


if __name__ == "__main__":
    sys.exit(main())
