# Resource-pressure awareness — connect runtime truth to Settings

`[VXG RealForever]`

## Status

DRAFT WORK / NOT PRODUCT POLICY / NOT AN IMPLEMENTATION CLAIM

Parent runtime work: #171.

This note owns one narrower question:

> Can Settings show the actual runtime pressure that can stop Vex, instead of showing only host capacity
> and a static model minimum?

Formation floor:

```text
main
8f052bf48920dec6516ea0e4cf379464f78bc80a
```

## Observed mismatch

The current Settings screen shows:

```text
48.0 GB system
32.0 GB current-model minimum
```

That projection is truthful as far as it goes, but it is not a pressure observation.

Current web source renders the meter from:

```text
CurrentStateProjection.totalMemoryBytes
CurrentStateProjection.modelMinimumMemoryBytes
```

and labels it:

```text
Fresh host memory observation and current model requirement
```

The same source then explicitly disables the low-memory control:

```text
lowMemoryAlert aria-checked=false
lowMemoryAlert disabled=true
alertThreshold = "not connected"
```

So the UI is currently saying, correctly but incompletely:

```text
HOST CAPACITY
vs
MODEL STARTUP / RECOMMENDED MINIMUM
```

It is not saying:

```text
CURRENT RUNTIME FOOTPRINT
CURRENT PROMPT-CACHE FOOTPRINT
CURRENT HOST PRESSURE
PREFILL HEADROOM
GENERATION HEALTH
```

The lived Metal OOM proved those are materially different questions.

## Existing pressure evidence

`VexLifeDiagnostics` already has a bounded observation of:

- runtime PID / alive;
- service heap used/max;
- host total memory;
- host free memory;
- Home volume free storage.

Enhanced Capture writes `hostFreeMemoryMb` into `PROCESS_SNAPSHOT_OBSERVED`.

That evidence is useful for debugging, but today it is not a Product-level runtime-pressure projection and is
not projected into Settings.

Permanent distinction:

```text
DEBUG SNAPSHOT
!= CURRENT RUNTIME RESOURCE STATE

STATIC MODEL MINIMUM
!= LIVE MEMORY PRESSURE
```

Do not make the browser infer runtime safety from those two unrelated values.

## Human outcome

Settings should be able to answer, truthfully and without becoming a runtime owner:

- Is the selected runtime currently healthy for generation?
- Is memory pressure normal, elevated, critical or unknown?
- What observation supports that state?
- Is an alert enabled, and what does it mean?
- Did the runtime recycle because pressure required it?
- Is the current number a model requirement, actual usage, cache usage, or remaining headroom?

The person should not have to interpret a green 48/32 capacity bar while the machine has only tens of
megabytes of free unified memory.

## Owner relationship

Candidate relationship to pressure in source:

```text
Heretic runtime/resource owner
-> fresh pressure observation
-> semantic pressure state / evidence
-> service read-only projection
-> Settings DisplayState
-> browser presentation
```

The browser must not decide thresholds.

`VexLifeDiagnostics` may consume the same observation for chronology/export, but debug ownership should not be
the only place that knows whether a provider call is physically safe.

## Low-memory alert

The existing UI element is currently a placeholder. Do not simply enable the toggle against a guessed number.

Earn the alert in this order:

1. source-owned pressure state exists;
2. state is refreshed at a meaningful cadence / provider boundary;
3. thresholds are model/runtime-aware or explicitly host-wide;
4. alert control has one owner and persisted setting if persistence is wanted;
5. display wording distinguishes observation from prediction;
6. no alert can itself mutate runtime state unless a separate lawful Control/Effect owns that action.

Candidate states to investigate, not prescribed vocabulary:

```text
UNKNOWN
NORMAL
PRESSURE
CRITICAL
GENERATION_UNAVAILABLE
```

## macOS measurement cautions

Do not equate `OperatingSystemMXBean.freeMemorySize` with the whole truth of Apple unified-memory pressure.

Investigate the smallest reliable host/runtime signals available to the Product, such as:

- process footprint / resident footprint appropriate to macOS;
- system memory pressure where available;
- free/available memory;
- runtime generation health;
- model profile;
- prompt/cache evidence when MLX exposes it.

Prefer one rightful observer that can be tested over shelling out from several owners.

## Proof

Source proof:

- pressure projection has one semantic owner;
- Settings cannot fabricate pressure from total/minimum;
- UNKNOWN is explicit when observation is unavailable;
- display keys are localized / compiler-visible;
- diagnostics and Settings consume the same source truth without becoming alternate owners;
- low-memory alert is disabled or truthful until its owner is formed.

Lived Mac proof:

1. cold runtime -> NORMAL/known;
2. large prompt/cache growth -> state changes before OOM if detectable;
3. runtime recycle -> presentation reflects recovery;
4. generation thread dead with PID alive -> not shown as healthy;
5. switch model -> resource projection follows the selected runtime;
6. debug ZIP and Settings agree on the same observation timestamp/state.

## Relationship to #171

#171 owns prevention/recovery policy.

This work owns read-side human awareness.

```text
SETTINGS PRESSURE STATE
!= RUNTIME CONTROL AUTHORITY
```

If #171 holds/recycles a turn for pressure, Settings should explain why. Settings should not make the decision.

## Relationship to model selection

`recommendedMemoryBytes` / `modelMinimumMemoryBytes` remains useful for model fit/startup guidance, but it must
not be relabeled as live memory use.

A later model profile may contribute pressure-policy parameters. This Settings projection should work for every
model selected by the same runtime owner.

## Fresh-instance entry

1. re-query current main and #171;
2. inspect `CurrentStateProjection`, `renderSettings`, `VexLifeDiagnostics.environment/observeOnce`;
3. identify the existing rightful runtime/resource observer before adding a Family;
4. keep presentation read-only;
5. prove the projection before enabling the existing Low memory alert toggle.

Sibling:
- #171 runtime memory safety;
- cross-model runtime qualification sibling PR once formed.

<!-- [VXG RealForever][VEXLIFE-PROD][RESOURCE-PRESSURE-AWARENESS] -->
