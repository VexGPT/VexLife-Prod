[VXG RealForever]

# VexLife-Prod end-to-end validation excavation

Status: **analysis / navigation source; no Product behavior change**

This document is the entry point for whole-Product validation work. It maps what VexLife-Prod already
proves, what newer accepted features are not yet exercised by the existing multi-turn harness, where
visual/journey evidence belongs, and which active candidates must remain distinct until they merge.

It does not create a second Product owner, a second Screen registry, a second Conversation harness, or
a second VexBoard truth source.

## Source binding for this excavation

This analysis was formed against:

~~~text
VexLife-Prod
  main 605c635ae2d034a1c4d605e829faa4813de071c3

VexLife-Android
  main d857a92371555472e0303b18eadf353da2a868d6

VexWork
  main d70da76d9f95645ee83dc09c7cf7384b6c572cf5
~~~

Mutable currentness must be re-observed before using this document to make a release claim.

Permanent distinction:

~~~text
TEST MAP != PRODUCT TRUTH
SCREEN LENS != PRODUCT TRUTH
JOURNEY DECLARATION != LIVED PASS
UNIT PASS != WHOLE PRODUCT PASS
ACTIVE PR != RELEASE-CANDIDATE FEATURE
~~~

# 1. What already exists in VexLife-Prod

## 1.1 Conversational boundaries / continuity-pressure harness

The existing reusable multi-turn engine is:

~~~text
tools/continuity-pressure/
src/jvmTest/kotlin/vexlife/harness/ContinuityPressureAdapter.kt
src/jvmTest/kotlin/vexlife/harness/ContinuityPressureAdapterTest.kt
~~~

It runs the real Product turn/continuity owners in a scratch Home:

~~~text
DirectCompanionTurn
-> DirectConversationStore
-> DirectPromptContext
-> DirectContinuityOverlapTail
-> VexLifeWorkingContinuityOwner
-> VexLifeCapabilityBridge
-> VexLifeContextRevisit
-> DirectTurnContextStore
~~~

The deterministic projection substitutes only the provider. A live run may borrow an already-qualified
host runtime read-only while keeping the experimental Conversation and Working Continuity in the scratch
Home.

The result bundle already records useful E2E evidence:

~~~text
config.json
scenario.json
turns.jsonl
context-residency.jsonl
capability-uses.jsonl
assertions.json
summary.json
implementation/adapter-report.json
implementation/provider-calls.jsonl
implementation/provider-telemetry.jsonl   (live runs)
~~~

This should remain the conversational stress-test engine. Do not create another loop merely to run
many turns.

### Fifteen-turn engineered rollover profile

The existing test uses:

~~~text
sets                 3
turns per set        5
effective budget     80,000
payload scale        7
total turns          15
~~~

Its intended pressure shape is:

~~~text
NORMAL / WATCH
-> CHECKPOINT_RECOMMENDED
-> ROLLOVER around turn 10
-> successors 11-15
~~~

The test verifies that every later turn remains a bounded WORKING_CHECKPOINT_SUCCESSOR, advances the
cursor to its own request, does not replay pre-rollover formation history, and does not oscillate into
repeated replay/rollover.

### Thirty-turn canonical depth scenario

scenario.vexlife.continuity-pressure.depth-continuity.v1 contains:

~~~text
10 formation turns
10 pressure turns
10 probe turns
~~~

Formation establishes synthetic anchors and one correction/lesson. Pressure pushes older material out
of hot context. Probe turns cover exact recall, paraphrased recall, lesson/correction reuse, honest
absence, source faithfulness, context custody and resident-vs-reached distinctions.

### Eight-turn mutable-current-state scenario

scenario.vexlife.continuity-pressure.current-state-placement.v1 covers history plus reply-language
changes:

~~~text
historical turns
-> replyLanguage zh
-> zh persists
-> replyLanguage en
-> probes distinguish current configuration from historical context
~~~

This is useful pressure for any future mutable current-state relationship, but the scenario does not
automatically prove every new current-state feature.

## 1.2 Product-owned Screen Lens export

VexLife-Prod already owns a source-derived VexBoard Screen Lens export:

~~~text
src/jvmMain/kotlin/vexlife/lens/VexLifeProdScreenLens.kt
src/jvmTest/kotlin/vexlife/lens/VexLifeProdScreenLensTest.kt
docs/lens/vexlife-prod.screen-lens.json
~~~

