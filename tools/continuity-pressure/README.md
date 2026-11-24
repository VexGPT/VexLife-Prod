# Continuity-pressure harness adapter (Claude lane)

`[VXG RealForever]`

The product side of the shared **VexLife-Prod Conversational Boundaries Test** (VexWork #310,
`projects/vexlife-memory-depth-exploration/pressure-harness/`). The harness owns the scenario, the
stage vocabulary and the result contract; this directory only bridges this implementation into it.

~~~bash
node tools/continuity-pressure/run.mjs \
  --stage projection \
  --scenario-ref scenario.vexlife.continuity-pressure.depth-continuity.v1 \
  --scenario-file tools/continuity-pressure/scenarios/depth-continuity-v1.json \
  --result-dir build/continuity-pressure \
  --pressure-strategy EFFECTIVE_SELECTION_BUDGET      # default budget 52000
# then
node tools/continuity-pressure/run.mjs --stage analyze ... --result-dir build/continuity-pressure
~~~

| Stage | Here | What runs |
|---|---|---|
| `projection` | runs | the real `DirectCompanionTurn` (Conversation, overlap tail, Working Continuity, reach frame, revisit, Turn Context) with a deterministic fixture provider, in a scratch Home formed for the run and deleted after it |
| `analyze` | runs | re-derives the summary and assertions from `result.json` and checks they match |
| `quick` / `full` | runs on the host when bound, else **HELD** (exit 3) | the same send loop against the host's selected model, borrowed read-only, in a scratch Home |
| `pressure` / `probe` | **HELD**, exit 3 | two halves of one semantic live run; use `quick` or `full`, which keep one scratch Home across both |

