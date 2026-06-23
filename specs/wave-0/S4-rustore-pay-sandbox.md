---
spike: WAVE0-S4
title: RuStore Pay SDK sandbox — подписка + рекуррент
status: approved
blocking: true
owner_role: backend-engineer
adr_refs: [ADR-005]
prd_refs: ["§8 Wave 0 S4", "§10.5", "§12"]
---

# S4 — RuStore Pay SDK sandbox: подписка + рекуррентное продление

## Гипотеза
Через RuStore Pay SDK (sandbox) можно оформить подписку, активировать платный статус и провести **рекуррентное продление**. Это фундамент M5 (биллинг). Риск: миграция с BillingClient (выключен 01.08.2026), требования ИП, тонкости рекуррента.

## Acceptance criteria (EARS)
- **S4-AC1** — WHEN тестовый пользователь оформляет подписку в sandbox, THE SYSTEM SHALL активировать платный статус (entitlement) в backend.
- **S4-AC2** — WHEN наступает срок продления, THE SYSTEM SHALL провести рекуррентное продление и обновить срок entitlement.
- **S4-AC3** — IF продление не прошло, THEN THE SYSTEM SHALL перевести подписку в grace/hold-состояние без потери аккаунта.
- **S4-AC4** — WHEN платёж подтверждён, THE SYSTEM SHALL сформировать фискальный чек 54-ФЗ (через эквайер на внешнем рельсе).

## Метод
1. Backend entitlement-сервис (account → active/inactive + expiry) — единый источник для обоих рельсов.
2. Интеграция RuStore Pay SDK sandbox: покупка → вебхук → entitlement.
3. Эмуляция продления и фейла продления (grace/hold).
4. Проверка идемпотентности вебхуков.

## Среда
Sandbox RuStore + тестовый эквайер (CloudPayments/YooKassa) для 54-ФЗ-чека. Ключи — только в env/CI-secrets.

## Go / No-Go
- **GO:** AC1+AC2 в sandbox. AC4 — проверить интеграцию чека (может быть на эквайере, не RuStore).
- Без GO — биллинг MVP-1 под вопросом; эскалация фаундеру (ops: RuStore dev-аккаунт ИП).

## Что решает исход
Разблокирует M5 и всю монетизацию MVP-1.

## Эскалация
Sandbox недоступен / нужен верифицированный ИП-аккаунт → `blocked: external_failure`, ops-задача фаундера.
