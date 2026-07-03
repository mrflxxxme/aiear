#!/usr/bin/env python3
"""Compose a spawnable prompt for an AIEAR agent role (ADR-011 Block C).

The autonomous runner delegates phase work to the profile roles (planner /
android-engineer / backend-engineer / reviewer / verifier / evaluator / ...).
AIEAR keeps each role as a SINGLE file ``.claude/agents/<role>.md`` (YAML
frontmatter: name/description/model, then the role body). This script reads that
file and emits a spawn-prompt the runner passes to a general-purpose ``Agent``/
``Task`` spawn — so the pipeline can delegate to a role without the role being a
separately-registered native subagent.

(ORIION's load_role reads a per-role DIRECTORY of system-prompt/tools-allowlist/
workflows/checklists; AIEAR's roles are single-file, so this variant reads the
one file and, on request, appends the shared model-routing note.)

Output (stdout, UTF-8): the role file body + a composed header. Frontmatter is
kept (the model: hint is useful to the spawner).

Examples:
  python scripts/autonomy/load_role.py --role planner
  python scripts/autonomy/load_role.py --role android-engineer --with-routing
  python scripts/autonomy/load_role.py --list
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

_AGENTS_DIR = ".claude/agents"
_SHARED_DIR = "_shared"
_ROUTING = "_shared/model-routing.md"


def _repo_root() -> Path:
    # scripts/autonomy/load_role.py -> repo root is parents[2]
    return Path(__file__).resolve().parents[2]


def _roles(agents_dir: Path) -> list[str]:
    return sorted(
        p.stem
        for p in agents_dir.glob("*.md")
        if p.is_file()
    )


def _section(title: str, body: str) -> str:
    return f"\n\n<!-- ==== {title} ==== -->\n\n{body.strip()}\n"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--role", default=None)
    parser.add_argument("--agents-dir", default=None, help="Defaults to <repo>/.claude/agents.")
    parser.add_argument("--with-routing", action="store_true", help="Append _shared/model-routing.md.")
    parser.add_argument("--list", action="store_true", help="List available roles and exit.")
    args = parser.parse_args(argv)

    # The composed prompt may contain non-cp1251 glyphs (arrows etc.) — force
    # UTF-8 stdout so a Windows cp1251 console doesn't UnicodeEncodeError.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    agents_dir = Path(args.agents_dir) if args.agents_dir else _repo_root() / _AGENTS_DIR
    if not agents_dir.is_dir():
        print(f"[load_role] agents dir not found: {agents_dir}", file=sys.stderr)
        return 1

    if args.list:
        for role in _roles(agents_dir):
            print(role)
        return 0

    if not args.role:
        print("[load_role] --role is required (or --list)", file=sys.stderr)
        return 1

    role_file = agents_dir / f"{args.role}.md"
    if not role_file.exists():
        print(
            f"[load_role] unknown role '{args.role}' (no {role_file}). "
            f"Available: {', '.join(_roles(agents_dir))}",
            file=sys.stderr,
        )
        return 1

    parts: list[str] = [
        f"<!-- AIEAR role spawn-prompt: {args.role} (composed by "
        f"scripts/autonomy/load_role.py; source: {_AGENTS_DIR}/{args.role}.md) -->",
        _section("ROLE (system prompt)", role_file.read_text(encoding="utf-8")),
    ]

    if args.with_routing:
        routing = agents_dir / _ROUTING
        if routing.exists():
            parts.append(_section("MODEL ROUTING (_shared)", routing.read_text(encoding="utf-8")))
        else:
            print(f"[load_role] note: no {routing}", file=sys.stderr)

    print("".join(parts))
    return 0


if __name__ == "__main__":
    sys.exit(main())
