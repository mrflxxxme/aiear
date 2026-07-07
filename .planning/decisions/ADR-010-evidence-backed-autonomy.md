---
id: ADR-010
title: Evidence-backed автономность — self-run тестов + live-gold перед PR
status: accepted
date: 2026-06-24
supersedes: []
informs: [ALL]
deciders: [founder]
---

# ADR-010 — Evidence-backed автономность

## Контекст
Фаундер требует **полную, подтверждённую результатами автономность внутри фаз**: агенты должны сами прогонять проверки и приносить на гейт **доказательства прохождения**, а не утверждения «должно работать». Для EARAI это критично — главный риск (нативный фон/OEM, качество ru-STT/LLM) невозможно закрыть mock-зелёным; нужна проверка на **реальных** сервисах/устройствах.

Понятия:
- **Gold** — прогон по golden dataset (детерминированные эталонные выходы).
- **Live-gold** — те же golden/приёмочные сценарии, исполненные против **реальных** сервисов/устройств (live Yandex SpeechKit, live GigaChat/YandexGPT, RuStore Pay sandbox, реальные OEM через Firebase Test Lab / agent-device), а не моков. Результат — реальные метрики (WER, pass-rate, латентность, выживаемость на устройстве), сохранённые как evidence.

## Решение
**Фаза не доходит до PR без evidence-бандла.** Правила:

1. **Self-run перед хендофом.** Имплементер (`android-engineer`/`backend-engineer`) сам прогоняет lint + typecheck + unit + (где есть) integration **до** хендофа в ревью и прикладывает вывод. «Написал тесты» ≠ «тесты зелёные».
2. **Verifier исполняет, не только определяет.** `verifier` реально гоняет EARS-как-тесты и **live-gold там, где возможно** (device-матрица FTL + agent-device для нативного; live-сервисы для бэка) и прикладывает evidence. Вердикт гейта = **подтверждён результатами**, не утверждением.
3. **Live-gold для AI-фаз.** `evaluator` гоняет golden + adversarial против **live** STT/LLM (не моков), прикладывает pass-rate/WER. Mock-only прогон не закрывает AI-фазу.
4. **Где live невозможно — явный gap, не тихий скип.** Нет ключей/устройства/sandbox → агент эмитит `status: blocked` (`blocker_type: needs-live-evidence`) ИЛИ помечает `evidence_gap` в хендофе с причиной и что нужно (creds/device/sandbox), и поднимает фаундеру. **Молчаливый mock-зелёный запрещён.**
5. **No evidence = fail.** На phase-close аудите линза `live-gold/evidence` обязательна; нет воспроизводимого evidence на критерий → вердикт `fail` (или `pass-with-followups` только если gap явно задекларирован и принят фаундером).

> **Поправка 2026-07-07 (grill, решение 4.1) — native-gap гибрид.** Единая формулировка (снимает противоречие с ADR-011 D3-native «нет device-evidence → stuck»): **нативная фаза без device-evidence МОЖЕТ закрыться `pass-with-followups`; runner продолжает следующую фазу; merge PR в `main` блокирован до device-evidence ИЛИ явного founder-ack на gap (RUN-QUEUE-запись).** Gap по-прежнему явный (п. 4), «тихий mock-зелёный» по-прежнему запрещён — гибрид меняет только то, что gap не стопорит конвейер, а стопорит **мёрж**. Протокол: [`GRILL-2026-07-07-project-docs.md`](../_session-context/GRILL-2026-07-07-project-docs.md).

Где: evidence-бандл — `specs/<wave>/evidence/<PHASE>/` (тест-репорты, ссылки на FTL-прогоны, live-gold-результаты, device-логи, coverage). Ссылается из хендоф-поля `evidence`, `AUDIT-<PHASE>.md` и тела PR.

## Последствия
- Хендоф-контракт: новое поле `evidence` (пути/ссылки) на impl/verify/eval-хендофах ([`handoff-template.md`](../../.claude/agents/_shared/handoff-template.md)).
- Пайплайны: явный **self-test + evidence** гейт перед ревью и перед PR ([`pipeline-*.yaml`](../../.claude/agents/_shared/)).
- Phase-close аудит: 7 линз → **8** (+ `live-gold/evidence`); см. [`04-POST-AUDIT.md`](../agent-handbook/04-POST-AUDIT.md).
- PR-тело: секция **Evidence** обязательна ([`06-PR-WORKFLOW.md`](../agent-handbook/06-PR-WORKFLOW.md)).
- Спеки: блок **Evidence & live-gold plan** обязателен (шаблоны + `specs/README.md`).
- Канон: [`07-VERIFICATION-EVIDENCE.md`](../agent-handbook/07-VERIFICATION-EVIDENCE.md) — единый источник «что есть evidence и live-gold».
- Стоимость: live-прогоны жгут API/FTL-минуты — заложено в `cost-budget.yaml` (отдельная статья verify/eval). Это сознательный размен: дороже прогон, но автономность подтверждена.

## Альтернативы (отклонены)
- **Mock-зелёный достаточно** — не закрывает нативный/AI-риск EARAI; даёт ложную уверенность.
- **Только CI проверяет (без self-run)** — агент приносит непроверенный код, CI-цикл дороже и медленнее; self-run дешевле ловит до PR.
- **Live-gold опционально** — размывает «подтверждённую автономность»; делаем обязательным там, где технически возможно, с явной декларацией gap иначе.
