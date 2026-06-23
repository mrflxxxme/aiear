---
id: ADR-002
title: Активация MVP — путь B (CDM/tile), default-ассистент отложен
status: accepted
date: 2026-06-23
supersedes: []
informs: [WAVE0, MVP0]
deciders: [founder]
---

# ADR-002 — Активация: путь B (капчер-first)

## Контекст
Надёжной brand-agnostic «волшебной кнопки» при закрытом приложении на Android нет — самые мощные поверхности (assist-жест, always-on hotword, фоновый микрофон) ОС резервирует за default-ассистентом (`ROLE_ASSISTANT` + `VoiceInteractionService`). Кнопки наушников портативно не ловятся.

## Решение
MVP активируется **путём B**: `CompanionDeviceManager` (автозапуск по BT) + Quick Settings **tile** + кнопка в приложении + media-button hook (бонус, in-session). Фоновый захват — **mic-FGS** (`FOREGROUND_SERVICE_MICROPHONE`). Default-ассистент (путь A) — только V1 «Power Mode».

## Последствия
- Wave 0 спайки S1–S3 валидируют этот путь на реальных OEM.
- wake-word — только на взведённом сервисе пока наушники подключены (ADR-009, S6).

## Запрещено (block на ревью)
- `BOOT_COMPLETED` mic-старт (бан Android 14+).
- AccessibilityService-автоматизация на Google Play (бан с 28.01.2026; возможно только RuStore, под вопросом).
- silent-audio Now Playing хак.

## Альтернативы (отклонены)
- **Default-ассистент в MVP** — сложный онбординг, роль молча сбрасывается при обновлении; отложено в V1.
- **Кнопка наушников как primary** — не доходит до стороннего приложения по большинству брендов.
