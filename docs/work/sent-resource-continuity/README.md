[VXG RealForever]

# Sent Resource Continuity — trajectory front door

## Human outcome

After a canonical VexLife Send that included files or images, the historical human request should still make this relationship visible and reviewable:

```text
human request words
+ exact sent resource relationship
-> durable historical projection
-> compact file/image presentation beside the human message
-> later review without changing Conversation text
```

The person should be able to tell **what was sent with that request** after Send and after reload.

Wide layout:
- compact row/grid when space allows.

Narrow layout:
- responsive wrapping without horizontal overflow.

Images:
- bounded thumbnail/preview;
- display name;
- media/dimension detail when useful;
- exact sent image review path.

Readable text/files:
- compact file card;
- display name/type;
- bounded readable snippet where useful;
- exact Product-held projection/original review path when lawful.

Missing historical bytes/projection:
- truthful HELD/unavailable card;
- do not silently erase a still-provable request→resource relationship.

## Current Product baseline

Repository:
`VexGPT/VexLife-Prod`

Accepted current main at trajectory formation:

```text
commit ae4a511b49ad4720b5c51de55ccb834d43aef218
tree   776f587d2489f3972cd5d92cadf47bc454ee8bd0
```

This is merge #471, the currentized visual-input stack.

Visual lived proof is already terminal:

```text
A011
RETURN sha256 c0b1bdf3d2e6e79eeebe34e69987c48b106742b9fa0bc011e39625cdb9afe9b3
PASS
replayRequired=false
```

Do not rebuild vision support and do not replay A009–A011.

SDK proof-context continuity remains in:
`vgong24/Vextreme-SDK#1965`

That PR owns terminal visual proof continuity, not this Product UX trajectory.

## Observed lived gap

Before Send, the Composer correctly shows a READY attachment chip.

After Send:
- Vex can use the attached image and answer from it;
- the canonical request words remain visible;
- the Composer draft correctly clears;
- the historical human message no longer shows that a resource was sent.

Therefore:

```text
MACHINE RESOURCE RELATIONSHIP SURVIVES
!=
HUMAN HISTORICAL PROJECTION SURVIVES
```

The gap is projection/continuity, not attachment custody or visual inference.

The source handoff containing the lived screenshots is:

```text
VEXLIFE-SENT-RESOURCE-CONTINUITY-SUCCESSOR-HANDOFF-20261121-R2
```

R1 provenance SHA-256:

```text
01afb7f1c905e73021a579436fa148632303343d148f07391234d91c59636267
```

## Existing rightful owners

### Attachment Draft — pre-Send only

`src/commonMain/kotlin/vexlife/attachment/VexLifeAttachmentDraftFamily.kt`

Its contract explicitly owns only files staged in the unsent Composer draft.

Permanent:

```text
READY DRAFT ITEM != REQUEST RESOURCE BINDING
REMOVE FROM DRAFT != DELETE ORIGINAL ARTIFACT
```

Do not keep sent resources alive by extending AttachmentDraft ownership past Send.

### Local Artifact — immutable resource truth

`src/commonMain/kotlin/vexlife/artifact/VexLifeLocalArtifact.kt`

Already owns:
- OriginalArtifact;
- DerivedProjection;
- StaticImageProjection;
- RequestBinding;
- RequestVisualInterpretation.

Permanent:

```text
ORIGINAL BYTES != DERIVED TEXT
DERIVED TEXT != MODEL INTERPRETATION
STATIC IMAGE PROJECTION != VISUAL INTERPRETATION
STAGED != SENT
SENT != READ
EXACT HUMAN WORDS != PRODUCT RESOURCE MANIFEST
```

### Local Artifact Store — durable request binding/custody

`src/jvmMain/kotlin/vexlife/artifact/VexLifeLocalArtifactStore.kt`

Already supports durable lookup of exact request→attachment bindings and exact artifact/projection custody.

Do not add a second artifact database or event ledger.

### Conversation — exact human words

`src/jvmMain/kotlin/vexlife/conversation/DirectConversationStore.kt`

Canonical request content remains the person's exact words.

Current integration proof already requires:

`requestWordsStayExactAndResourcesAreBoundBeforeTheProviderRuns`

Do not append filenames, resource summaries or hidden attachment text to Conversation content merely for presentation.

