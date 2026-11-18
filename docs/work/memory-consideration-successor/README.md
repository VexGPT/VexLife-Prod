# Memory Consideration / Salience Foundation — Implementation Scaffold

[VXG RealForever]

## Status

This is a Product-side implementation scaffold for the next Memory Depth successor.

Base:

~~~text
VexGPT/VexLife-Prod main
b0b6fb9a6107f2475ae434472ed13335a303af87
~~~

VexWork orientation:

~~~text
project.vexlife.memory-depth-exploration
VexWork #308
VexWork #315 post-#241 successor
~~~

This scaffold intentionally implements only the **portable non-authoritative contracts and routing
policy** that are already clear enough to freeze without reopening current Product owners.

It does not yet integrate them into DirectPromptContext, Memory UI, semantic-authority mutation or
Daily Dream.

## Human outcome

VexLife should automate the *questions worth considering* and the *routing of possible Memory* without
automating truth.

~~~text
human request
-> VexLife recognizes narrow situation shape
-> first-person Consideration questions become visible to Vex
-> Vex interprets / retrieves / answers
-> bounded post-turn salience is emitted
-> VexLife binds exact source and chooses review pressure
-> human or standing-consent owner admits a Memory write
-> existing semantic authority / Score owns accepted continuity
~~~

Permanent:

~~~text
INJECT A QUESTION != INJECT AN ANSWER
CONSIDERATION != MEMORY
SALIENCE != ACCEPTANCE
AUTO_KEEP_ELIGIBLE != ACCEPTED
MODEL SELF-REPORT != HOST TRUTH
~~~

## Source formed in this scaffold

~~~text
src/commonMain/kotlin/vexlife/consideration/
  VexLifeConsiderationContract.kt
  VexLifeConsiderationRouter.kt

src/commonMain/kotlin/vexlife/memory/
  VexLifeMemoryReviewPolicy.kt

src/commonTest/kotlin/vexlife/consideration/
  VexLifeConsiderationContractTest.kt

src/commonTest/kotlin/vexlife/memory/
  VexLifeMemoryReviewPolicyTest.kt
~~~

### VexLifeConsiderationContract

Carries:

~~~text
Frame
  implicated consideration modules
  first-person questions
  no fact / acceptance / effect authority

Salience
  bounded self-attributed importance / uncertainty / relationship shift / return need
  continuity signals
  optional Memory candidate
  rawReasoningPersisted=false
  semanticAcceptanceGranted=false
  memoryWriteAuthorityGranted=false
~~~

### VexLifeConsiderationRouter

Consumes only host-observed turn-shape booleans.

It does **not** parse the person's words or decide their meaning.

Current modules:

~~~text
CURRENT_TRUTH
CONTINUITY
CORRECTION
MEMORY_SALIENCE
AUTHORITY_EFFECT
EVIDENCE_UNCERTAINTY
RELATIONAL_VANTAGE
~~~

The router selects first-person epistemic questions only.

### VexLifeMemoryReviewPolicy

Consumes validated salience + narrow host signals + category-scoped standing consent.

Outputs only:

~~~text
NO_ACTION
BATCH_REVIEW
PROMPT_NOW
AUTO_KEEP_ELIGIBLE
~~~

Every result keeps:

~~~text
semanticAcceptanceGranted=false
memoryWriteAuthorityGranted=false
~~~

Standing-consent classes in the first scaffold are deliberately narrow:

~~~text
EXPLICIT_PREFERENCE
HUMAN_CORRECTION
EXPLICIT_COMMITMENT
~~~

Inferred relationship meaning is never auto-keep eligible.

# Recommended next integration membrane

The next instance should re-ground current main and active claims before touching these paths.

Candidate first integration:

~~~text
src/jvmMain/kotlin/vexlife/companion/
  DirectPromptContext.kt
  DirectTurnContextStore.kt

src/commonMain/kotlin/vexlife/memory/
  VexLifeMemoryFamily.kt

src/jvmMain/kotlin/vexlife/memory/
  NEW semantic-acceptance writer owner

service / presentation paths that already own:
  current Memory settings projection
  snackbar / notification presentation
