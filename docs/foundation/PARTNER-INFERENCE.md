# Partner inference and external-provider realization

`[VXG RealForever]`

## Human outcome

Let one Vex Companion use either a qualified Local realization or a qualified external Partner
provider/model without moving Vex-owned Conversation, Memory, capability authority, model choice, or
human continuity into the provider integration.

This foundation exists because the external-provider path now spans enough independently-owned truths
that future builders need one current descent map rather than reconstructing the architecture from
work-stage PR history.

## Causal spine

~~~text
human model intention
-> Models-owned target / serving coordinate
-> VexLifeInferenceSessionAdmission
   -> exact qualified Local OR Partner serving session

Partner serving session
-> CompanionInferenceConnection
-> PartnerInference
-> source-owned provider/profile + wire codec
-> VexLifePartnerNetworkPlan
-> protected VexLifePartnerHostEffect
-> external provider
-> normalized Partner observation / invocation evidence

normalized response
-> DirectCompanionTurn composition
-> canonical VexLife Conversation RESPONSE
-> Context / Head
-> human-facing result
~~~

The provider participates in realization. It does not become the semantic owner of the Vex turn.

## Rightful owners

| Question | Current owner |
|---|---|
| What model/source does the person target and what is actually serving? | Models + `VexLifeInferenceCurrentState` |
| Is that exact serving realization admissible for a normal turn? | `VexLifeInferenceSessionAdmission` |
| Which Partner provider/model/profile representations does Product understand? | `VexLifePartnerProviderCatalog` + `VexLifePartnerModelProfileCatalog` |
| What configured non-secret credential coordinate currently exists? | `VexLifePartnerConnectionSettings` + `VexLifePartnerConfiguredCurrentness` |
| What does current provider/account/service/model standing mean? | `VexLifePartnerConnectionProjection` |
| What support-proof threshold has been crossed? | `VexLifePartnerSupportQualification` |
| Which accepted deterministic support evidence is current for the built-in profiles? | `VexLifePartnerDeterministicSupport` |
| What exact live provider/model qualification exists in this process? | `VexLifePartnerLiveQualification` |
| What exact non-secret network effect is intended? | `VexLifePartnerNetworkPlan` |
| Who resolves the secret and performs HTTPS? | platform realization of `VexLifePartnerHostEffect` |
| How is provider representation normalized? | `ExternalProviderWire` + provider-specific codecs |
| What is the provider-neutral inference relationship used by a turn? | `CompanionInferenceConnection` |
| Who joins current Home settings, protected-host truth, Models serving, qualification and admission for the Product service? | `VexLifeProdPartnerInference` |
| Who forms the minimum reference-only Relay for an exact direct message without interpreting its words? | `VexLifeDirectMessageSemanticRelay` |
| Who composes one human turn and commits canonical Vex truth? | `DirectCompanionTurn` + canonical Conversation/Context owners |

## Permanent non-collapses

~~~text
MODEL TARGET != SERVING REALIZATION
SERVING COORDINATE != QUALIFIED SERVING SESSION

PROVIDER PROFILE != LIVE ACCOUNT/MODEL QUALIFICATION
CONTRACT PROVEN != LIVE QUALIFIED
LIVE UNQUALIFIED != BROKEN

CREDENTIAL REF != CREDENTIAL VALUE
NETWORK PLAN != NETWORK EFFECT
HOST RECEIPT != PROVIDER SEMANTIC AUTHORITY

PROVIDER TOOL/CALL ID != VEX CAPABILITY AUTHORITY
PROVIDER RESPONSE != CANONICAL CONVERSATION UNTIL VEX ADMITS IT

PARTNER REALIZATION != CONVERSATION OWNER
PARTNER REALIZATION != MEMORY OWNER
PARTNER REALIZATION != MODEL-SELECTION OWNER
PARTNER REALIZATION != PRESENTATION OWNER
~~~

## Process-current live qualification

Live provider/model qualification is deliberately process-current.

~~~text
current configured credential coordinate
+ source-owned provider/model profile
+ deterministic contract/adapter/Vex proof
+ one explicit bounded protected-host probe
    ↓
current connection observation
+ credential/account-bound live qualification receipt
~~~

The receipt is not a durable restart fact. Restart begins from durable connection coordinates and
protected-host presence, then current provider/account/service/model truth must be observed again.

A credential replacement or account-scope change invalidates a prior in-process qualification for
serving admission.

~~~text
LIVE QUALIFICATION FOR CREDENTIAL A
!= LIVE QUALIFICATION FOR CREDENTIAL B

HISTORICAL PROVIDER SUCCESS
!= CURRENT PROCESS QUALIFICATION

QUALIFICATION FOR STAGED TARGET A
!= PERMISSION TO PROMOTE AFTER MODELS TARGET CHANGES TO B
~~~

The live qualification probe has no retry loop, no capability tools, no Conversation/Memory material,
and no authority to promote a model into serving. Models-owned Use/Start remains the human semantic
control. A successful asynchronous qualification is re-bound to the still-current staged Models
target before promotion; a target change makes that earlier result stale and leaves prior serving
truth unchanged.