### Request Artifact Manifest — current-request resource interpretation

`src/jvmMain/kotlin/vexlife/artifact/VexLifeRequestArtifactManifest.kt`

Already joins:

```text
canonical REQUEST
+ RequestBinding
+ Local Artifact projection
```

and knows how to describe readable text and qualified image interpretation.

This is prompt/current-request oriented. It may supply reusable relationships/data, but it is not automatically the historical UI owner.

### Service projection — current missing seam

`src/commonMain/kotlin/vexlife/service/VexLifeProdServiceContract.kt`

Current `ConversationMessage` contains:
- event/message/turn identity;
- role/content/sequence/time/model.

It does **not** carry the sent-resource relationship.

### Local Product Service — composition seam

`src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt`

Already holds:
- canonical Conversation;
- Local Artifact Store;
- Attachment Draft;
- Send composition.

Prefer a bounded read-only projection here over a new broad service/foundation if sufficient.

### Web presentation — current visible omission

`src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt`

Current human `messageNode(message)` renders the exact human content and no historical resource tray.

The same runtime already renders pre-Send attachments separately from AttachmentDraft.

## VexArchitecture convergence — what is earned now

Architecture references re-grounded for this trajectory:

```text
vgong24/Vextreme
pages/vexarchitecture-first-stable-body.html
blob 328e1fdaffd59ced8f06584d28d769b90dae9a5e

pages/vexarchitecture-live-demonstration.html
blob 03e2764ec120a189467292e79132114fba1b84bd
```

The Builder Field Guide separates these questions:
- Family owns semantic/product meaning: State, Control, lawful Act, optional Effect, outward Projection/DisplayState when that shape is actually earned;
- Execution owns active asynchronous custody, not feature meaning or history;
- Store/provider answers the smallest truthful outside/durable fact;
- Projection/presentation returns semantic truth to the human without making the renderer the owner;
- Relay/convergence is conditional: it is earned when independently-owned complete truths must become **new relationship truth**;
- Partner is earned only when an independently-owned external participant needs Source/Signal/Connection/Observation vocabulary.

Applied to sent-resource continuity:

```text
Conversation
  owns exact human words / canonical event lineage

Local Artifact + Local Artifact Store
  own exact original/projection custody

RequestBinding
  ALREADY owns the durable REQUEST -> sent-resource relationship

historical sent-resource service/web work
  projects that already-owned relationship back to the human
```

Therefore the first implementation does **not** earn:
- a new sent-resource Family merely to render read-only history;
- a new VexExecution relationship merely to read/project durable state;
- a VexPartner relationship merely because bytes live outside Conversation;
- a new Relay/convergence owner merely to rediscover a relationship already owned by RequestBinding.

Permanent for this first slice:

```text
EXISTING REQUEST_BINDING RELATIONSHIP
-> BOUNDED PROJECTION
-> HUMAN PRESENTATION

!= NEW SEMANTIC OWNER
!= NEW RELAY TRUTH
!= NEW RESOURCE STORE
```

A later relationship may earn more architecture only when its own question appears:

1. **Resource retention / remove-local-copy**
   - may earn Family State/Control/Effect if the person gains a lawful Product Control over local custody;
   - historical REQUEST -> resource truth must survive even when local bytes are deliberately removed;
   - current custody and historical relationship must remain separate facts.

2. **Historical resource reacquisition by Vex**
   - a current human request + an exact historical Conversation request + retained Artifact custody can become a new turn-scoped relationship;
   - that is the point where a convergence/Relay-style owner may be earned, because independently-owned truths are being composed into new current-turn reach;
   - do not let the model enumerate arbitrary artifact IDs or local paths.

3. **Long-term Memory**
   - Memory may later retain source-attributed semantic refs back to Conversation/request source;
   - Memory must not become the owner or byte store for sent images/files;
   - semantic recall, exact historical Conversation, and exact Local Artifact custody remain separate truths.

The Live Demonstration's Maps lesson is the relevant guard: Relay observes coherence between independently-owned truths without stealing either owner. Here, however, RequestBinding already owns the historical resource relationship, so projection is sufficient until a genuinely new relationship is requested.

## Storage-lifecycle pressure to preserve now