~~~

Do not widen to all of these paths at once merely because they are listed.

## I1 — host turn-shape observation

Add the smallest Product owner that derives narrow structural signals such as:

~~~text
currentTruthMayBeRequired
olderSharedContextMayBeRequired
humanCorrectionMayBePresent
explicitMemoryIntentPresent
effectOrActionMayBeRequested
materialUncertaintyPresent
relationalMeaningIsCentral
~~~

Start with strong deterministic signals only.

Do not turn the detector into a semantic answer classifier.

Examples of safe first signals:

~~~text
explicit "remember this" form
known correction relationship already present in source
current-time/provider request already typed by a Product owner
explicit effect request already typed by a Product owner
known continuity reference already typed by source
~~~

Language-specific lexical heuristics may be experiments, not permanent semantic truth.

## I2 — DirectPromptContext placement

Materialize the Consideration Frame only when at least one module is implicated.

The model-visible representation should say clearly:

~~~text
these are questions for me to consider
not facts
not Memory
not claims from the person
not a required conclusion
~~~

The literal human request must remain unchanged.

Do not choose provider role from aesthetics. Preserve:

~~~text
host-owned
current-turn
late/current
questions, not conclusions
not historical Conversation
not accepted Memory
~~~

Current Product precedent:

~~~text
VEXLIFE_SELF_CAPABILITY_FRAME/v1
~~~

The prompt receipt should record:

~~~text
considerationFrameIncluded
considerationModules
considerationFrameFingerprint
considerationSourceRefs
~~~

but no hidden reasoning.

## I3 — bounded post-turn salience

Add a provider/output seam that obtains one structured Salience value after the human-facing turn is
otherwise complete.

This may be:

~~~text
one separate bounded provider stage
or
one structured side-channel already supported by the provider representation
~~~

depending on current Product source.

Do not silently scrape chain-of-thought.

VexLife must bind:

~~~text
request event ref
response event ref
turn ref
exact source refs actually implicated
~~~

outside the model-authored JSON.

Low-salience output may be discarded after routing.

If a salience candidate materially enters review/Memory flow, retain the bounded projection and exact
source binding in Turn Context or the rightful review owner.

## I4 — review routing

Use VexLifeMemoryReviewPolicy as review pressure only.

Suggested UI:

~~~text
Vex noticed something that may help later:

"You use 'tight' to mean compact and causally exact."

