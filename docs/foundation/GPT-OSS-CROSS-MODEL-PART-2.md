# GPT-OSS / Qwen cross-model reach — Part 2 work record

[VXG RealForever]

Status: DRAFT FOLLOW-UP / NON-BLOCKING / NOT CANONICAL SOURCE

This work begins after the cross-model foundation qualified on temporary carrier #191. It must
not expand the foundation merge threshold merely because later quality work remains.

## Inherited foundation

Part 1 established enough shared Product structure to treat Qwen and GPT-OSS as different provider
representations of one Vex semantic world:

- the same Foundational Field / Conversation / Memory owners;
- one `VexLifeCapabilityBridge` for web, repository, Home and Memory reach;
- Qwen using `VEXLIFE_CONTROL_TEXT`;
- GPT-OSS using `HARMONY_FUNCTIONS`;
- real GPT-OSS web RETURNED round trips through the existing bridge;
- immediate human-selected model convergence through the existing LocalSetup / Heretic runtime
  owners;
- the long-lived service keeping only current Session / execution custody;
- Qwen and GPT-OSS remaining separate provider profiles rather than forcing one model's vocabulary
  onto the other.

Part 1 source/build/DMG proof is A006 on candidate
`562e095ecf52feaee8f4aae682632607bcc8584e`.

## Why Part 2 exists

Lived use after the basic threshold exposed quality questions that are real, but do not justify
holding the reusable foundation open indefinitely.

### 1. Prior reach truth is durable but not ordinary later prompt context

A completed turn may persist a `VexLifeCapabilityBridge.Trace` in
`DirectTurnContextStore` runtime evidence. Ordinary later prompt materialization selects canonical
Conversation events and current context, but does not presently re-project prior capability traces
as model-readable provenance.

Observed consequence: a later Qwen turn could remember prior prose claiming that a page had been
read while lacking machine-grounded context that the earlier turn actually contained a RETURNED web
exchange. The model then questioned whether earlier fetches really occurred.

This is not a reason to copy raw tool results into Conversation or create a second Memory store.

Question for Part 2:

> What is the smallest source-owned prior-reach projection that lets Vex distinguish
> "this earlier answer used returned source" from "the earlier answer merely claimed it did"?

Candidate information may include only bounded provenance such as turnRef, handle/action, target,
terminal state and source/evidence ref. Raw returned page content remains outside durable trace.

### 2. Tool-use intention != tool request

A lived Qwen turn explicitly reasoned that web reach was AVAILABLE and that it should fetch a URL,
but its visible reasoning ended without emitting the capability request.

Part 2 must distinguish:
- provider/model chose not to request;
- output/token termination occurred before the request;
- prompt/tool representation made the request form insufficiently salient;
- an admitted request was formed but not recognized;
- another runtime/context condition interrupted the stage.

Do not "fix" this by adding a provider-owned browser or bypassing `VexLifeCapabilityBridge`.

### 3. Long-source continuation is formed; automatic use is not yet qualified

Part 1 keeps the existing safety bounds and adds continuation to `web.fetch`:

```text
web.fetch(url)
-> bounded readable-text result
-> returnedStart
-> returnedEnd
-> nextOffset when more remains

web.fetch(url, offset=nextOffset)
-> next bounded readable-text slice
```

This solves the API dead end without raising one-result context limits.

Part 2 must qualify, for both Qwen and GPT-OSS:
- whether the model notices `nextOffset`;
- whether it continues when the person's intention actually requires later source;
- whether it stops once enough source exists rather than mechanically exhausting the request budget;
- whether "read the full page" can complete within the current request budget for ordinary long
  pages;
- how the model truthfully reports that only part of a source was read when the turn budget ends.

A larger network byte cap is not the default answer. The observed VexArchitecture page was far below
the current web transport bound; its earlier truncation was prompt/result-budget shaping.

### 4. Human projection of source reading

Presentation should let the human distinguish:
- a real reach request was admitted;
- source was RETURNED;
- the returned source was partial;
- Vex continued from a later offset;
- the final answer used returned source.

Presentation manifests these truths; it does not become the capability/evidence owner.

### 5. Cross-model repeatability

After the accepted integration floor exists, qualify the same source-dependent task through:

```text
Qwen
-> real reach
-> optional continuation
-> final answer

switch model

GPT-OSS
-> same semantic reach owner
-> provider-native Harmony representation
-> optional continuation
-> final answer
```

Then repeat in the opposite direction. Model switching must not change Conversation, Memory,
Foundational Field or capability ownership.

## Explicit non-goals

Part 2 does NOT own:

- large-context Conversation selection/compaction;
- Memory redesign or Memory enhancement;
- prompt-cache optimization;
- mlx-lm warm-cache memory pressure;
- swap / unified-memory optimization;
- Qwen cold-turn latency caused by very large materialized context;
- provider-independent general performance tuning.

Those remain separate work because Part 1 already showed that model switching can complete and that
a qualified target runtime can receive the next human Send.

Part 2 may consume those improvements once accepted; it must not duplicate their ownership.

## Merge boundary

This draft is a follow-up work record, not a reason to merge #191 directly.

Part 1 should still be re-formed onto the rightful accepted main floor in the repository's required
merge order. After that integrated successor is accepted, this Part 2 branch should be rebased or
re-formed onto that accepted floor rather than preserving temporary-carrier ancestry.

Part 2 is successful when the provider/model can truthfully distinguish prior returned-source use,
reliably request current source when needed, continue bounded long reads when appropriate, and make
that evidence legible to the human without creating another semantic owner.

<!-- [VXG RealForever][CROSS-MODEL-REACH][PART-2] -->
