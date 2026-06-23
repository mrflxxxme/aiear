---
id: ADR-001
title: Android-first (отклонение iOS-first), РФ-only → рус-Казахстан
status: accepted
date: 2026-06-23
supersedes: []
informs: [WAVE0, MVP0, MVP1, V1]
deciders: [founder]
---

# ADR-001 — Android-first

## Контекст
PRD v1.0 был iOS-first. Ресёрч ([tech-scenarios-findings](../../docs/research/tech-scenarios-findings.md)) показал: на iOS в РФ сломаны платежи (Apple отключил IAP с 01.04.2026), фоновая активация стороннего приложения зарезервирована за Siri, управление приложениями ограничено. Android даёт легальный путь и к фоновому захвату (CDM/mic-FGS), и к приёму денег (RuStore), и к hands-free (роль ассистента, V1).

## Решение
Стартуем **Android-first**. iOS — отдельной волной V1.4. Гео: **РФ-only → рус-Казахстан** fast-follow; локаль `ru`-only.

## Последствия
- Весь MVP-стек — Kotlin/Compose + RuStore + ИП.
- iOS-биллинг откладывается (см. [ios-billing-options](../../docs/research/ios-billing-options.md), reader-app модель) до V1.4.
- Тест-ферма — Android (Firebase Test Lab + физ-OEM).

## Альтернативы (отклонены)
- **iOS-first** — платежи/фон/кнопка в РФ нерабочи.
- **Кросс-платформа сразу** — распыляет соло-ресурс, удваивает нативный риск.
