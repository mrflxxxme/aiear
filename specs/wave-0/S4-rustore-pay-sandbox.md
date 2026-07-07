---
spike: WAVE0-S4
title: RuStore Pay SDK sandbox — подписка + рекуррент
status: approved
blocking: true
owner_role: backend-engineer
adr_refs: [ADR-005, ADR-012]
prd_refs: ["§8 Wave 0 S4", "§10.5", "§12"]
---

# S4 — RuStore Pay SDK sandbox: подписка + рекуррентное продление

## Гипотеза
Через RuStore Pay SDK (sandbox) можно оформить подписку, активировать платный статус и провести **рекуррентное продление**. Это фундамент M5 (биллинг). Риск: миграция с BillingClient (выключен 01.08.2026), требования ИП, тонкости рекуррента.

## Acceptance criteria (EARS)
- **S4-AC1** — WHEN тестовый пользователь оформляет подписку в sandbox, THE SYSTEM SHALL активировать платный статус (entitlement) в backend.
- **S4-AC2** — WHEN наступает срок продления, THE SYSTEM SHALL провести рекуррентное продление и обновить срок entitlement.
- **S4-AC3** — IF продление не прошло, THEN THE SYSTEM SHALL перевести подписку в grace/hold-состояние без потери аккаунта.
- **S4-AC4** — WHEN платёж подтверждён, THE SYSTEM SHALL сформировать фискальный чек 54-ФЗ **на стороне эквайера (CloudPayments/YooKassa)** — см. допущение A9 ниже.

## Допущение A9 — фискальный чек (документированное допущение, на ратификацию в PR S4)
Развилка «кто формирует чек» закрыта допущением A9 grill-протокола 2026-07-07: **чек 54-ФЗ формирует эквайер (CloudPayments/YooKassa) на внешнем рельсе; RuStore Pay отвечает только за подписку/рекуррент**. Billing = tripwire (ADR-011 D2) → ратификация фаундером в PR S4 обязательна; если sandbox покажет, что RuStore Pay сам фискализирует — допущение пересматривается в том же PR.

## Метод
1. Backend entitlement-сервис — единый источник для обоих рельсов. **Entitlement привязан к device-token, с миграцией device→account при привязке аккаунта в MVP-1 (ADR-012)**; схема сразу с `owner_id: device|account`.
2. Интеграция RuStore Pay SDK sandbox: покупка → вебхук → entitlement.
3. Эмуляция продления и фейла продления (grace/hold).
4. Проверка идемпотентности вебхуков: **идемпотентный ключ = `purchase_token` RuStore** (повторный вебхук с тем же purchase_token не создаёт второй entitlement/продление).

## Среда
Sandbox RuStore + тестовый эквайер (CloudPayments/YooKassa) для 54-ФЗ-чека. Ключи — только в env/CI-secrets.

## Go / No-Go
- **GO:** AC1+AC2 в sandbox. AC4 — проверить интеграцию чека по допущению A9 (чек на эквайере; подтвердить/опровергнуть sandbox-прогоном).
- Без GO — биллинг MVP-1 под вопросом; эскалация фаундеру (ops: RuStore dev-аккаунт ИП).

## Что решает исход
Разблокирует M5 и всю монетизацию MVP-1.

## Эскалация
Sandbox недоступен / нужен верифицированный ИП-аккаунт → `blocked: external_failure`, ops-задача фаундера.