## Product service composition

The running Product service may consume Partner truth only through the same existing owners:

~~~text
Home Partner settings
+ protected-host credential presence
+ deterministic support evidence
+ Models-owned staged / serving coordinate
+ process-current live qualification
        ↓
VexLifeProdPartnerInference
        ↓
VexLifeInferenceCurrentState
+ VexLifeInferenceSessionAdmission
        ↓
exact qualified Partner text-turn realization
~~~

`VexLifeProdPartnerInference` is composition, not a new state store. It rereads durable non-secret
settings and protected-host presence whenever currentness is projected. A process-live qualification
is reprojected only while the exact credential/account coordinate that earned it remains current.
Credential rotation therefore cannot inherit an earlier success.

The direct-message Semantic Relay used by production Partner text turns is deliberately
non-interpretive. `VexLifeDirectMessageSemanticRelay` binds the exact canonical message and
participants, marks language/runtime/localization understanding UNKNOWN/NONE, retains CANDIDATE
interpretation standing, and duplicates no raw message content.

~~~text
REFERENCE-ONLY RELAY
!= INTERPRETED MEANING
!= TRANSLATION
!= PROVIDER AUTHORITY
~~~

## Credential and host boundary

The JVM keeps only an opaque credential coordinate.

~~~text
JVM
  provider/model/profile
  opaque credentialRef
  public account scope
  exact request body
      ↓
no-secret NetworkPlan
      ↓
protected platform host
  resolve OS credential
  form authentication
  perform exact HTTPS request
  refuse redirect
  discard secret
      ↓
JVM
  bounded non-secret result/evidence
~~~

No API key/access-token value returns to ordinary JVM semantic code.

Current platform realizations remain platform-owned manifestations of that same host-effect contract;
platform packaging does not become provider/model support authority.

## Conversation and capability boundary

Partner response provenance is admitted through the canonical Vex Conversation owner. Provider
identity remains provenance, not ownership.

Provider-native tool/control representation remains held behind the Vex capability relationship:

~~~text
provider capability representation
-> Partner normalization
-> Vex capability admission
-> Vex-owned effect authority
-> rightful effect owner
-> semantic result
-> provider continuation when admitted
~~~

A provider call identifier can preserve external continuation identity. It cannot authorize the Vex
effect.

## Source descent

Start here, then descend only to the owner implicated by the question.

~~~text
support/currentness
  src/commonMain/kotlin/vexlife/partner/
    VexLifePartnerDeterministicSupport.kt
    VexLifePartnerSupportQualification.kt
    VexLifePartnerConnectionProjection.kt
    VexLifePartnerModelProfileCatalog.kt

configured/restart truth
  src/jvmMain/kotlin/vexlife/partner/
    VexLifePartnerConfiguredCurrentness.kt
    VexLifePartnerConnectionConfiguration.kt

live qualification / protected effect
  src/jvmMain/kotlin/vexlife/partner/
    VexLifePartnerLiveQualification.kt
    VexLifePartnerNetworkPlan.kt
    VexLifePartnerHostEffect.kt
    VexLifePartnerNativeHostClient.kt
    VexLifePartnerMacHostRealization.kt

provider representation
  src/jvmMain/kotlin/vexlife/partner/
    ExternalProviderWire.kt
    OpenAiResponsesWire.kt
    AnthropicMessagesWire.kt
    KimiChatWire.kt

serving / turn composition
  src/commonMain/kotlin/vexlife/conversation/VexLifeDirectMessageSemanticRelay.kt
  src/jvmMain/kotlin/vexlife/model/VexLifeInferenceSessionAdmission.kt
  src/jvmMain/kotlin/vexlife/service/VexLifeProdPartnerInference.kt
  src/jvmMain/kotlin/vexlife/companion/CompanionInferenceConnection.kt
  src/jvmMain/kotlin/vexlife/companion/PartnerInference.kt
  src/jvmMain/kotlin/vexlife/companion/DirectCompanionTurn.kt

canonical durable truth
  src/commonMain/kotlin/vexlife/conversation/
  src/jvmMain/kotlin/vexlife/conversation/
~~~

The focused `docs/work/support-external-api-*` records remain useful implementation/proof provenance.
They are not competing current architecture owners.

## Proof follows the claim

- support/currentness law -> focused common/JVM tests;
- wire semantics -> provider fixture/codec tests;
- credential secrecy and native-host behavior -> platform protected-host proof;
- live-qualification source behavior -> deterministic fake-host tests first;
- actual LIVE_QUALIFIED truth -> explicit legitimate live-provider observation;
- ordinary Local-or-Partner human turn -> service/turn lived proof;
- capability continuation -> separate Vex capability/effect proof.

A fake provider result can prove Product behavior under bounded inputs. It cannot claim a real account,
credential, model, or provider is live-qualified.

<!-- [VXG RealForever][VEXLIFE-PROD][PARTNER-INFERENCE-FOUNDATION] -->
