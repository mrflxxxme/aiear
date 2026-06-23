---
id: ADR-003
title: ИИ-стек — один RF-стек (SpeechKit + GigaChat/YandexGPT), отказ от выбора-ИИ в MVP
status: accepted
date: 2026-06-23
supersedes: []
informs: [MVP0, MVP1]
deciders: [founder]
---

# ADR-003 — RF ИИ-стек

## Контекст
PRD v1.0 обещал «выбор любого ИИ (Claude/GPT/Алиса/GigaChat)» в MVP. Claude/GPT недоступны для оплаты из РФ без зарубежного юрлица и хранят данные за рубежом (конфликт с ФЗ-242). Нужен предсказуемый, оплачиваемый из РФ, ru-нативный стек.

## Решение
**Один RF-стек:** STT — **Yandex SpeechKit** (primary, стриминг ru), SaluteSpeech (fallback), Vosk/whisper.cpp (офлайн/free). LLM-структурирование — бенчмарк **GigaChat ↔ YandexGPT**, старт на **GigaChat-2 Lite**. TTS — с V1. Выбор ИИ + intl-модели — **V1.3** (через KZ-юрлицо + прокси, с явным согласием).

## Последствия
- Открытый вопрос Q1 (GigaChat vs YandexGPT) закрывается бенчмарком `evaluator` до MVP-0 F2.
- Себестоимость STT ≈ 39 ₽/час — драйвер квот (ADR-005).
- Ключи — только на backend (ADR-004).

## Альтернативы (отклонены)
- **Claude/GPT в MVP** — нет RF-оплаты, данные за рубежом, ФЗ-242.
- **Мульти-провайдер сразу** — преждевременная сложность; отложено в V1.
