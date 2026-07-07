# Autonomy build plan — живой трекер (AIEAR)

Реализует [ADR-011](../../.planning/decisions/ADR-011-autonomous-multiphase-runner.md) (8 решений D1–D8, адаптация ORIION ADR-037). Rails-first: согласие фаундера на auto-merge (D1) обусловлено тем, что усиленные гейты (D2/D3) существуют и зелены **до** активации.

Legend: ✅ done · 🚧 in progress · ⬜ todo · ⏸ deferred (founder-owned)

---

## Блок A — Рельсы (D2/D3) ✅ (эта интеграция)

- ✅ `tripwire.yaml` — 7 категорий D2 (вкл. AIEAR-специфичный `native_background_permissions` + `requires_device_evidence`).
- ✅ `evidence-schema.json` + `verify_evidence.py` + `ci-evidence.yml` — D3 целостность (stdlib-only, non-breaking: нет manifest → exit 0).
- ✅ `classify_tripwire.py` — диф-классификатор (stdlib мини-YAML, v1: любое совпадение = ack; surfacing `device_evidence_required_by`).
- ✅ `premerge_hook.py` — pre-merge хук (fail-closed на unparseable-mention; bare-python classify).
- ✅ ADR-011 + cross-refs (ADR-002/006/010, handbook 06).
- ⏸ **branch protection на `main`** — founder-owned, отложена до активации (см. §Активация).

---

## Блок B — Фронт (D4/D5) ✅

- ✅ `escalation-policy.md` — агент владеет all impl+arch (решает+логирует); эскалирует ТОЛЬКО продукт/рынок + трипвайр (вкл. нативный двойной гейт).
- ✅ `judge-panel.md` — триггер широкого форка + N подходов + рубрика `evaluator` (корректность→безопасность/ФЗ→простота→стоимость→перформанс) + победитель/прививка + evidence.
- ✅ `log_decision.py` + `DECISIONS-LOG.md` — единый decision-log для post-hoc аудита.
- ✅ `/autonomy:discuss` — auto-discuss рутина (D4).

---

## Блок C — Runner (D6/D8) ✅

- ✅ `/autonomy:run` — секвенциальная цепь `discuss → plan → execute → gates → PR → (ack: rails-first pause | auto-merge) → next`, петля до эскалации / ack / stuck-гейта / пустой очереди / budget-cap.
- ✅ RUN-QUEUE машинерия — `run_queue.py` (add/resolve/check-ack/pending; протестировано) + `/autonomy:ack` фаундер-команда.
- ✅ notify — инструкции runner'а: PushNotification на 5 interrupt-событиях + опц. Telegram phone-ack (`notify.json`).
- ✅ pre-merge tripwire **хук** — `premerge_hook.py`. **FOUNDER ACTION вооружить:** мёржни `settings.hook-snippet.json` в `.claude/settings.json` (при активации).
- ✅ **role-prompt loader** — `load_role.py` компонует спавн-промпт из однофайлового `.claude/agents/<role>.md` (протестировано против всех 11 ролей). Runner передаёт его general-purpose `Agent`-спавнам — разрыв «хендбуки vs нативные сабагенты» закрыт без конверта.

---

## Блок D — Self-healing (D7) ✅

- ✅ regression-watch — `check_main_health.py`: последний completed run на gate-workflow (ci-android/backend/security/evidence) → verdict JSON + `offender_sha` (exit 0/20/1; in-progress игнор; PR-only workflow не пинит main).
- ✅ auto-revert + notify — `/autonomy:heal` §2: revert-ветка от свежего main → gated revert-PR → merge → **обязательный RUN-QUEUE `revert` + notify** (фаундер всегда узнаёт о реверте). Attribution sanity-check + conflict-guard → `stuck`, не гадать.
- ✅ автономный fix-loop — `/autonomy:heal` §3: cherry-pick → диагноз из `gh run view --log-failed` → фикс + закрыть gate-gap → гейты + tripwire → merge/ack; max 3 цикла → `stuck`.

---

## Блок E — Параллелизм (D6) ✅

- ✅ opt-in worktree-параллелизм — `run.md` §Parallel tracks: только для доказуемо-независимых фаз (нет общих контекстов / нет dependency-edge / не tripwire-heavy); executor-сабагенты `isolation: "worktree"` + background, max 2 трека; **мёржи всегда секвенциальны** через health-check (один offender за раз для D7).
- ✅ per-run budget accounting — `run.md` §Budget: оценка в `complete`-запись; панели + fix-loop'ы считаются против dev_team caps; деградируй N при тесноте.

---

## Активация auto-merge (D1) — ⏸ ОТЛОЖЕНА (rails-first, решение фаундера)

Условия активации (все три; 3-е добавлено grill 2026-07-07, решение 4.4):
1. ⬜ **Wave 0 спайки S1–S5 зелёные на ≥2 OEM** (главный нативный риск снят реальным device-evidence).
2. ⬜ **CI подключён к secrets** — ci-android (FTL: `GCP_SA_KEY`), ci-backend (live), ci-security.
3. ⬜ **Evidence-контур реально работает** — единый путь `specs/<wave>/evidence/<PHASE>/` (машинный `manifest.json` + `<gate>.json` рядом с человеческим бандлом), машиночитаемый DoD, непустой manifest обязателен для нативных/AI-фаз, `verify_evidence.py --require` фейлит при отсутствии/пустоте манифеста — подтверждено хотя бы одной фазой, прошедшей контур end-to-end.

Затем founder one-time actions:
- ⬜ branch protection (require PR + `ci-android`/`ci-backend`/`ci-security`/`ci-evidence` + linear + enforce_admins + delete-branch-on-merge).
- ⬜ вооружить `premerge_hook` (мёрж `settings.hook-snippet.json`).
- ⬜ (опц.) `notify.json` с telegram_chat_id.
- ⬜ флип: убрать «пауза-на-каждом-PR» из `run.md` step Merge + обновить README-баннер + этот раздел.

Пилот после активации: первая независимая MVP-0 фаза (напр. F1) через `/autonomy:run` — retro → подтянуть tripwire-глобы / escalation-policy, где мисфайрит.

---

## Post-build

- ⬜ pilot retro: на чём runner эскалировал/паузил/хилил → уточнить трипвайр/эскалацию.
- ⬜ v2 tripwire: контентная проверка миграций (как ORIION) — только когда появится backend/migrations и накопится доверие.
