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
| `pressure` / `probe` / `quick` / `full` | **HELD**, exit 3 | the live model; this revision does not yet bind a qualified runtime to a scratch Home |

- `scenarios/depth-continuity-v1.json` is a byte copy of the shared canonical scenario. `run.mjs` and
  the adapter refuse any other digest.
- The adapter lives in test scope (`src/jvmTest/kotlin/vexlife/harness/`) and runs through the
  experiment-only Gradle task `continuityPressure`. Nothing it uses ships in the Product.
- **Effective selection budget:** the adapter sets the module-internal
  `DirectCompanionTurn.Hooks.experimentEffectiveInputTokenBudget`. The Product never sets it. It
  replaces only the selection budget. The model window, the 16k output reserve and the 208k hard
  limit stay as they are, and the prompt receipt records it as `experimentEffectiveInputTokenBudget`.
- **Projection assertions** judge whether the source was available to the prompt (resident or
  revisited). They do not judge answer quality, which is a question for the live stage.

Result: `build/continuity-pressure/result.json` (`vexlife.continuity-pressure-result/v1`) and
`continuity-pressure-<experimentRef>.zip`. The ZIP holds the manifest, config, scenario, turns,
residency, capability uses, assertions, summary and `implementation/adapter-report.json`.

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
