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
after Vex replies. The EvolveContext dynamic-counterpart method is rooted here so fresh instances do
not need to know a PR number before finding it:

~~~text
tools/continuity-pressure/README.md
-> DYNAMIC-COUNTERPART-LIVED-PROOF.md
-> scenarios/evolve-context-dynamic-counterpart-v1.json
-> src/jvmTest/kotlin/vexlife/harness/DynamicCounterpart*.kt
-> dynamicCounterpartProof
~~~

Design/proof-method owner: `DYNAMIC-COUNTERPART-LIVED-PROOF.md`.
The first executable prototype is test-only and does not change Product/Foundation semantics.

Current first proof class:

~~~text
DCP-M0 — adaptive method over the existing direct-person route

Vex
  ordinary DirectCompanionTurn
  scratch canonical Home
  host's exact selected Product 4B runtime

Sarah
  proof-owned second persistent mlx-lm 4B process
  same exact already-present model/Python bytes from Product runtime receipt
  separate loopback port
  prompt-cache size 1

execution
  serialized Sarah -> Vex -> Sarah -> Vex
  no simultaneous generation required
~~~

Sarah owns a private synthetic role and bounded experimental purpose. Only her `visibleMessage`
enters Vex's Conversation. Her private observer note never enters Vex's context and is not an
automatic semantic verdict.

Important participant boundary:

~~~text
Sarah harness roleRef
!= canonical Product speaker identity

current direct-person speakerRef
= installation.personRef
= person.local-user on a fresh default Home
~~~

The first scenario deliberately begins before Sarah has introduced her name. This lets the proof
observe whether Vex invents a person identity. A later visible self-introduction is ordinary source
content; it does not retroactively rewrite older canonical speaker history.

DCP-M0 therefore proves the **adaptive test method**, not first-class Product participant identity.
A participant-aware DCP-1B remains held on the relational-custody / participant-provenance pressure
tracked by Recognition Circuit checkpoint #508 and EvolveContext architecture #500.

Three-character testing is also held on two independent gates:

~~~text
runtime gate
  healthy multi-context realization

relationship gate
  Sarah / second counterpart remain distinct Vex-facing participants
  instead of both collapsing to person.local-user
~~~

Do not treat a cache-2 pass as a multi-participant pass.

Experiment entry after source proof:

~~~bash
./gradlew --no-daemon dynamicCounterpartProof --args="\
  --runtime-home $HOME/.vexlife \
  --scenario-file tools/continuity-pressure/scenarios/evolve-context-dynamic-counterpart-v1.json \
  --result-dir build/dynamic-counterpart \
  --source-sha <exact-proof-head> \
  --source-clean true \
  --experiment-window true \
  --runtime-start-at-bind true"
~~~

The host task must run in a dedicated experiment window. Model acquisition and model switching are
not part of this method. The Sarah child process is proof-owned and must be terminated on every
reachable terminal path.

Permanent:

~~~text
COUNTERPART != EVALUATOR
PRIVATE ROLE IDENTITY != PRODUCT SPEAKER IDENTITY
ADAPTIVE PRESSURE != COMPLIANCE STEERING
PROCESS SLOT != CHARACTER
MULTI-CACHE PASS != MULTI-PARTICIPANT PASS
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
