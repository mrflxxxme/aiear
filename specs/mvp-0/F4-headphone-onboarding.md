---
phase: MVP0-F4
title: Онбординг наушников
wave: mvp-0
status: approved
tier: 3
pipeline: android
deps: [WAVE0-S2, WAVE0-S3]
adr_refs: [ADR-002]
ui_spec: true
prd_refs: ["§8 MVP-0 F4", "§10.3"]
---

# MVP0-F4 — Онбординг наушников

## User story
Как новый пользователь, я хочу за минуту настроить свои наушники и понять, как запускать захват, чтобы дальше всё работало «на ходу».

## Scope
- Автоопределение подключённой гарнитуры.
- CDM-сопряжение (one-time) для BT-autostart.
- Тест активации (tile / media-button) с мгновенной обратной связью.
- Сохранение маппинга наушников.
- Гайд по tile (как добавить в шторку).
- Экран согласия на обработку ПД (ФЗ-152).

## Non-scope
- Сам захват (F1). Wake-word онбординг (MVP-0.x, если S6 зелёный).

## Acceptance criteria (EARS)
- **MVP0-F4-AC1** — WHEN пользователь впервые открывает приложение с подключёнными наушниками, THE SYSTEM SHALL определить гарнитуру и предложить настройку.
- **MVP0-F4-AC2** — WHEN пользователь проходит CDM-сопряжение, THE SYSTEM SHALL сохранить ассоциацию для BT-autostart (S2).
- **MVP0-F4-AC3** — WHEN пользователь тестирует активацию, THE SYSTEM SHALL дать немедленную обратную связь (сработало/нет) для tile и media-button.
- **MVP0-F4-AC4** — THE SYSTEM SHALL показать гайд добавления Quick Settings tile.
- **MVP0-F4-AC5** — WHEN пользователь не дал согласие на обработку ПД, THE SYSTEM SHALL не начинать захват.
- **MVP0-F4-AC6** — WHERE OEM требует battery-unrestricted для фона, THE SYSTEM SHALL подсказать снять ограничение (Don't-Kill-My-App, `memory/android-oem.md`).

## Edge-cases / unhappy-path
- AirPods на Android → честно сообщить об ограничениях (только tile-путь, F3-degradation).
- Тач-панель без кнопки → активация через tile/BT-autostart.
- Согласие отозвано позже → остановить захват, не терять уже сохранённое по правилам.

## Test plan
- Instrumented: онбординг-флоу на ≥2 OEM (вкл. Xiaomi для battery-hint).
- Unit: сохранение/чтение маппинга, состояние согласия.
- Screenshot: экраны онбординга (Compose).

## Data / privacy
Экран согласия — обязательный гейт перед первым захватом (ФЗ-152). Текст согласия — `memory/billing-compliance.md` / legal (Q4 для записи встреч — позже).

## Model hints
- CDM-сопряжение/battery-hint — T3 (нативное).
- UI-онбординг — T2 + designer.
