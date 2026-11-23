# Heretic effective execution topology

[VXG RealForever]

## Human question

Can a human or AI inspect the local Companion's effective model execution configuration without
rediscovering values from three different layers of Product source plus the pinned model/runtime
dependency?

The topology answers:

~~~text
what value is effective now
+ who/source owns that value
+ what boundary must be crossed for it to change
~~~

It does not become a new configuration authority.

## Ownership

~~~text
pinned model config.json
  -> model architecture truth

HereticCompanionRepresentation
  -> provider/message/reasoning/generation relationship

DirectPromptContext
VexLifeMemoryContract
VexLifeCapabilityBridge
  -> Product prompt / continuity ceilings and reserves

HereticRuntimeConfiguration
FileHereticRuntimeConfigurationStore
  -> Product-owned runtime configuration and explicit per-Home choices

HereticPersistentRuntime
  -> consumes the effective launch configuration and owns exact runtime lifecycle

exact pinned mlx-lm source
  -> inherited runtime defaults

HereticExecutionTopology
  -> read-only effective projection of those truths
  != new owner
~~~

## Why this exists

The lived 2026-11-17 continuation run made the missing visibility concrete.

One human turn reached five provider stages and four GitHub reads while the same runtime stayed alive.
Later stages carried roughly 66k-75k prompt tokens but reused only about 2.7k cached prompt tokens, so
most of each stage was re-prefilled. Enhanced diagnostics also observed very low free host memory
during the run.

That evidence does not establish a new tuning policy. It establishes that future tuning must not begin
by rediscovering hidden defaults or confusing model architecture with Product continuity policy.

## Configuration classes

### Model artifact

Examples:

~~~text
model type
native context length
hidden-layer count
full-attention interval
attention / KV heads
head dimension
use_cache
~~~

These values are read from the exact pinned config.json artifact member. When that artifact
provides an explicit layer_types sequence, the topology counts full/linear layers from that sequence
and validates it against num_hidden_layers and full_attention_interval rather than reconstructing a
second layer layout.

Change boundary:

~~~text
MODEL_ARTIFACT_CHANGE
~~~

In particular:

~~~text
full_attention_interval
!= runtime continuity knob
~~~

For Qwen3.5/3.8 under pinned mlx-lm, that value participates in layer construction: it determines
which layers instantiate GatedDeltaNet versus full Attention and therefore which cache type each layer
uses. Changing it is a different model/checkpoint architecture question, not a host-pressure reaction.

### Product source

Examples:

~~~text
generation sampling/profile relationship
Product output-token custody
input-token ceiling
capability-exchange reserve
Memory token cap
hard input+output ceiling
maximum capability requests per turn
prompt-cache recommendation / safety floor
~~~

These values remain owned by their existing Product sources. An explicit per-Home prompt-cache
choice is instead owned by `FileHereticRuntimeConfigurationStore`. "Use Recommended Configuration"
writes today's Product recommendation as one explicit Home value; it does not subscribe that Home to
future recommendation changes. Predecessor candidate `RECOMMENDED` records remain readable for
compatibility. The topology reads those owners and does not become another configuration authority.

### Pinned mlx-lm defaults

The Product pins mlx-lm 0.32.0. Some effective runtime values are inherited because VexLife does not
pass an overriding server argument.

The topology currently makes these defaults explicit:

~~~text
prefill step size      2048
prompt concurrency        8
decode concurrency       32
KV bits                 none
KV group size             64  (dormant while KV quantization is off)
quantized KV start      5000  (dormant while KV quantization is off)
prompt-cache bytes      none
~~~

prompt-cache-size is different:

~~~text
mlx-lm 0.32.0 default = 10

no explicit Home choice
  VexLife effective = current Product recommendation (1 today)
  provenance        = PRODUCT_SOURCE

explicit Home choice, including "Use Recommended Configuration"
  VexLife effective = admitted per-model value
  provenance        = PRODUCT_HOME_CONFIGURATION

legacy predecessor candidate mode = RECOMMENDED
  VexLife effective = current Product recommendation
  provenance        = PRODUCT_SOURCE

change boundary for the running mlx-lm process
  RUNTIME_RESTART
~~~