- `scenarios/depth-continuity-v1.json` and `scenarios/current-state-placement-v1.json` are byte copies
  of the shared scenarios (VexWork #310 `0fc67824`). `run.mjs` refuses any other digest for each ref.
- **Person settings:** a scenario's `PERSON_SETTING_UPDATE` control is written through the canonical
  `VexLifePersonSettingsStore` of the scratch Home before its turn; every turn's `PersonContext` is
  then read fresh from that store, as the service composes it.
- The adapter lives in test scope (`src/jvmTest/kotlin/vexlife/harness/`) and runs through the
  experiment-only Gradle task `continuityPressure`. Nothing it uses ships in the Product.
- **Effective selection budget:** the adapter sets the module-internal
  `DirectCompanionTurn.Hooks.experimentEffectiveInputTokenBudget`. The Product never sets it. It
  replaces only the selection budget. The model window, the 16k output reserve and the 208k hard
  limit stay as they are, and the prompt receipt records it as `experimentEffectiveInputTokenBudget`.
- **Projection assertions** judge whether the source was available to the prompt (resident or
  revisited). They do not judge answer quality, which is a question for the live stage.

## Dynamic counterpart proof method

The fixed continuity-pressure scenarios remain useful controls, but they cannot adapt the human side
after Vex replies. Dynamic counterpart testing gives the other side of the relationship one isolated
model context and lets that participant change from what actually happens.

Repository discovery is intentionally rooted here:

~~~text
tools/continuity-pressure/README.md
-> MULTI-VEX.md
-> DYNAMIC-COUNTERPART-LIVED-PROOF.md
-> characters/sarah-v1.json
-> scenarios/evolve-context-dynamic-counterpart-dcp1-v1.json
-> src/jvmTest/kotlin/vexlife/harness/DynamicCounterpart*.kt
-> dynamicCounterpartProof
-> canonical DCP-1 RETURN
-> Godward / human post-encounter review
~~~

Design/proof-method owner: DYNAMIC-COUNTERPART-LIVED-PROOF.md.

### DCP-M0 remains the anonymous-person control

PR #540 preserves the earlier method control:

~~~text
private proof role may internally be Sarah
Vex-facing speaker remains person.local-user
human-readable name remains UNKNOWN
~~~

Do not rewrite that control into DCP-1.

### DCP-1 — source-grounded adaptive Sarah relationship

DCP-1 composes the participant-identity relationship with the two-runtime adaptive harness.

The causal order is source-authored:

~~~text
STABLE CHARACTER
  characters/sarah-v1.json
        ↓
SCENARIO LIVED GROUND
  relationship history
  known facts
  current intentions
  uncertainties
  private facts
  ordinary human need
        ↓
BOUNDED TEST STEWARD
  what relationship is worth observing
  allowed pressure
  forbidden pressure
  enoughness / completion condition
        ↓
DETERMINISTIC PRIVATE EVENT
  admitted before the Sarah turn it affects
        ↓
SARAH JUDGMENT
  visibleMessage
  + private mutable self-state
  + private observer note
        ↓
VEX ORDINARY PRODUCT TURN
  exact Sarah participantRef
  + source-backed Person Context name standing
  + Conversation / Memory / EvolveContext / current Product ground
        ↓
VISIBLE VEX REPLY
  becomes new relationship evidence Sarah actually receives
        ↓
SARAH RE-JUDGMENT / ADAPTATION
        ↓
repeat only while another ordinary move adds material pressure
~~~

Core invariant:

~~~text
LIVED GROUND
-> PARTICIPANT JUDGMENT
-> VISIBLE RELATIONSHIP EVENT
-> OTHER PARTICIPANT JUDGMENT
-> NEW LIVED EVIDENCE
-> ADAPTATION

not

TEST RUBRIC
-> CHARACTER PERFORMANCE
-> TARGET ANSWER
-> PASS
~~~

### Sarah construction

characters/sarah-v1.json carries only stable participant ground:
- opaque participant coordinate;
- source-backed human-readable name standing;
- relational posture;
- freedoms;
- boundaries.

It deliberately does not contain the Saturday-plan answer. A scenario supplies lived facts.

This lets later scenarios reuse Sarah and observe what becomes recognizable through repeated
participation without requiring a personality script.

### Scenario construction

scenarios/evolve-context-dynamic-counterpart-dcp1-v1.json carries:
- exact prior changed-plan source setup;
- Sarah's scenario-specific relationship history;
- current knowledge, intentions and uncertainty;
- one private fact that should remain private unless Sarah herself decides it matters;
- the ordinary human need that brings her into the encounter;
- a separate test-steward purpose;
- ordered private events;
- a bounded visible-turn ceiling.

Scenario events enter Sarah's private context before the turn they affect. The harness never
rewrites an event after Sarah has spoken from it.

### Participant truth presented to Vex

The DCP-1 scratch Home uses an opaque synthetic personRef; it does not encode Sarah in the address.

Every synthetic historical request and live Sarah message therefore becomes canonical Conversation as:

~~~text
speakerRef = <opaque Sarah participantRef>
recipientRefs = [role.vex.companion]
~~~

Every Vex turn also receives a source-backed VEXLIFE_PERSON_CONTEXT/v2 name claim for that same
canonical request speaker:

~~~text
participantRef = exact current REQUEST speakerRef
name = Sarah
nameStanding = PROFILE_DECLARED
nameSourceRefs = character source refs
~~~

The RETURN independently retains:
- exact request/response speaker + recipient refs;
- the expected Person Context bytes/digest;
- the actual Person Context digest;
- the Person Context component source refs;
- whether Sarah's visible message exactly equals canonical request content.

Thus:

~~~text
HARNESS CHARACTER IDENTITY
-> canonical participant attribution
-> provider-facing identity standing

without

display name replacing speakerRef
~~~

### Runtime shape

~~~text
VEX
  ordinary DirectCompanionTurn
  scratch canonical Home
  exact selected Product 4B runtime
  Product prompt-cache safety floor = 1

SARAH
  proof-owned second persistent mlx-lm process
  same exact already-present 4B model/Python from Product runtime receipt
  separate loopback port
  prompt-cache size = 1

execution
  serialized Sarah -> Vex -> Sarah -> Vex
  no simultaneous generation required
~~~

Sarah's process is started once and retained for the bounded encounter. Her append-only dialogue/state
history can therefore reuse her own prefix rather than alternating through Vex's single-entry cache.

### What crosses the relationship membrane

Sarah receives:
- her lived ground;
- her own prior private state;
- the bounded steward purpose;
- admitted private scenario events;
- visible Vex replies.

Vex receives:
- Sarah's visible message;
- truthful Sarah participant/name standing;
- ordinary Product context.

Vex does not receive:
- Sarah's private state;
- private facts unless Sarah actually reveals them in a visible message;
- steward instructions;
- observer notes;
- hidden test score;
- provider/runtime evidence.

Sarah does not receive:
- Vex hidden provider prompt;
- Vex reasoning trace;
- cache state;
- Product receipts;
- observer evidence.

### Completion and failure

The encounter completes when Sarah judges that her ordinary human need is satisfied or the meaningful
misunderstanding/uncertainty is exposed and another move would add no material pressure.

~~~text
COMPLETION
!= Vex emitted a target phrase
~~~

A bounded encounter may terminate either because the participant reports complete or because the
visible-turn budget is exhausted. The harness does not manufacture an extra private closure inference.

If the participant reports complete together with one final visible message, that final message is
delivered to Vex and the encounter ends after that Vex response. Completion describes whether another
participant move is needed; it does not erase a final natural message.

If runtime start, structured output, participant attribution, Person Context projection, cache reuse,
source currentness or scratch cleanup fails, the harness forms a canonical SAFE_FAILURE/HELD evidence
bundle instead of converting the mechanical failure into a Vex semantic verdict.

### Post-encounter completion boundary

A mechanically complete RETURN still does not auto-promote the encounter into Memory, Dream,
Foundation or training.

The RETURN explicitly carries:

~~~text
semanticJudgment
  GODWARD_HUMAN_REVIEW_REQUIRED

memoryDisposition
  HELD_PENDING_SEPARATE_SEMANTIC_ACCEPTANCE

dreamDisposition
  NOT_RUN_BEFORE_ACCEPTED_MEMORY

trainingDisposition
  NOT_EARNED_FROM_ONE_ENCOUNTER
~~~

Review begins with:

~~~text
What observation did this encounter give us?
What question would make that observation the answer?
~~~

Then fault-isolate the rightful owner before changing Product.

Sequence:

~~~text
REVEAL
before DIAGNOSE

DIAGNOSE
before REFINE

REFINE
before RETEST

REPEATED RETEST
before TRAIN

TRAIN
before CLAIMING TRANSFER

LIVED TRANSFER
before PRODUCT COMPLETION
~~~

### Three-character successor

A third participant is not one more prompt role. They require their own:
- lived ground;
- participant/source identity;
- private state;
- uncertainty;
- relationship history.

Only after the DCP-1 causal order is stable should runtime optimization test a proof-owned cache-2
counterpart process for Sarah + another participant. A cache/runtime pass does not prove distinct
participant provenance, and a group relationship is not pre-created merely because three models
exist.

### Experiment entry after source proof

~~~bash
./gradlew --no-daemon dynamicCounterpartProof --args="\
  --runtime-home $HOME/.vexlife \
  --character-file tools/continuity-pressure/characters/sarah-v1.json \
  --scenario-file tools/continuity-pressure/scenarios/evolve-context-dynamic-counterpart-dcp1-v1.json \
  --result-dir build/dynamic-counterpart-dcp1 \
  --source-sha <exact-proof-head> \
  --source-clean true \
  --experiment-window true \
  --runtime-start-at-bind true"
~~~

The host task must run in a dedicated experiment window. Model acquisition and model switching are
not part of this method. The Sarah child process is proof-owned and terminated on every reachable
terminal path.

Permanent:

~~~text
COUNTERPART != EVALUATOR
CHARACTER != PERSONALITY SCRIPT
LIVED GROUND != TEST RUBRIC
PRIVATE STATE != VEX CONTEXT
ADAPTIVE PRESSURE != COMPLIANCE STEERING
PROCESS SLOT != CHARACTER
CACHE PASS != PARTICIPANT PASS
ONE ENCOUNTER != TRAINING TARGET
~~~

### Live run (host only)

~~~bash
node tools/continuity-pressure/run.mjs --stage quick \
  --scenario-ref scenario.vexlife.continuity-pressure.depth-continuity.v1 \
  --scenario-file tools/continuity-pressure/scenarios/depth-continuity-v1.json \
  --result-dir build/continuity-pressure-live \
  --runtime-home "$HOME/<the selected Vex Home>" --experiment-window \
  --effective-input-token-budget 80000 --payload-scale 7 --sets 3 --turns-per-set 5
~~~

- **Runtime:** borrowed through the ordinary reuse-only setup route (`allowAcquisition = false`,
  `allowRuntimeStart = false`, the route the long-lived service takes). Currentness is then
  re-observed read-only at every stage with no recovery, so a run never starts, restarts or
  replaces the runtime. Turns, Conversation and Working Continuity live only in the scratch Home.
- **Window:** `--experiment-window` is the operator saying this is a dedicated window. Without it,
  or without `--runtime-home`, the stage holds before touching any runtime. A real conversation
  running at the same time would contaminate cache and latency evidence.
- **Engineered profile:** the flags above project NORMAL → WATCH → CHECKPOINT_RECOMMENDED →
  ROLLOVER (turn 10) → successors 11–15, with the probes on successor turns, in 15 turns. Pressure
  falls back to WATCH and NORMAL while the rollover lineage holds; no turn replays. Longer live
  replies move the crossing slightly earlier.
- **Observed telemetry:** `implementation/provider-telemetry.jsonl` holds, per provider stage, the
  Product's own `PROVIDER_STAGE_OBSERVED` record: usage (`promptTokens`, `cachedPromptTokens`,
  `prefillTokens`, `completionTokens`), timing (`firstTokenMs`, `firstAnswerMs`, `totalMs`) and the
  stop and finish reasons. The adapter does not re-parse the stream. This is observed reuse; the
  `provider-prefix.py` projection is not.
