# Autonomy decisions-log

> Append-only. Every agent-owned fork the autonomous runner resolved without asking the founder (ADR-011 D4). The founder's post-hoc audit trail. Architectural entries also have an ADR (see `ADR-refs`). Written by `scripts/autonomy/log_decision.py`.

### 2026-07-07T12:01:00Z | phase GRILL-2026-07-07 | arch
- Fork: A1: протокол STT-стриминга (S5/F1) — WebSocket vs gRPC
- Decision: WebSocket default; gRPC — только если S5 покажет, что WS не держит p95<2с
- Rationale: Проще прокси и клиент; S5 спроектирован это проверить
- Reversibility: reversible
- Session: GRILL-2026-07-07-project-docs

### 2026-07-07T12:02:00Z | phase GRILL-2026-07-07 | arch
- Fork: A2: on-device STT движок (F6)
- Decision: Vosk (ru, без внешнего ключа); whisper.cpp — fallback при провале качества
- Rationale: Нулевые внешние зависимости для free-тира
- Reversibility: reversible
- Session: GRILL-2026-07-07-project-docs

### 2026-07-07T12:03:00Z | phase GRILL-2026-07-07 | impl
- Fork: A3: wake-word движок (S6)
- Decision: Vosk-keyword сначала; Porcupine — только после добавления Picovoice-ключа в env
- Rationale: Не вводить новый секрет без нужды; wake-word теперь «Эй, бадди»
- Reversibility: reversible
- Session: GRILL-2026-07-07-project-docs

### 2026-07-07T12:04:00Z | phase GRILL-2026-07-07 | arch
- Fork: A4: единый API-контракт app↔backend
- Decision: specs/_contracts/openapi.yaml + thought.schema.json — канонические; спеки ссылаются, не дублируют
- Rationale: Иначе рассинхрон Android↔backend на fullstack-фазах
- Reversibility: reversible
- Session: GRILL-2026-07-07-project-docs

### 2026-07-07T12:05:00Z | phase GRILL-2026-07-07 | arch | ADR-011
- Fork: A5: две несовместимые системы evidence (specs/<wave>/evidence/<PHASE>/ vs корневой evidence/)
- Decision: Единый путь: машинные manifest.json + <gate>.json живут ВНУТРИ specs/<wave>/evidence/<PHASE>/; verify_evidence.py — discovery по specs/*/evidence/*/manifest.json + --require для native/AI-фаз
- Rationale: Свести две системы evidence в одну (аудит методологии, несостыковка №1); поправка в ADR-011
- Reversibility: reversible
- Session: GRILL-2026-07-07-project-docs

### 2026-07-07T12:06:00Z | phase GRILL-2026-07-07 | impl
- Fork: A6: выбор «что дальше» runner'ом — проза STATUS vs машиночитаемый бэклог
- Decision: .planning/roadmap/phase-queue.yaml (id/deps/status/spec) — единственный источник очереди; status пишет memory-curator на phase-close
- Rationale: Убрать LLM-эвристику по прозе STATUS (несостыковка №6)
- Reversibility: reversible
- Session: GRILL-2026-07-07-project-docs

### 2026-07-07T12:07:00Z | phase GRILL-2026-07-07 | impl
- Fork: A7: гейт «спека одобрена фаундером» под runner
- Decision: Поле status: approved во фронтматтере спеки = машинный гейт; runner НЕ стартует фазу с draft-спекой (эскалация вместо старта)
- Rationale: Несостыковка №5 аудита методологии: неявный ручной шаг → машинная проверка
- Reversibility: reversible
- Session: GRILL-2026-07-07-project-docs

### 2026-07-07T12:08:00Z | phase GRILL-2026-07-07 | impl
- Fork: A8: 4 несовместимых конвенции имён веток
- Decision: claude/<slug> (фактическая harness-конвенция) канонизируется; phase/* и wave/* — deprecated (историческая запись)
- Rationale: Одна фактическая конвенция вместо четырёх; conventions §7 + handbook 06
- Reversibility: reversible
- Session: GRILL-2026-07-07-project-docs

### 2026-07-07T12:09:00Z | phase GRILL-2026-07-07 | impl
- Fork: A9: фискальный чек 54-ФЗ в S4 (billing = tripwire)
- Decision: Документированное допущение: чек — на стороне эквайера (CloudPayments/YooKassa); RuStore Pay — только подписка/рекуррент
- Rationale: На ратификацию founder-ack'ом в PR S4 (tripwire billing_money)
- Reversibility: reversible
- Session: GRILL-2026-07-07-project-docs