The projection fails closed if the pinned mlx-lm version changes without the default map being
re-audited.

### Candidate append-only Conversation cache boundary

Prompt-cache capacity and prompt semantic ownership remain separate.

~~~text
DirectPromptContext
  -> owns provider message order
  -> records selected Conversation as APPEND_ONLY_HISTORY
  -> records the exact provider-message coordinate where that history ends

DirectCompanionConnection
  -> may carry that existing coordinate as provider-execution metadata

HereticPersistentRuntime / pinned mlx-lm adapter
  -> may realize a cache segment at that coordinate
  != prompt owner
  != Conversation owner
~~~

The current source candidate is deliberately fail-soft for the optimization:

~~~text
exact admitted Qwen/MLX text-control representation
+ exact pinned mlx-lm server source
+ boundary ends one assistant message before the later user/current tail
+ rendering messages through the boundary is an exact token prefix of the full prompt
  -> cache system + append-only Conversation as intermediate segments

missing / unsupported / token-prefix mismatch
  -> keep ordinary mlx-lm segmentation
  -> human Send remains lawful
~~~

No model-visible message, template argument, Conversation event, Memory value or current-state value
is changed to make cache reuse work. The mutable current-state envelope and current request remain
after selected Conversation as owned by `DirectPromptContext`; the provider adapter changes only
where an already-identical token sequence may be snapshotted.

With the Product safety floor of one entry, the candidate still retains only the system cache. An
explicit two-entry experiment is expected to permit system + append-only Conversation retention.
That expectation is a lived-proof predicate, not a recommendation or default change.

Permanent:

~~~text
PROMPT SEMANTICS / ORDER
!= PROVIDER CACHE SEGMENTATION

CACHED PREFIX
!= CURRENT CONTEXT AUTHORITY

CACHE HIT
!= PERMISSION TO RETAIN STALE FOUNDATION / MEMORY / CONFIGURATION
~~~

## Effective field vocabulary

Every projected field carries:

~~~text
value
provenance
changeBoundary
sourceRef
inheritedDefault / inheritedDefaultSourceRef when applicable
ACTIVE or DORMANT
~~~

Current provenance:

~~~text
MODEL_ARTIFACT
PRODUCT_SOURCE
PRODUCT_HOME_CONFIGURATION
PROVIDER_RECOMMENDATION
PINNED_MLX_LM_DEFAULT
~~~

Current change boundaries:

~~~text
MODEL_ARTIFACT_CHANGE
PRODUCT_SOURCE_CHANGE
RUNTIME_RESTART
~~~

## Continuity distinction

The current Product exposes ceilings and reserves, not an earned adaptive working target:

~~~text
inputTokenBudget                 192000
capabilityExchangeReserveTokens   24000
directTurnInputWithReachTokens   168000
maximumMemoryTokens              100000
reservedOutputTokens              16000
hardInputOutputTokenLimit        208000

adaptiveContextPolicyState
NOT_CONFIGURED
~~~

Permanent distinction:

~~~text
NATIVE MODEL CAPACITY
!= PRODUCT HARD CEILING
!= ACTIVE CONTEXT TARGET
!= VERBATIM CONVERSATION REPLAY
!= WORKING CONTINUITY / MEMORY
~~~

A later behavioral owner may form resource-aware context targets, pressure thresholds and rollover
policy only from lived evidence. This topology intentionally invents none.

## Exact-config observation

HereticExecutionTopology.observe(...) verifies the model's exact pinned config.json member before
projecting architecture and reads the existing Product/Home runtime-configuration owner before
projecting the effective prompt-cache value.

Those reads are observation only. The narrow config.json hash does not replace whole-model artifact
custody or runtime qualification, and the topology never writes runtime configuration.

## Explicitly held

This foundation does not:

- change full_attention_interval;
- change prompt/context budgets;
- turn on KV quantization;
- change prefill step size or concurrency;
- create adaptive memory-pressure thresholds;
- restart/switch a model;
- mutate Home or model artifacts;
- change Conversation, Memory, Working Continuity, Capability Bridge or provider behavior.

Those are behavioral successors and require proof matching their own claims.

<!-- [VXG RealForever][VEXLIFE-PROD][HERETIC-EXECUTION-TOPOLOGY] -->