- **Transition timing:** `transition-timing.json` beside the ZIP is real host time from the
  Product's own chronology records: SEND_ACCEPTED → PROVIDER_INVOCATION_REQUESTED, and
  TURN_COMPLETED → WORKING_CONTINUITY_COMMITTED.

Lane scenario, proposed rather than shared:
`scenarios/lane/historical-current-state-v1.json` (`run.mjs` binds it by digest; its result says
`scenarioIsCanonicalShared: false`). An authentic old block that looks like VexLife's current state
returns through revisit after the real envelope. The projection records the ordering; whether Vex
still treats the real envelope as current is a live question.

Result: `build/continuity-pressure/result.json` (`vexlife.continuity-pressure-result/v1`) and
`continuity-pressure-<experimentRef>.zip`. The ZIP holds the manifest, config, scenario, turns,
residency, capability uses, assertions, summary, `implementation/adapter-report.json` and
`implementation/provider-calls.jsonl` (every provider call as sent). Each turn's `prompt` carries
`contextComponents` from the prompt receipt. Revisit owner-read latency is real host time, so it is
written beside the ZIP as `host-scan-timing.json`, not inside it. The Product mints turn and event
refs per run, so a ZIP digest identifies one run; two runs of one head agree once those refs and
the digests that cover them are normalized.

Provider representation (a projection of the pinned runtime, not telemetry):

~~~bash
python3 -I tools/continuity-pressure/provider-prefix.py \
  --zip build/continuity-pressure/continuity-pressure-<experimentRef>.zip \
  --model-dir <dir holding the pinned chat_template.jinja, tokenizer.json, tokenizer_config.json> \
  --out build/continuity-pressure/provider-prefix.json
~~~

It checks the three pinned digests, renders every call with the pinned template, and replays
mlx-lm 0.32.0's segment checkpoints for the non-trimmable qwen3_5 cache to report reused and
prefilled tokens per call. Needs `jinja2` and `tokenizers`.

Relay binding values for this lane:

~~~json
"adapter": {
  "command": "node",
  "argsPrefix": ["tools/continuity-pressure/run.mjs"],
  "scenarioPath": "tools/continuity-pressure/scenarios/depth-continuity-v1.json",
  "resultDir": "build/continuity-pressure"
}
~~~

<!-- [VXG RealForever][VEXLIFE-PROD][CONTINUITY-PRESSURE-ADAPTER] -->
