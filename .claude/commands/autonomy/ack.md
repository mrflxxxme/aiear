---
description: Резолв RUN-QUEUE-записи — фаундер 1-клик ack для tripwire-мёржей и эскалаций (ADR-011 D2/D8)
argument-hint: [RQ-ID approved|rejected [note]] (без аргументов: список pending)
allowed-tools: Read, Bash, PowerShell
---

# /autonomy:ack — фаундер ack/resolve для interrupt-очереди runner'а

Аргументы: **$ARGUMENTS**

## Без аргументов → покажи что ждёт
`python scripts/autonomy/run_queue.py pending` + покажи фаундеру каждую pending-запись **полным блоком** из `.planning/_session-context/RUN-QUEUE.md` (summary, categories, resolve-hint). Для `ack-needed` покажи ссылку PR + компактный risk-дайджест: `gh pr view <N> --json title,additions,deletions,files` — перечисли ТОЛЬКО tripwire-совпавшие файлы (1-клик фаундера — про них, не про весь диф). Для нативных — покажи, приложен ли `device_survival`-evidence.

## `<RQ-ID> approved|rejected [note...]`
1. `python scripts/autonomy/run_queue.py resolve <RQ-ID> --verdict <verdict> --note "<note>"`.
2. Если запись `ack-needed` с `pr:<N>` и verdict **approved**:
   - Pre-merge хук теперь пропустит мёрж (`check-ack` проходит). Заверши: `gh pr merge <N> --squash --delete-branch`, подтверди merged, отрапортуй.
   > Rails-first: под текущим режимом ЛЮБОЙ PR требует ack (не только трипвайр). После активации auto-merge только трипвайр/device-фазы попадают сюда.
3. Если **rejected**: НЕ мёржи. Суммируй, что runner'у поменять (из note), предложи follow-up (`/autonomy:run` re-entry или ручная сессия на ветке).
4. Для `escalation`-записей: verdict + note ЕСТЬ продукт-решение фаундера — запиши через `python scripts/autonomy/log_decision.py --phase <P> --kind escalated --fork "<fork>" --decision "<что фаундер выбрал>" --rationale "founder verdict: <note>"`, чтобы decision-trail был полон.

## Guardrails
- Резолвь только записи, явно названные фаундером. Никогда bulk-approve.
- Неизвестный/уже-резолвнутый RQ-ID → скажи (exit 4 скрипта) — не гадай.
- В окружении без `gh` — GitHub MCP `merge_pull_request` как эквивалент.