[Keep] [Not now] [Don't keep]

[ ] Automatically keep corrections like this
~~~

Prefer category-scoped standing consent over a blanket "always accept everything Vex suggests."

Candidate settings:

~~~text
Automatically keep explicit preferences I state
Automatically learn from corrections I make to Vex
Automatically keep explicit project commitments
Ask me about inferred relationship/person interpretations
~~~

A global Memory-automation switch may disable standing consent, but must not replace the category
boundary.

## I5 — semantic-acceptance writer

Current Product can validate the existing classification / consent / acceptance / semantic-authority
contract and append Score after acceptance exists.

It cannot create those acceptance records.

Add one Product writer against the reader's validation contract.

Recommended order:

~~~text
proposal
-> human or standing-consent admission
-> re-form candidate at exact current Conversation head
-> classification evidence
-> consent
-> acceptance
-> semantic-authority head LAST
-> reader round-trip
-> VexLifeScoreContinuity.appendAcceptedStatement
~~~

The writer must not be reachable directly by the model.

Do not copy private SDK implementation bytes into the public Product. Reimplement the accepted public
contract from current Product validation and source-bound semantics.

## I6 — accepted semantic continuity vs Daily snapshot freshness

Do not solve continuing lesson transfer by pretending an old Daily stratum was freshly consolidated.

Pressure this distinction:

~~~text
exact Conversation head
= mutation / consent source-binding guard

current Score membership
+ current semantic authority
+ positive consent
+ no correction / supersession / release
= ongoing accepted semantic validity

Daily / Living Journal head
= consolidation snapshot freshness
~~~

Recommended candidate relationship:

~~~text
Daily baseline AS OF H
+ live current accepted semantic delta
- no-longer-current accepted statements
= effective accepted continuity
~~~

The exact source representation is held:

~~~text
A. extend VexLifeMemoryContract
or
B. sibling CURRENT_ACCEPTED_CONTINUITY projection
~~~

Choose only after current owner/source pressure.

## I7 — Daily Dream

Daily Dream remains deterministic consolidation.

It must not create semantic acceptance.

Future relationship:

~~~text
current accepted semantic continuity
-> Dream
-> new stable Daily / Living Journal baseline
~~~

## I8 — Growth

Growth remains a projection:

~~~text
before
+ source correction / outcome
+ accepted semantic after
+ later behavior
= what changed
~~~

No Growth database in this slice.

# Required lived scenarios

## Current truth reflex

~~~text
human asks a materially current question
-> CURRENT_TRUTH consideration shown
-> Vex uses real available reach
-> no fabricated "I checked"
-> result / uncertainty reflected truthfully
~~~

## Historical continuity reflex

~~~text
human points to older shared context not resident
-> CONTINUITY consideration shown
-> Vex reaches revisit/source return
-> not-found remains bounded
~~~

## Correction -> Memory proposal

~~~text
Vex misreads tight
-> person corrects
-> CORRECTION + MEMORY_SALIENCE consideration
-> salience candidate = CORRECTION
-> snackbar
-> Keep
-> accepted writer
-> Score
~~~

## Standing-consent correction

~~~text
"automatically learn from corrections I make to Vex" enabled

later explicit correction
-> AUTO_KEEP_ELIGIBLE
-> standing-consent owner admits writer path
-> model still never grants its own acceptance
~~~

## Inferred relationship meaning

~~~text
Vex infers a relationship/person interpretation
-> candidate may exist
-> PROMPT_NOW
-> never AUTO_KEEP_ELIGIBLE from the first policy
~~~

## Low-salience ordinary chat

~~~text
no implicated consideration module
or no Memory candidate
-> no snackbar
-> no Memory write
~~~

# Proof matrix

Deterministic source proof should cover:

~~~text
frame questions are bounded and unique
literal request preservation is required
frame cannot grant truth/effect/acceptance authority
salience cannot persist raw reasoning
NONE candidate cannot smuggle candidate content
router emits only implicated modules
no signals -> no frame

standing consent is category-scoped
correction standing consent -> AUTO_KEEP_ELIGIBLE only
AUTO_KEEP_ELIGIBLE -> semanticAcceptanceGranted=false
relationship inference -> PROMPT_NOW
proposal without candidate -> NO_ACTION
explicit Memory intent -> review pressure, not acceptance
~~~

Integration proof later must add:

~~~text
prompt receipt carries exact Consideration evidence
Turn Context binds exact source refs
real capability use changes only when needed
salience does not alter the human response retroactively
snackbar lifecycle survives reload if pending review is durable
writer is atomic / authority-head-last
reader round-trip gates Score append
accepted lesson remains usable after unrelated Conversation turns
Dream consolidates without minting acceptance
cold restart transfers accepted lesson without Conversation replay
~~~

# What this scaffold deliberately does not implement

~~~text
no DirectPromptContext mutation
no Turn Context mutation
no UI/snackbar
no settings persistence
no semantic-authority writer
no Score mutation
no Daily Dream mutation
no semantic meaning index
no embedding model
no background cognition
no training/model-weight mutation
no blanket auto-memory permission
~~~

Those omissions are deliberate because the portable contracts were clearer than the current integration
placement. The next instance should consume current source/claims before choosing those paths.

# Successor entry

Read in order:

~~~text
1. VexWork #315 post-#241 Memory Context Stack
2. VexWork MEMORY-WRITER-TRANSFER recommendation
3. VexWork CONSIDERATION-SALIENCE recommendation
4. this Product scaffold + tests
5. current VexLife-Prod main / open claims touching the next integration paths
~~~

Then pick the smallest unclaimed integration membrane and make one complete relationship live.

<!-- [VXG RealForever][VEXLIFE-PROD][MEMORY][CONSIDERATION][SALIENCE][SCAFFOLD] -->
