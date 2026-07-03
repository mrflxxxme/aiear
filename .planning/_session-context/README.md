# `_session-context/` — рабочая память автономного runner'а

Артефакты автономного многофазного runner'а ([ADR-011](../decisions/ADR-011-autonomous-multiphase-runner.md)). НЕ роллинг-статус проекта (это `../STATUS.md`) — это межсессионное состояние runner'а: что ждёт фаундера и что агент решил сам.

| Файл | Роль | Пишет |
|---|---|---|
| [`RUN-QUEUE.md`](./RUN-QUEUE.md) | D8 interrupt-очередь: ack-needed / escalation / revert / stuck / complete. Единое окно фаундера «что ждёт меня». Резолв — `/autonomy:ack`. | `scripts/autonomy/run_queue.py` |
| [`DECISIONS-LOG.md`](./DECISIONS-LOG.md) | D4 decision-log: каждый agent-owned форк, решённый без вопроса фаундеру. Post-hoc audit trail. Архитектурные — с ADR-ref. | `scripts/autonomy/log_decision.py` |

Оба append-only, машинно-парсимые заголовки, пишутся скриптами (не редактируй вручную — сломаешь парсер `run_queue.py`/`check-ack`). UTF-8 (русские summary), но заголовки записей — ASCII-стабильный формат.

Отношение к memory-curator: STATUS/JOURNAL/memory остаётся зоной `memory-curator` (single-writer, handbook правило). Эти два файла — рантайм-состояние runner'а, их ведут его скрипты; `memory-curator` на phase-close консолидирует из них уроки в durable-память, но не переписывает их.