The current content-addressed artifact layout already permits byte reuse across multiple requests. The first historical projection must not make future space reclamation destructive to history.

Design target:

```text
historical relationship
  request -> artifactRef
  remains durable

current local custody
  available / intentionally removed / missing-or-damaged
  is observed separately
```

Do not define those words as runtime enums until a rightful owner/source contract earns them.

Before an explicit delete/GC Control is added, historical projection should already tolerate unavailable bytes/projections truthfully rather than turning one missing local file into loss of the whole request-resource relationship.

## Smallest likely Product shape

Do not memorize this syntax; preserve the relationships.

Likely path:

```text
canonical REQUEST event
+ exact request event hash
-> existing RequestBinding
-> read-only historical resource projection
-> Conversation service projection
-> messageNode human resource tray/grid
```

Possible wire shapes:
- `ConversationMessage.resources`; or
- sibling `requestResourcesByEventHash/messageRef`.

Prefer the smaller form that:
- leaves Conversation content exact;
- leaves artifact meaning/custody with Local Artifact;
- does not copy large text/image bytes into every Conversation response;
- reconstructs after reload from durable Product truth.

A bounded review route may be earned for exact resource content/bytes if the timeline projection itself should remain metadata-only.

## Presentation invariants

```text
HISTORICAL RESOURCE PROJECTION != CONVERSATION CONTENT
HUMAN REVIEW != MODEL PROMPT ADMISSION
RESOURCE CARD != NEW ARTIFACT CUSTODY
THUMBNAIL != VISUAL INTERPRETATION
DISPLAY NAME != ARTIFACT IDENTITY
MAC VISUAL QUALIFICATION != WINDOWS VISUAL QUALIFICATION
```

Preserve request order.

Current maximum resource count remains:
`VexLifeLocalArtifact.MaximumPerRequest = 8`.

Long names may truncate visually but must remain accessible through title/accessible label.

## Platform / multiplatform posture

The human-facing sent-resource continuity relationship is intended to be **Product/platform-neutral**.

Shared meaning:
```text
request event
+ durable RequestBinding
+ artifact/projection identity
-> historical review projection
```

Shared presentation:
- the generated VexWeb workspace is the same Product presentation relationship on supported desktop hosts;
- row/grid/wrap behavior should be renderer/browser responsive, not Mac-specific.

JVM/local implementation:
- Local Artifact Store and Local Product Service are JVM owners and should not encode macOS UI assumptions;
- file/image review routes must remain under exact VexLife Home/artifact custody and no-follow/path-containment rules.

Mac:
- current visual path has lived A011 PASS for selected 4B;
- ordinary current-main DMG is the current human test route;
- use Mac lived proof for the first UX checkpoint because the exact feature is already exercised there;
- concurrent VexLife lanes can change host runtime; re-observe exact source/PID/currentness rather than blaming candidate source from timing alone.

Windows:
- do not borrow Mac visual-runtime qualification;
- the **historical sent-resource projection itself must not require visual inference** merely to show/review an already-sent image;
- text/file/image resource-history semantics should remain the same when Windows Product attachment flow is available;
- Windows gets its own lived host proof before claiming Windows end-to-end completion;
- if Windows visual interpretation is still HELD, historical image review should truthfully show the original sent image relationship without pretending Windows image inference ran.

Other/future model realizations:
- sent-resource historical continuity follows the canonical RequestBinding, not a specific model;
- model capability controls whether Vex can interpret/use a resource during a turn, not whether the human can later see what they sent.

## Relay / VexLocalBridge continuation contract

Do not reconstruct Relay from old task ZIPs or prose.

For every **new/unformed** proof attempt:
1. read current Operations entry;
2. read live SDK `docs/private-continuity/vexrelay/current-contract.json`;
3. verify:
   `contractRef=source.vexrelay.current-contract/v1`;
4. classify exact stage/effect;
5. lock the route from the current contract;
6. use the matching accepted recipe/adapter;
7. retain and consume the canonical RETURN;
8. continue lifecycle from the returned forward state.

Current observed SDK binding at this trajectory formation:

```text
SDK main 6f8f6f50467f5df5a542713f7dc25b82db4ac8fa
tree     96795640b01f1969da980f52d5a8ec136a8c44aa

Relay contract
schemaVersion vex.relay.current-contract/v1
contractRef   source.vexrelay.current-contract/v1
runtime       v0.2.0-RC1
task          vex.relay.task/v2
result        vex.relay.result/v2
```

Accepted recipe families include:
- CurrentMainCompositionProof;
- ExactHeadSourceOwnerProof;
- IndependentAssurance;
- LifecycleCurrentness;
- OwnerMergeDuty;
- PostMergeVerification.

Current host profiles include:
- `macos-victor`;
- `windows-victor`.

Current repository-operation host carrier:
`adapter.vexlocalbridge.native-vexrelay-task-archive/v1`.

Permanent:
```text
NEW RUNTIME AVAILABLE != REPLAY EXISTING VALID ATTEMPT
PACKAGE FAILURE != CANDIDATE FAILURE
HOST FAILURE != SOURCE DEFECT
READY != MERGE
MERGE != POST-MERGE VERIFICATION
```

When host proof is required, use the current qualified VexLocalBridge human realization from the current contract/Operations rather than asking the human to choose scripts, branches, shells or recovery paths.

The previous visual trajectory also established a useful continuity pattern:
- durable proof context may live in a companion PR;
- direct PR-backed source runner may be preferable to chat-generated formation ZIPs when it truthfully binds current accepted carrier behavior;
- the canonical RETURN remains the evidence returned from host execution.

Do **not** assume that exact runner/package shape is automatically valid for this new UX proof; bind the current Relay contract at formation time.

## Human-boundary proof route — companion PR, one command, one RETURN

Do not make GitHub Actions the ordinary proof dependency for this trajectory.

The predecessor visual-input trajectory established the preferred human-facing pattern in:
`vgong24/Vextreme-SDK#1965`.

Target shape after an exact Product candidate exists:

```text
companion SDK proof-context PR
  holds source-owned formation/runner context
  + exact Product candidate binding
  + current Relay/VexLocalBridge route binding
        |
        v
Victor runs ONE exact command
        |
        v
command fetches/verifies the exact companion PR source
re-binds the live current Relay contract
forms/uses the source-managed qualified carrier internally
executes the bounded proof
        |
        v
one canonical RETURN ZIP
        |
        v
Victor uploads RETURN
        |
        v
Operations consumes and continues
```

Human contract:

```text
Victor does not manually download/manage the task ZIP.
Victor does not choose scripts, branches, shells, logs, or recovery.
Victor's ordinary Relay responsibility is:
  1. run the one exact supplied script/command;
  2. upload the one canonical RETURN.
```

The source-managed carrier may still use an immutable ZIP internally when the current accepted contract requires it. The point is that package formation/custody stays behind the qualified companion/source runner rather than becoming a manual human download/assembly step.

macOS:
- one exact Bash command;
- one canonical RETURN ZIP.

Windows when its separate lived checkpoint is earned:
- one exact PowerShell command;
- one canonical RETURN ZIP.

At formation time, always re-read and bind:
`docs/private-continuity/vexrelay/current-contract.json`
with `contractRef=source.vexrelay.current-contract/v1`.

Do not copy #1965's old candidate coordinates, timestamps, task identities, or package bytes. Reuse the proven **pattern**, not a sealed attempt.

## Planned trajectory after coordination clears

### A. Current docs/coordination front door

Current #478 stays the durable trajectory owner while shared implementation paths are held.

Re-grounded overlap:
- #381 currently carries an explicit implementation claim over the main service/web paths this feature is likely to need;
- #387 remains relevant Screen-Lens/convergence context, but its existence alone is not a source-ownership blocker.

Do not author through #381's live claimed paths. Reconcile/adopt the rightful forward source state first.

### B. First Product implementation — historical human projection

Once exact source ownership is clear:

```text
canonical human REQUEST
+ requestEventHash
+ existing RequestBinding
+ current exact Local Artifact custody observation
-> small historical resource descriptor
-> Conversation/service projection
-> human message resource tray
-> bounded exact local review
```

The timeline response remains metadata-small. Original bytes are never embedded into every Conversation JSON response.

Proof includes:
- exact human words unchanged;
- single/multiple image/file ordering;
- reload reconstruction;
- narrow/wide responsive presentation;
- repeated identical artifact byte reuse;
- missing/unavailable local resource does not erase relationship;
- no duplicate tray after Send retry/reconciliation.