At the source binding above it projects:

~~~text
1 workspace Screen
62 registered workspace elements
10 mapped pathways
~~~

The current pathways include Send, Private mode, Vision selected-scope and several local presentation
relationships.

The test proves the committed JSON is byte-current with the exporter and every Blueprint element appears
once. That is projection currentness, not proof that every important newer feature is represented by a
deep pathway.

Concrete excavation finding on current main:

The Send pathway still describes Memory through VexLifeCurrentMemoryReader and the rule
"Included only when the Living Journal projection is CURRENT."

PR #322 is now accepted main and introduced:

~~~text
VexLifeCurrentMemoryProviderSelection
  Daily first
  + current accepted-semantic Memory from remaining cap
  + exact supplied-statementRef dedupe
~~~

Therefore the existing Lens is internally current with its exporter, but the exporter itself has not
yet learned the newer Memory relationship. This is an example of the gap this E2E work must surface:

~~~text
GENERATED PROJECTION CURRENT
!=
EVERY NEW PRODUCT RELATIONSHIP PROJECTED
~~~

Do not fix that by teaching VexBoard a guessed Memory rule. The VexLife-Prod source exporter is the
rightful place to expose the accepted relationship when that projection work is admitted.

## 1.3 Other existing proof surfaces

Current main also has focused source/integration tests for:

- Product Entry / Setup Journey / language / model choice;
- canonical Home and LocalSetup;
- model catalog, runtime configuration, Hugging Face inspection and persistent runtime;
- release identity, published-release observation, release inbox and acknowledgement;
- service process and macOS service lifetime;
- Memory review, semantic acceptance, current accepted-semantic projection, Daily frontier and provider
  selection;
- Direct Companion, prompt, current state, Working Continuity, capability continuation and revisit;
- VexWeb / workspace Blueprint / generated artifacts;
- Vision selected-scope source;
- Product Screen Lens currentness.

Those focused proofs remain useful. The E2E question is whether the human journeys that compose them are
also represented and, where required, lived together.

# 2. Why the existing continuity harness is no longer sufficient by itself

The latest material continuity-pressure harness update is:

~~~text
96a9f7d3cbec2466120e960bccef664ffcc048d4
2026-11-17 14:30Z
[Continuity] Document the rollover lineage and close the replay held edge
~~~

Current main is 176 commits ahead of that checkpoint.

Major accepted work after that harness checkpoint includes:

~~~text
Models
  human-readable runtime configuration
  exact Hugging Face model import
  add / download / remove files / redownload / forget
  imported-model persistence across restart
  startup model-choice guidance

