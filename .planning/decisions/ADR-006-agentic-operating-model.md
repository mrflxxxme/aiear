---
id: ADR-006
title: Операционная модель — соло-фаундер + ИИ-агенты (SDD-харнесс), гибридный рантайм
status: accepted
date: 2026-06-23
supersedes: []
informs: [ALL]
deciders: [founder]
---

# ADR-006 — Агентная операционная модель

## Контекст
Команда из 4 чел. (~4,8 млн ₽) заменяется на соло-фаундера + ИИ-агентов. Референс — ORIION (11 персистентных Opus, CloudEvents, AgentDB), но он токеножорный. Цель: дешевле ORIION по токенам, профильные субагенты, всегда пост-аудит, обновляемая память.

## Решение
**SDD-харнесс**, гибридный рантайм:
- **Рантайм:** native Claude Code субагенты (эфемерные, спавн-под-задачу) + claude-flow MCP (AgentDB) для семантической памяти и cost-телеметрии.
- **Ростер:** 7 core + 4 on-demand (Android-перецеленный). Агенты — нативные одно-файловые `.claude/agents/<role>.md`.
- **Модели:** Opus-дефолт + Sonnet-fallback; pinned-Opus: security/verifier/architect/planner. Экономия токенов — от контекст-дисциплины + эфемерности + T0/T1-выноса механики.
- **Хендофы:** лёгкие MD+YAML (не CloudEvents-36-defs).
- **Память:** общая тегированная `memory/<domain>.md` + AgentDB-рекалл; single-writer memory-curator.
- **Пост-аудит:** phase-close обязателен (8 линз, вкл. live-gold/evidence — ADR-010) + adversarial wave-аудит.
- **Контроль:** автономно до фаза-гейта + независимые автономные сессии; фаундер ревьюит гейты (тиры 1-5).
- **Верификация:** Gradle Managed Devices + Firebase Test Lab + agent-device + 2–3 физ-OEM.

Полностью — [agentic-build-operating-model](../../docs/research/agentic-build-operating-model.md) + [`agent-handbook/`](../agent-handbook/00-START-HERE.md).

## Последствия
- Бюджет смещён с зарплат на токены/инфру/тест-ферму ([`cost-budget.yaml`](../../.claude/agents/_shared/cost-budget.yaml)).
- Фаундер руками: spec/ADR-аппрув, ревью PR на гейтах, физ-OEM sanity, разовый ops-setup, клапан эскалации.
- Нативный риск снимается обратной связью с реального устройства, не наймом кодера.

## Альтернативы (отклонены)
- **Полный claude-flow swarm (ORIION 1:1)** — токеножорный, web-роли не подходят.
- **Ультра-минимум (5 ролей)** — теряет профильность.
- **Найм команды** — против бюджета/модели соло-фаундера.