### C. Source proof and fresh Assurance

Use current Relay recipes/routes, without GitHub Actions dependency where the actual gate accepts local/host proof:

```text
exact candidate
-> ExactHeadSourceOwnerProof
-> fresh IndependentAssurance
```

Keep SourceOwner, Assurance, lived UX, and lifecycle evidence separate.

### D. Lived Mac UX proof through companion source runner

Create/update a companion SDK proof-context PR only after the candidate head/tree is exact.

Human experience:
```text
one Bash command
-> ordinary Product launch/proof
-> send resource
-> response
-> reload
-> historical resource still visible/reviewable
-> canonical RETURN
```

A concurrent Product service/process is re-grounded before attribution; do not blame candidate source from timing/interference alone.

### E. Integration/lifecycle

```text
lived UX PASS
-> lifecycle currentness / integration review
-> READY when earned
-> OwnerMergeDuty
-> PostMergeVerification
```

No GitHub Actions result is invented or required merely because it is conventional.

### F. Natural successors, not first-slice scope

After the human historical projection is accepted:
1. explicit local-resource retention/removal + space accounting;
2. historical resource reacquisition into a new current request;
3. Memory/source-reference composition where semantic Memory legitimately points back to exact source;
4. Windows lived proof under its own qualified Product/runtime conditions.

## Expected proof/lifecycle path

Source candidate:
- exact current-main composition;
- focused tests for service projection + renderer;
- request-content exactness regression;
- reload reconstruction regression;
- mixed resource order/layout regression;
- held/unavailable historical resource regression.

Then:
```text
ExactHeadSourceOwnerProof
-> fresh IndependentAssurance
-> lived Mac UX proof
-> lifecycle currentness / integration review
-> READY
-> OwnerMergeDuty
-> PostMergeVerification
```

Do not invent cross-stage proof aggregation. Each gate owns its own truth.

If Product main advances while SourceOwner is running:
- exact candidate head/tree unchanged + fixed base still ancestor may continue SourceOwner;
- integration currentness belongs later.

## Lived acceptance

Text:
```text
stage text file READY
-> Send
-> request words unchanged
-> response completes
-> Composer draft clears
-> reload
-> historical request still shows file
-> human can review exact sent file/projection
```

Image:
```text
stage image READY
-> Send
-> existing visual inference behavior remains unchanged where qualified
-> response completes
-> Composer draft clears
-> reload
-> historical request shows thumbnail/card
-> human can review exact sent image
```

Mixed:
- 2–8 resources preserve order;
- wide row/grid;
- narrow wrap;
- no horizontal overflow;
- no duplication after reload/recovery.

Recovery:
- recovered/idempotent Send projects exact resources once;
- missing artifact/projection becomes HELD/unavailable, not invisible.

## Current overlap to re-ground before source claim

At formation time, older open Product PRs still claim some likely shared service/web paths, including:
- #381 — Vex Labs Native Test;
- #387 — Experience/Screen Lens convergence.

Their existence alone is not a blocker, but source authorship must re-ground actual current overlap before claiming shared paths.

This front-door PR intentionally claims only this trajectory document until implementation ownership is established.

## Likely implementation paths to inspect

Start with:
- `src/commonMain/kotlin/vexlife/service/VexLifeProdServiceContract.kt`;
- `src/jvmMain/kotlin/vexlife/service/VexLifeProdLocalService.kt`;
- `src/commonMain/kotlin/vexlife/web/VexLifeWebP1RuntimeArtifacts.kt`;
- `src/commonMain/kotlin/vexlife/web/VexLifeWebP1ExperienceStyle.kt`;
- corresponding service/web tests.

Read but do not assume mutation is required:
- `VexLifeLocalArtifactStore.kt`;
- `VexLifeRequestArtifactManifest.kt`;
- `DirectConversationStore.kt`.

## Completion meaning

This trajectory is complete when the human can send files/images and later perceive/review the exact resources next to the exact canonical human request after reload, without introducing a second resource truth or changing model/Conversation authority.

<!-- [VXG RealForever][VEXLIFE-PROD][SENT-RESOURCE-CONTINUITY] -->