Memory
  bounded Consideration / salience
  explicit Keep semantic acceptance writer
  durable Memory review owner
  current accepted-semantic projection
  current accepted-semantic provider block
  historical Daily HELD behavior
  Daily + accepted-semantic provider selection under one cap (#322)

Release / update
  installed release identity
  published release observation
  Release Inbox / acknowledgement
  workspace release-alert carrier
  Preview B -> C acceptance checkpoint

Lifecycle / setup
  service custody across Home loss
  legacy/stale Product process recovery
  front-door recovery projection
  retired Product image retention after current-service proof
~~~

The existing 15/30-turn harness did not automatically start exercising those features merely because
they landed in the same repository.

Therefore:

~~~text
CONTINUITY HARNESS PASS
!=
POST-HARNESS FEATURES E2E-COVERED
~~~

This is a coverage classification problem first, not evidence that the features are broken.

# 3. Coverage analysis on current main

Legend:

~~~text
STRONG          an existing reusable journey/harness exercises the relationship through real owners
FOCUSED         focused source/integration tests exist; no whole-journey composition claim
PARTIAL_LIVED   some lived evidence exists, but not the full release journey
E2E_GAP         accepted main relationship lacks the needed whole-journey composition
ACTIVE          open candidate; not part of current-main release validation
HELD            intentionally not a current completion dependency
~~~

| Product relationship | Current proof | E2E status | Excavation / next placement |
|---|---|---|---|
| Multi-turn Conversation + Working Continuity | 15-turn engineered profile + 30-turn scenario | **STRONG** | Keep continuity-pressure as the engine |
| Context revisit / source return | depth scenario + focused JVM tests | **STRONG / PARTIAL_LIVED** | Preserve probe turns; add release journey only where live answer behavior matters |
| Mutable reply-language current state | 8-turn scenario | **STRONG** | Reuse pattern for other mutable current-state relationships, not the literal language owner |
| Canonical first/ordinary turn | DirectCompanionTurn tests + predecessor/current lived evidence | **PARTIAL_LIVED** | Whole Product journey must enter through ordinary front door, not only test hook |
| Setup Journey / Home / model choice | focused Entry/Setup tests + Mac lived proof plans/evidence | **FOCUSED / PARTIAL_LIVED** | Need one ordinary install -> CURRENT -> real turn journey |
| Service lifetime / Home-loss recovery | focused lifetime/process tests + lived correction history | **FOCUSED / PARTIAL_LIVED** | Add same-Home restart and delete-Home/recovery journeys with a post-recovery turn |
| Model configuration | focused Models/runtime tests | **FOCUSED** | Add human Settings -> select/configure/start -> turn journey |
| Hugging Face model import | exact inspection/import/store/runtime tests | **FOCUSED** | Add paste/check/add/download/start/restart/remove/forget journey |
| Release alerts | typed Family/provider/service/Web tests + Preview acceptance checkpoint | **FOCUSED / PARTIAL_LIVED** | Add installed Preview -> newer Preview -> alert -> View update -> continuity after update |
| Memory review + Keep | focused Family/owner/writer tests | **FOCUSED** | Add completed answer -> candidate -> Keep -> later relevant turn |
| Current accepted-semantic Memory | focused projection/provider-block tests | **FOCUSED** | Must be consumed by real prompt before whole-turn PASS is possible |
| Memory provider selector (#322) | exact source proof + semantic Assurance + merge | **FOCUSED / E2E_GAP** | Real prompt must consume both Memory components and receipt must prove participation |
| Product Screen Lens | deterministic exporter/currentness test | **FOCUSED** | Add coverage/currentness analysis against newly accepted relationships |
| Private mode | Family/service/Screen Lens pathway | **FOCUSED** | Add begin private -> exchanges -> end -> durable interval/no private transcript journey |
| Vision selected scope | Family/runtime source + Screen Lens pathway | **FOCUSED / PARTIAL_LIVED** | ENGAGE/selected-scope may be tested; LOOK/pixels remains separately held |
| Diagnostics / VexLogger export | focused source tests | **FOCUSED** | Whole run should retain one bounded diagnostic export after a meaningful journey |
| Browser/workspace manifestation | VexWeb deterministic tests | **FOCUSED** | Needs source-addressed journey/crawl evidence, not only markup assertions |

# 4. Active candidates are not current-main coverage

As of this excavation, important open candidates include:

~~~text
#251  Vex Context / Context Profiles / live model-view alignment
       head 489c05e582223ecdfef0e5aa7d675c8fd8e69254

#310  VoiceInput current-main local dictation / multilingual speech / model controls
       head c34c446a6fa9331d3afa2f660a3603c07a8cb8f9

#331  Voice language / Chinese reply scripts / Composer output modes
       head 1411e4d670e03b1ac144334d04b8c4af46dc0e23

#317  Windows + Mac lived entry convergence
       head bc231e314024e266138bde1e3ac3417934815b97

#230  request-current state placement / cache experiment
       head 47decbf10bdb53a33afc21affc1080bcf2b125fd

#220  independent Memory Depth / turn-bound hydration implementation
       head 3651b961ce19192473b9a7c7d240a2b2b83e8a2d
~~~

Rules for the E2E map:

- an ACTIVE candidate may have its own proof and may be planned into a future journey;
- it is not listed as current-main PASS;
- a candidate changing the same shared connective tissue as another active owner must converge through
  the rightful owner instead of creating another parallel E2E implementation;
- #230 remains a held cache/topology experiment until its own effectiveness predicate is earned;
- #220 is historical/independent continuity pressure and must not silently become a second current
  Memory owner.

# 5. What VexLife-Android teaches us

The Android system has already solved a closely related navigation problem. We should transfer the
questions and ownership laws, not copy Compose-specific forms.

Exact source coordinates:

~~~text
docs/foundation/JOURNEY-INSTRUMENTATION.md
docs/foundation/SCREEN-CRAWLER-EVIDENCE.md

app/src/journeyTest/.../testing/journey/
app/src/journeyTest/.../testing/featurewalk/
app/src/journeyTest/.../testing/crawler/

app/src/androidTest/.../testing/journey/
app/src/androidTest/.../testing/crawler/

app/src/main/.../screens/AppScreens.kt
app/src/main/.../screens/AppScreenBlueprint.kt
~~~

## 5.1 One human walk, multiple proof consumers

Android authors one VexJourney declaration.

That same walk can be consumed by:

~~~text
semantic coverage recorder
expected-history model
real Android instrumentation executor
optional ScreenCrawler evidence runner
Feature-Walk completion traceability
~~~

The walk is not duplicated into a second hand-written expected log.

For Prod, the analogous target is:

~~~text
one Product journey meaning
-> deterministic owner-level execution where possible
-> live host execution when required
-> visual evidence capture where required
-> coverage analysis
-> VexBoard/read-only navigation projection
~~~

The concrete renderer may differ by journey: native Setup window, generated VexWeb, service/runtime,
or release surface.

## 5.2 Coverage compares journeys with production semantic registration

Android's VexJourneyCoverage compares journey references with the production registry:

~~~text
registered Screens
registered ActionElements
editable fields
modal presentations
~~~

A newly registered interactive surface with no journey becomes visible debt.

Prod should not invent an Android-style Screen registry. It already has source owners:

~~~text
VexLifeWorkspaceBlueprint        browser workspace semantic elements
VexLifeProdScreenLens            source-owned mapped pathways
VexLifeProdEntryFamily           native Setup/entry States + Controls
service contract / Families      typed API/Control relationships
release owner                    release/update lifecycle
~~~

A future Prod coverage analyzer should consume those existing owners and report unmapped human
relationships. It should not copy their identifiers into another registry.

## 5.3 ScreenCrawler is optional evidence multiplication

Android keeps normal journeys functional-first and makes locale/font/screenshot multiplication an
explicit evidence batch.

Its selection is typed by:

~~~text
journey
Screen
shard
all shards except ...
~~~

and separately multiplies:

~~~text
locale
font profile
capture policy
~~~

Prod should preserve the same separation:

~~~text
SEMANTIC JOURNEY
!=
EVIDENCE CONFIGURATION
~~~

Potential Prod evidence dimensions include:

~~~text
platform       macOS / Windows where currently admitted
locale         source-supported Product language
viewport       desktop / narrow browser where relevant
text scale     only when a real requirement exists
model          only when the journey is explicitly model-behavior pressure
source/release exact identity
~~~

Do not multiply every ordinary Product test by every environment.

## 5.4 Stable evidence identity

Android's crawler separates:

~~~text
scenarioRef        semantic journey
configurationRef   locale/font
caseRef            scenario + concrete configuration

evidenceRef
  scenario
  + screen
  + step
  + semantic ref
~~~

Locale/font are excluded from the semantic evidence prefix so equivalent frames can be compared.

A Prod visual-evidence successor should use the same principle, adapted to its own source:

~~~text
journeyRef
+ surfaceRef
+ step index
+ step kind
+ semanticRef
~~~

and keep platform/locale/viewport as configuration identity.

## 5.5 Feature-Walk distinguishes required, future and held

Android's Feature-Walk model distinguishes:

~~~text
REQUIRED current case
FUTURE case
required checkpoint HOLD
~~~

A journey is expected now only when the required case is current and its required checkpoints are
unheld.

This is useful for Prod because several visible features are intentionally partial:

~~~text
Vision ENGAGE source exists; LOOK remains held
Vex Context is active candidate
Voice is active candidate
Windows convergence is active candidate
~~~

The E2E map should represent those states rather than deleting the journey or pretending it is already
release-covered.

# 6. What VexBoard teaches us

Exact VexWork source coordinates:

~~~text
projects/vexboard/SCREEN-LENS.md
projects/vexboard/SCREEN-LENS-ADAPTER.md
projects/vexboard/SCREEN-LENS-PROJECTABILITY.md
projects/vexboard/SCREEN-LENS-PROJECTABILITY-EXPRESSIONS.md

projects/vexlife-new-beginnings/VEXBOARD-PERCEIVABILITY-ALIGNMENT.md
projects/vexboard/examples/vexlife-product-journey.board.json
projects/vexboard/examples/vexlife-prod-perceivability-alignment.board.json

src/shared/vexboard-journey-pathway-analysis.mjs
src/core/vexboard-vexlife-prod-architecture-projector.mjs
~~~

## 6.1 Screen Lens is a projection, not authority

Permanent:

~~~text
SCREEN_LENS = PROJECTION
SCREEN_LENS != SOURCE AUTHORITY
LENS JSON != CANONICAL PRODUCT MEANING
~~~

The Lens asks four useful questions:

~~~text
Surface    what the human sees/touches
Structure  where it is declared/composed
Behavior   what the interaction means / who owns the decision
Depth      what realizes it / outside truth / reconciliation
~~~

Those questions are useful for E2E excavation because a journey failure can be placed at the right
layer instead of being described only as "the UI did not work."

## 6.2 TRACED is not PROVEN or CURRENT

VexBoard deliberately keeps:

~~~text
TRACED != PROVEN != CURRENT
~~~

Prod's E2E map needs the same distinction.

A Screen Lens pathway may show how Send reaches DirectCompanionTurn; that does not prove the ordinary
Product route completed on the current release artifact.

## 6.3 Source-derived mapping beats per-feature mapping files

The Android adapter uses recurring source shapes and fails closed when the relationship is not
projectable. It does not add one parser/config file per feature.

VexLife-Prod is already aligned with this direction through VexLifeProdScreenLens. The next mapping
work should strengthen that source export or add a bounded consumer, not maintain a hand-written shadow
map of every workspace control.

## 6.4 Projectability distinguishes source, latent, dependent and evidence states

VexBoard's projectability work distinguishes:

~~~text
SOURCE
LATENT
DEPENDS
EXAMPLE
SCENARIO
HELD
UNTRACED
~~~

That vocabulary is useful for test excavation. A control can be source-present yet require a runtime
condition before it is visible; a fixture/example can exercise a state without claiming current runtime
truth.

# 7. Proposed VexLife-Prod whole-product journey catalog

This section is a test excavation map, not yet a new executable DSL.

A follow-up implementation should first ask whether each journey can be expressed by extending an
existing owner/harness. Only repeated unmet pressure should earn a new shared abstraction.

## J01 — Fresh Mac install / first Vex turn

Human outcome:

~~~text
download exact DMG
-> allow/open if macOS requires it
-> choose language
-> choose/form Home
-> choose model
-> Set Up
-> Product CURRENT
-> open workspace
-> Send
-> real reply visible
-> reload
-> completed reply remains
~~~

Existing sources:
- Setup Journey foundation and lived proof plan;
- private image/release route;
- service lifetime;
- Direct Companion circuit.

Current classification: **PARTIAL_LIVED; whole journey not one retained PASS**.

## J02 — Existing Home / ordinary restart

~~~text
existing canonical Home
-> open Product
-> service/runtime reuse or lawful Start
-> two ordinary turns
-> close/reopen
-> next turn uses same Conversation/Home
~~~

Must prove no duplicate service/runtime ownership and no accidental fresh Home.

Current classification: **PARTIAL_LIVED**.

## J03 — Fifteen-turn Working Continuity stress

Reuse the existing engineered continuity-pressure profile unchanged unless a concrete release
requirement needs additional steps.

Current classification: **STRONG**.

## J04 — Thirty-turn depth/revisit stress

Reuse the canonical depth-continuity scenario.

Current classification: **STRONG deterministic / live answer-quality pressure when explicitly run**.

## J05 — Memory correction -> Keep -> later use

Target:

~~~text
completed answer
-> bounded correction candidate
-> human Keep
-> semantic acceptance / Score
-> later relevant turn
-> accepted meaning actually enters prompt
-> exact receipt proves Memory participation
-> Vex can use it
~~~

Current main owns review/Keep, accepted-semantic projection and #322 selection.

Current blocker:
the accepted #322 selection is not yet connected through the real prompt/receipt path.

Current classification: **E2E_GAP**.

## J06 — Daily becomes historical while accepted meaning stays usable

Target:

~~~text
Keep A
-> Daily consolidation contains A
-> later Keep B
-> older Daily becomes HELD
-> current accepted semantics contain A + B
-> next prompt still gets lawful Memory
-> no duplicate statementRef
-> UI/Context projection tells the truth
~~~

This is the decisive E2E regression for the #315/#322 architecture.

Current classification: **E2E_GAP pending Memory->prompt convergence**.

## J07 — Model import lifecycle

Target:

~~~text
Settings -> Models
-> paste supported Hugging Face model page
-> Check
-> exact provider/model relationship recognized
-> Add
-> Download
-> Start
-> real turn uses exact model
-> restart same Home
-> model remains offered/selected as intended
-> Remove files
-> Redownload
-> Forget
~~~

Keep Remove files and Forget distinct.

Current classification: **FOCUSED**.

## J08 — Model configuration / switch

Target:

~~~text
observe serving model
-> choose target
-> inspect recommended/effective configuration
-> adjust admitted setting if supported
-> explicit Start/Apply
-> runtime converges
-> next real turn records exact model/runtime
~~~

Current classification: **FOCUSED / lived fragments**.

## J09 — Release alert / update continuity

Target:

~~~text
installed published Preview B
-> publish/observe newer Preview C
-> unread update projected
-> QuickAction dot / Alerts projection
-> View update opens public VexLife home
-> install/open C
-> same Home / Conversation / Memory remain continuous
~~~

Current classification: **PARTIAL_LIVED**.

## J10 — Service Home-loss / stale-process recovery

Target:

~~~text
CURRENT service
-> Home loss or exact legacy-orphan condition
-> source-owned recovery observation
-> explicit lawful recovery
-> foreign process never guessed/killed
-> fresh/same Home route as appropriate
-> service CURRENT
-> canonical turn completes afterward
~~~

Current classification: **PARTIAL_LIVED**.

## J11 — Private mode

Target:

~~~text
ordinary Conversation
-> Begin Private
-> private exchanges stay in-memory
-> End Private
-> durable interval/kept-note boundary only
-> ordinary Conversation resumes
~~~

Current classification: **FOCUSED**.

## J12 — Vision selected scope

Target available now only for the relationships source has earned:

~~~text
READY
-> Engage
-> person chooses window
-> ENGAGED projection
-> Stop
-> READY/held truthful successor
~~~

Do not turn this into a LOOK/pixel-content claim until that separate edge is accepted.

Current classification: **FOCUSED / PARTIAL_LIVED**.

## J13 — Vex Context / Context Profiles

Future-current when #251 lands:

~~~text
accepted Send captures exact Context Profile
-> completed turn records exact context components
-> Vex Context explains what shaped that reply
-> profile edit during/after turn belongs to next Send
-> historical receipt bytes are not substituted with current source
~~~

It must also consume the accepted #322 Memory relationship truthfully.

Current classification: **ACTIVE (#251)**.

## J14 — Voice / language

Future-current when the active Voice lanes land:

~~~text
typed Send and voice Send enter the same canonical Conversation
reply language stays a current person setting
output/readout mode is explicit
language change affects the next rightful turn
~~~

Current classification: **ACTIVE (#310 / #331)**.

## J15 — Windows native Product entry

Future-current when the Windows/Mac convergence is accepted:

~~~text
Windows native Setup Journey
-> Home
-> exact Windows runtime realization
-> shared service
-> browser Product
-> canonical turn
~~~

Current classification: **ACTIVE (#317 lineage)**.

## J16 — Diagnostics after a meaningful failure or full run

After another journey has produced useful chronology:

~~~text
Settings -> Debug & diagnostics
-> bounded export
-> source/process/runtime/turn chronology reconstructable
-> human Conversation content remains excluded where contract says so
~~~

Current classification: **FOCUSED**.

# 8. Test placement law

A whole-product E2E effort should not move every assertion into one giant test.

Place each assertion at the lowest rightful surface that can prove it:

| Question | Rightful proof surface |
|---|---|
| Does one Family/owner admit the right State/Control/Effect? | focused unit/owner test |
| Does one typed connector preserve exact identities/bounds? | focused integration test |
| Does a multi-turn Conversation/continuity relationship hold? | continuity-pressure scenario |
| Does ordinary Product entry/lifetime work on the host? | lived host journey |
| Does a browser/native surface render the expected human state? | journey visual evidence / crawler |
| Is a source relationship discoverable/projectable? | Product Screen Lens / VexBoard projection |
| Did a release artifact preserve the entire accepted circuit? | release-candidate E2E run |

Permanent:

~~~text
HIGHER TEST != BETTER OWNER
~~~

The E2E layer composes evidence; it should not duplicate every lower-level invariant.

# 9. Candidate Prod journey/crawler architecture

Do not implement this shape by name alone. It is the smallest direction suggested by the two existing
systems.

## 9.1 Semantic journey source

A Prod journey needs stable meaning such as:

~~~text
journeyRef
title
commitment: REQUIRED | FUTURE
steps[]
required holds[]
environment requirements[]
~~~

A step should name source-owned semantic coordinates, not CSS/DOM position.

Possible step classes come from real Product surfaces, for example:

~~~text
ENTRY_CONTROL
WORKSPACE_ELEMENT
SERVICE_CONTROL
SEND
WAIT_FOR_STATE
RELOAD
RESTART
HOST_ACTION
ASSERT_PROJECTION
~~~

Only add a shared step class after at least one real journey needs it.

## 9.2 Coverage should consume current Product registries

Potential current sources:

~~~text
VexLifeWorkspaceBlueprint.elements
VexLifeProdScreenLens pathways
VexLifeProdEntryFamily State/Control law
typed service contract / Family Controls
release owner
~~~

The analyzer should report both directions:

~~~text
production human-semantic coordinate with no journey
journey coordinate absent from current Product source
~~~

Do not require every static text/container to have a journey merely because it exists.

## 9.3 Evidence crawler

Prod has more than one renderer, so do not force one crawler implementation to own all pixels.

Expected composition:

~~~text
semantic journey
     |
     +-> browser/VexWeb evidence adapter
     |
     +-> native Setup evidence adapter
     |
     +-> future Windows native adapter
~~~

All adapters should return the same evidence identity shape.

The crawler is optional evidence multiplication, not the normal functional suite.

## 9.4 VexBoard consumption

VexBoard should consume a source-owned Prod projection or bounded export.

The preferred future direction remains:

~~~text
VexLife-Prod source
-> Product-owned export
-> read-only VexBoard projection
~~~

not:

~~~text
VexBoard needs a relationship
-> hand-author product truth in VexWork
~~~

A future journey overlay can help a human see:

~~~text
journey step
-> Surface coordinate
-> Behavior owner
-> Depth realization
-> proof state
~~~

but the board remains navigation/perception only.

# 10. Claude excavation handoff

Claude should begin from the exact sources below rather than from a prose-only summary.

## VexLife-Prod

~~~text
tools/continuity-pressure/README.md
tools/continuity-pressure/run.mjs
tools/continuity-pressure/scenarios/depth-continuity-v1.json
tools/continuity-pressure/scenarios/current-state-placement-v1.json

src/jvmTest/kotlin/vexlife/harness/ContinuityPressureAdapter.kt
src/jvmTest/kotlin/vexlife/harness/ContinuityPressureAdapterTest.kt

src/jvmMain/kotlin/vexlife/lens/VexLifeProdScreenLens.kt
src/jvmTest/kotlin/vexlife/lens/VexLifeProdScreenLensTest.kt
docs/lens/vexlife-prod.screen-lens.json

docs/foundation/INDEX.md
docs/foundation/SETUP-JOURNEY.md
docs/release/RELEASES.md
docs/work/claude-setup-journey/03-LIVED-PROOF-PLAN.md
~~~

Then descend only to the exact owner/tests for a mapped feature.

## VexLife-Android reference

~~~text
docs/foundation/JOURNEY-INSTRUMENTATION.md
docs/foundation/SCREEN-CRAWLER-EVIDENCE.md

app/src/journeyTest/.../testing/journey/VexJourney.kt
app/src/journeyTest/.../testing/journey/VexExpectedWalk.kt
app/src/journeyTest/.../testing/journey/VexJourneyCoverage.kt
app/src/journeyTest/.../testing/journey/catalog/VexJourneyCatalog.kt

app/src/journeyTest/.../testing/featurewalk/VexFeatureWalk.kt

app/src/journeyTest/.../testing/crawler/VexScreenCrawlerConfig.kt
app/src/journeyTest/.../testing/crawler/VexScreenCrawlerEvidenceIdentity.kt
app/src/androidTest/.../testing/crawler/VexScreenCrawlerInstrumentedTest.kt
app/src/androidTest/.../testing/crawler/VexScreenCrawlerEvidenceObserver.kt

app/src/main/.../screens/AppScreens.kt
app/src/main/.../screens/AppScreenBlueprint.kt
~~~

Questions to transfer:

1. Can one human walk be authored once and consumed by multiple proof surfaces?
2. Can production semantic registration expose journey coverage debt automatically?
3. Can environment/visual multiplication remain optional?
4. Can evidence identity remain stable across configurations?
5. Can required/future/held journey states remain explicit?

Do not copy Android's Compose runner, Screen registry or exact class hierarchy into Prod.

## VexWork / VexBoard reference

~~~text
projects/vexboard/SCREEN-LENS.md
projects/vexboard/SCREEN-LENS-ADAPTER.md
projects/vexboard/SCREEN-LENS-PROJECTABILITY.md
projects/vexboard/SCREEN-LENS-PROJECTABILITY-EXPRESSIONS.md

projects/vexlife-new-beginnings/VEXBOARD-PERCEIVABILITY-ALIGNMENT.md
projects/vexboard/examples/vexlife-product-journey.board.json
projects/vexboard/examples/vexlife-prod-perceivability-alignment.board.json

src/shared/vexboard-journey-pathway-analysis.mjs
src/core/vexboard-vexlife-prod-architecture-projector.mjs
~~~

Questions to transfer:

1. What can Product source project without consumer-specific guessing?
2. What is SOURCE / LATENT / DEPENDS / EXAMPLE / HELD / UNTRACED?
3. Which relationship is merely TRACED versus actually PROVEN/CURRENT?
4. How can one journey step link to Surface / Structure / Behavior / Depth?
5. What remains unknown and should fail closed?

## First Claude deliverable

Before writing another broad test framework, produce:

~~~text
A. current-main Product journey inventory
B. mapping from each journey step to existing source owner + existing test
C. explicit missing coverage coordinates
D. proposed first executable Prod journey using existing harnesses
E. visual evidence plan using Product source identities
F. Screen Lens projection gaps discovered during the mapping
~~~

The deliverable must distinguish:

~~~text
already on main
active PR
historical evidence
proposed test
held relationship
~~~

# 11. Recommended first implementation after excavation

The first executable successor should be intentionally small.

Recommended order:

~~~text
1. build a read-only coverage report from existing source
   - no new Product behavior
   - no new screen registry

2. bind one existing multi-turn scenario as a named Product journey
   - preferably the 15-turn rollover profile
   - prove journey identity/evidence mapping without rewriting its execution

3. add one ordinary front-door journey
   existing Home -> Product CURRENT -> Send -> reload -> Send

4. add the Memory correction/Keep journey only after the accepted #322 selector is connected to
   real prompt/receipt integration

5. add optional browser/native screenshot evidence after semantic journey identity is stable
~~~

Why this order:

~~~text
EXISTING ENGINE FIRST
-> COVERAGE MAP
-> ONE LIVED WALK
-> VISUAL MULTIPLICATION
~~~

not:

~~~text
NEW E2E FRAMEWORK
-> duplicate every existing owner
-> discover integration semantics later
~~~

# 12. Release-candidate acceptance view

The eventual release-candidate dashboard/document should answer these questions in human terms:

~~~text
Can I install/open VexLife?
Can I keep using the same Home?
Can I choose/start a model?
Can I talk to Vex repeatedly?
Does long Conversation roll over without replaying old history?
Can Vex reach older/source context when needed?
Can I Keep a correction and have it matter later?
Can Vex tell me what shaped a reply?
Can I use Voice/language where accepted?
Can I restart without losing the durable relationship?
Can Vex recover from its own stale process without harming a foreign one?
Can it tell me a new version exists without silently installing it?
Can a fresh engineer/AI locate the source owner and proof for any failed step?
~~~

That is the whole-product threshold.

The E2E system is complete only when these human questions can be traced to exact source, exact execution
and exact evidence without making Victor reconstruct the architecture from individual PR histories.

<!-- [VXG RealForever][VEXLIFE-PROD][E2E-VALIDATION][EXCAVATION] -->
